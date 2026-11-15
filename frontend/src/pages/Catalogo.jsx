import { useCallback, useEffect, useState } from 'react';
import { Search, Plus, X, Edit, RotateCcw, EyeOff, Image as ImageIcon } from 'lucide-react';
import {
  listarTecidos,
  criarTecido,
  atualizarTecido,
  desativarTecido,
  reativarTecido,
} from '../services/tecidos';

const formatarPreco = (valor) =>
  Number(valor).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
const formatarLargura = (valor) =>
  Number(valor).toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

// Foto por URL; sem foto (ou com URL que não carrega) mostra o ícone padrão.
function FotoTecido({ url, nome }) {
  const [falhou, setFalhou] = useState(false);
  if (!url || falhou) {
    return <div style={styles.noImage}><ImageIcon size={32} color="#9CA3AF" /></div>;
  }
  return <img src={url} alt={nome} style={styles.image} onError={() => setFalhou(true)} />;
}

export default function Catalogo() {
  const [searchTerm, setSearchTerm] = useState('');
  const [mostrarInativos, setMostrarInativos] = useState(false);
  const [catalogoData, setCatalogoData] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState('');
  const [modal, setModal] = useState(null); // null = fechado; {} = novo; tecido = edição

  const carregar = useCallback(async () => {
    setErro('');
    try {
      setCatalogoData(await listarTecidos({ busca: searchTerm.trim(), incluirInativos: mostrarInativos }));
    } catch (e) {
      setErro(e.message);
    } finally {
      setCarregando(false);
    }
  }, [searchTerm, mostrarInativos]);

  // A busca por nome ou composição (RF08) é feita pela API; espera o usuário parar de digitar.
  useEffect(() => {
    const espera = setTimeout(carregar, 300);
    return () => clearTimeout(espera);
  }, [carregar]);

  const alterarSituacao = async (item) => {
    if (item.ativo && !window.confirm(`Desativar o tecido "${item.nome}"? Ele deixa de aparecer no catálogo e no estoque.`)) {
      return;
    }
    try {
      await (item.ativo ? desativarTecido(item.id) : reativarTecido(item.id));
      await carregar();
    } catch (e) {
      setErro(e.message);
    }
  };

  const total = catalogoData.length;

  return (
    <div style={styles.container}>
      {/* Cabeçalho */}
      <div style={styles.header}>
        <div>
          <h1 style={styles.title}>Catálogo de Tecidos</h1>
          <p style={styles.subtitle}>{total} {total === 1 ? 'tecido cadastrado' : 'tecidos cadastrados'}</p>
        </div>
        <button onClick={() => setModal({})} style={styles.btnPrimary}>
          <Plus size={18} />
          Novo Tecido
        </button>
      </div>

      {/* Barra de Pesquisa */}
      <div style={styles.toolbar}>
        <div style={styles.searchBox}>
          <Search size={18} color="#9CA3AF" style={styles.searchIcon} />
          <input
            type="text"
            placeholder="Buscar por nome ou composição..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            style={styles.searchInput}
          />
        </div>
        <label style={styles.toggle}>
          <input type="checkbox" checked={mostrarInativos} onChange={(e) => setMostrarInativos(e.target.checked)} />
          Mostrar inativos
        </label>
      </div>

      {erro && <div role="alert" style={styles.alertError}>{erro}</div>}

      {/* Grade de Produtos */}
      <div style={styles.grid}>
        {catalogoData.map((item) => (
          <div key={item.id} style={{ ...styles.card, opacity: item.ativo ? 1 : 0.7 }}>
            <div style={styles.imageContainer}>
              <FotoTecido key={item.fotoUrl} url={item.fotoUrl} nome={item.nome} />
            </div>

            <div style={styles.cardContent}>
              <h2 style={styles.productName}>{item.nome}</h2>
              {item.composicao && <p style={styles.productSpecs}>{item.composicao}</p>}
              {(item.gramatura || item.largura) && (
                <p style={styles.productSpecs}>
                  {[item.gramatura && `${item.gramatura} g/m²`, item.largura && `${formatarLargura(item.largura)} m`]
                    .filter(Boolean).join(' · ')}
                </p>
              )}
              <p style={styles.productPrice}>{formatarPreco(item.precoMetro)}<span style={styles.priceUnit}>/m</span></p>
              {!item.ativo && <span style={styles.inativoBadge}>Inativo</span>}
            </div>

            <div style={styles.cardFooter}>
              <button style={styles.btnAction} onClick={() => setModal(item)}><Edit size={16} /> Editar</button>
              {item.ativo ? (
                <button style={styles.btnActionDanger} onClick={() => alterarSituacao(item)}><EyeOff size={16} /> Desativar</button>
              ) : (
                <button style={styles.btnActionNeutral} onClick={() => alterarSituacao(item)}><RotateCcw size={16} /> Reativar</button>
              )}
            </div>
          </div>
        ))}
      </div>
      {carregando && <p style={styles.emptyState}>Carregando catálogo...</p>}
      {!carregando && !erro && total === 0 && (
        <p style={styles.emptyState}>Nenhum tecido encontrado.</p>
      )}

      {/* Modal de Novo Tecido / Editar Tecido */}
      {modal && (
        <ModalTecido
          tecido={modal.id ? modal : null}
          onClose={() => setModal(null)}
          onSaved={() => {
            setModal(null);
            carregar();
          }}
        />
      )}
    </div>
  );
}

// Só nome e preço por metro são obrigatórios (AGENTS 7.3). Erros aparecem abaixo do campo.
function ModalTecido({ tecido, onClose, onSaved }) {
  const [formData, setFormData] = useState({
    fotoUrl: tecido?.fotoUrl ?? '',
    nome: tecido?.nome ?? '',
    composicao: tecido?.composicao ?? '',
    gramatura: tecido?.gramatura ?? '',
    largura: tecido?.largura ?? '',
    precoMetro: tecido?.precoMetro ?? '',
  });
  const [erros, setErros] = useState({});
  const [erroGeral, setErroGeral] = useState('');
  const [salvando, setSalvando] = useState(false);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    setErros({ ...erros, [e.target.name]: undefined });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErroGeral('');

    const novosErros = {};
    if (!String(formData.nome).trim()) novosErros.nome = 'O nome do tecido é obrigatório';
    if (formData.precoMetro === '' || Number(formData.precoMetro) <= 0) {
      novosErros.precoMetro = 'O preço por metro é obrigatório e deve ser maior que zero';
    }
    if (Object.keys(novosErros).length > 0) {
      setErros(novosErros);
      return;
    }

    // Campos opcionais vazios vão como null.
    const dados = {
      nome: formData.nome.trim(),
      composicao: String(formData.composicao).trim() || null,
      gramatura: formData.gramatura === '' ? null : Number(formData.gramatura),
      largura: formData.largura === '' ? null : Number(formData.largura),
      precoMetro: Number(formData.precoMetro),
      fotoUrl: String(formData.fotoUrl).trim() || null,
    };

    setSalvando(true);
    try {
      await (tecido ? atualizarTecido(tecido.id, dados) : criarTecido(dados));
      onSaved();
    } catch (err) {
      // "campos" da API: cada erro de validação vai abaixo do campo correspondente.
      const campos = err.detalhes?.campos ?? [];
      if (campos.length > 0) {
        setErros(Object.fromEntries(campos.map((c) => [c.campo, c.mensagem])));
      } else if (err.message?.startsWith('A URL da foto')) {
        // Validação da URL da foto (422, sem "campos"): mostra abaixo do campo da foto.
        setErros({ fotoUrl: err.message });
      } else {
        setErroGeral(err.message);
      }
    } finally {
      setSalvando(false);
    }
  };

  const campoErro = (nome) => erros[nome] && <p role="alert" style={styles.fieldError}>{erros[nome]}</p>;

  return (
    <div style={styles.modalOverlay}>
      <div style={styles.modalCard}>
        <div style={styles.modalHeader}>
          <h2 style={styles.modalTitle}>{tecido ? 'Editar Tecido' : 'Novo Tecido'}</h2>
          <button type="button" onClick={onClose} aria-label="Fechar" style={styles.closeBtn}><X size={20} color="#6B7280" /></button>
        </div>

        <form onSubmit={handleSubmit} noValidate style={styles.form}>
          <div style={styles.inputGroup}>
            <label style={styles.label}>URL DA FOTO (OPCIONAL)</label>
            <input
              name="fotoUrl" type="url" placeholder="https://..."
              value={formData.fotoUrl} onChange={handleChange} style={styles.input}
            />
            {campoErro('fotoUrl')}
          </div>

          <div style={styles.inputGroup}>
            <label style={styles.label}>NOME *</label>
            <input name="nome" type="text" value={formData.nome} onChange={handleChange} style={styles.input} />
            {campoErro('nome')}
          </div>

          <div style={styles.inputGroup}>
            <label style={styles.label}>COMPOSIÇÃO</label>
            <input
              name="composicao" type="text" placeholder="Ex: 100% Algodão"
              value={formData.composicao} onChange={handleChange} style={styles.input}
            />
            {campoErro('composicao')}
          </div>

          <div style={styles.row}>
            <div style={{ ...styles.inputGroup, flex: 1 }}>
              <label style={styles.label}>GRAMATURA (g/m²)</label>
              <input name="gramatura" type="number" value={formData.gramatura} onChange={handleChange} style={styles.input} />
              {campoErro('gramatura')}
            </div>
            <div style={{ ...styles.inputGroup, flex: 1 }}>
              <label style={styles.label}>LARGURA (m)</label>
              <input name="largura" type="number" step="0.01" value={formData.largura} onChange={handleChange} style={styles.input} />
              {campoErro('largura')}
            </div>
          </div>

          <div style={styles.inputGroup}>
            <label style={styles.label}>PREÇO POR METRO (R$) *</label>
            <input name="precoMetro" type="number" step="0.01" value={formData.precoMetro} onChange={handleChange} style={styles.input} />
            {campoErro('precoMetro')}
          </div>

          {erroGeral && <p role="alert" style={styles.fieldError}>{erroGeral}</p>}

          <div style={styles.modalFooter}>
            <button type="button" onClick={onClose} style={styles.btnCancel}>Cancelar</button>
            <button type="submit" disabled={salvando} style={styles.btnSubmit}>{salvando ? 'Salvando...' : 'Salvar'}</button>
          </div>
        </form>
      </div>
    </div>
  );
}

// Estilos in-line
const styles = {
  container: { fontFamily: 'sans-serif' },
  header: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' },
  title: { fontSize: '1.5rem', fontWeight: '600', color: '#111827', margin: '0 0 0.25rem 0' },
  subtitle: { fontSize: '0.875rem', color: '#6B7280', margin: 0 },
  btnPrimary: { display: 'flex', alignItems: 'center', gap: '0.5rem', backgroundColor: '#4F46E5', color: 'white', border: 'none', padding: '0.5rem 1rem', borderRadius: '6px', fontSize: '0.875rem', fontWeight: '500', cursor: 'pointer' },
  toolbar: { marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '1.5rem', flexWrap: 'wrap' },
  searchBox: { position: 'relative', width: '100%', maxWidth: '400px' },
  searchIcon: { position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)' },
  searchInput: { width: '100%', padding: '0.5rem 1rem 0.5rem 2.5rem', border: '1px solid #D1D5DB', borderRadius: '6px', fontSize: '0.875rem', outline: 'none' },
  toggle: { display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.875rem', color: '#374151' },

  grid: { display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: '1.5rem' },
  card: { backgroundColor: 'white', border: '1px solid #E5E7EB', borderRadius: '8px', overflow: 'hidden', display: 'flex', flexDirection: 'column' },
  imageContainer: { width: '100%', height: '160px', backgroundColor: '#F3F4F6' },
  image: { width: '100%', height: '100%', objectFit: 'cover' },
  noImage: { width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center' },
  cardContent: { padding: '1.25rem', flex: 1 },
  productName: { fontSize: '1rem', fontWeight: '600', color: '#111827', margin: '0 0 0.25rem 0' },
  productSpecs: { fontSize: '0.875rem', color: '#6B7280', margin: '0 0 0.25rem 0' },
  productPrice: { fontSize: '1.125rem', fontWeight: '600', color: '#4F46E5', margin: '0.75rem 0 0 0' },
  priceUnit: { fontSize: '0.875rem', color: '#6B7280', fontWeight: 'normal' },
  inativoBadge: { display: 'inline-block', marginTop: '0.5rem', padding: '0.125rem 0.5rem', borderRadius: '9999px', backgroundColor: '#F3F4F6', color: '#6B7280', fontSize: '0.75rem' },
  cardFooter: { display: 'flex', borderTop: '1px solid #E5E7EB' },
  btnAction: { flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem', padding: '0.75rem', background: 'none', border: 'none', borderRight: '1px solid #E5E7EB', color: '#374151', fontSize: '0.875rem', cursor: 'pointer' },
  btnActionNeutral: { flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem', padding: '0.75rem', background: 'none', border: 'none', color: '#374151', fontSize: '0.875rem', cursor: 'pointer' },
  btnActionDanger: { flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem', padding: '0.75rem', background: 'none', border: 'none', color: '#DC2626', fontSize: '0.875rem', cursor: 'pointer' },
  emptyState: { textAlign: 'center', padding: '2rem', color: '#6B7280', fontSize: '0.875rem' },
  alertError: { backgroundColor: '#FEF2F2', border: '1px solid #FECACA', color: '#B91C1C', padding: '0.75rem 1rem', borderRadius: '6px', fontSize: '0.875rem', marginBottom: '1rem' },
  fieldError: { color: '#DC2626', fontSize: '0.75rem', margin: 0 },

  // Modal
  modalOverlay: { position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 50 },
  modalCard: { backgroundColor: 'white', borderRadius: '8px', width: '100%', maxWidth: '500px', padding: '1.5rem', maxHeight: '90vh', overflowY: 'auto' },
  modalHeader: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' },
  modalTitle: { fontSize: '1.25rem', fontWeight: '600', color: '#111827', margin: 0 },
  closeBtn: { background: 'none', border: 'none', cursor: 'pointer', padding: 0 },
  form: { display: 'flex', flexDirection: 'column', gap: '1.25rem' },
  inputGroup: { display: 'flex', flexDirection: 'column', gap: '0.5rem' },
  row: { display: 'flex', gap: '1rem' },
  label: { fontSize: '0.75rem', fontWeight: '600', color: '#6B7280', letterSpacing: '0.05em' },
  input: { padding: '0.75rem', border: '1px solid #D1D5DB', borderRadius: '6px', fontSize: '0.875rem', outline: 'none', width: '100%', boxSizing: 'border-box' },
  modalFooter: { display: 'flex', justifyContent: 'flex-end', gap: '1rem', marginTop: '1rem' },
  btnCancel: { backgroundColor: 'white', border: '1px solid #D1D5DB', color: '#374151', padding: '0.5rem 1rem', borderRadius: '6px', cursor: 'pointer', fontSize: '0.875rem', fontWeight: '500' },
  btnSubmit: { backgroundColor: '#4F46E5', color: 'white', border: 'none', padding: '0.5rem 1.5rem', borderRadius: '6px', cursor: 'pointer', fontSize: '0.875rem', fontWeight: '500' }
};
