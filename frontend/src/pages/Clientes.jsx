import { useCallback, useEffect, useState } from 'react';
import { Search, Plus, MessageSquare, X } from 'lucide-react';
import FichaCliente from '../components/FichaCliente';
import { listarClientes, criarCliente, atualizarCliente, reativarCliente } from '../services/clientes';
import { listarTags, consultarUsoTag, excluirTag } from '../services/tags';
import {
  formatarTelefone,
  mascararTelefone,
  soDigitos,
  telefoneParaApi,
  telefoneParaCampo,
} from '../utils/formatadores';

const pluralClientes = (n) => `${n} ${n === 1 ? 'cliente' : 'clientes'}`;

export default function Clientes() {
  const [searchTerm, setSearchTerm] = useState('');
  const [idTagSelecionada, setIdTagSelecionada] = useState(null); // null = "Todos"
  const [mostrarInativos, setMostrarInativos] = useState(false);
  const [clientes, setClientes] = useState([]);
  const [tags, setTags] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState('');
  const [idFicha, setIdFicha] = useState(null);
  const [modal, setModal] = useState(null); // null = fechado; {} = novo; cliente = edição

  const carregarTags = useCallback(async () => {
    try {
      setTags(await listarTags());
    } catch (e) {
      setErro(e.message);
    }
  }, []);

  const carregarClientes = useCallback(async () => {
    setErro('');
    try {
      setClientes(await listarClientes({
        busca: searchTerm.trim(),
        idTag: idTagSelecionada,
        incluirInativos: mostrarInativos,
      }));
    } catch (e) {
      setErro(e.message);
    } finally {
      setCarregando(false);
    }
  }, [searchTerm, idTagSelecionada, mostrarInativos]);

  // Tags para o filtro e para as sugestões do modal; recarregadas depois de salvar ou excluir.
  useEffect(() => {
    let cancelado = false;
    listarTags()
      .then((dados) => { if (!cancelado) setTags(dados); })
      .catch((e) => { if (!cancelado) setErro(e.message); });
    return () => { cancelado = true; };
  }, []);

  // A busca é feita pela API; espera o usuário parar de digitar.
  useEffect(() => {
    const espera = setTimeout(carregarClientes, 300);
    return () => clearTimeout(espera);
  }, [carregarClientes]);

  const tagSelecionada = tags.find((t) => t.id === idTagSelecionada);

  const handleWhatsAppList = (e, cliente) => {
    e.stopPropagation(); // Evita que o clique no botão abra a ficha
    let mensagem = `Olá ${cliente.nome}, como podemos ajudar hoje?`;
    if (tagSelecionada) {
      mensagem = `Olá ${cliente.nome}, acabou de chegar um novo mostruário de ${tagSelecionada.nome}, posso agendar uma visita?`;
    }
    // wa.me/<telefone> exatamente como gravado (já tem o DDI 55), sem acrescentar nada.
    const url = `https://wa.me/${cliente.telefone}?text=${encodeURIComponent(mensagem)}`;
    window.open(url, '_blank', 'noopener,noreferrer');
  };

  const handleReativar = async (e, cliente) => {
    e.stopPropagation();
    try {
      await reativarCliente(cliente.id);
      await carregarClientes();
    } catch (err) {
      setErro(err.message);
    }
  };

  // Excluir a tag remove os vínculos com os clientes; confirma informando em quantos clientes ela está.
  const handleExcluirTag = async (tag) => {
    try {
      const uso = await consultarUsoTag(tag.id);
      const quantos = pluralClientes(uso.quantidadeClientes);
      if (!window.confirm(`A tag "${tag.nome}" está em ${quantos}. Excluir remove a tag desses clientes. Deseja excluir?`)) {
        return;
      }
      await excluirTag(tag.id);
      if (idTagSelecionada === tag.id) setIdTagSelecionada(null);
      await Promise.all([carregarTags(), carregarClientes()]);
      if (idFicha) setIdFicha(null);
    } catch (e) {
      setErro(e.message);
    }
  };

  return (
    <div style={styles.layoutWrapper}>

      <div style={styles.mainContainer}>
        <div style={styles.header}>
          <div>
            <h1 style={styles.title}>Clientes</h1>
            <p style={styles.subtitle}>{pluralClientes(clientes.length)}</p>
          </div>
          <button style={styles.btnPrimary} onClick={() => setModal({})}>
            <Plus size={18} />
            Novo Cliente
          </button>
        </div>

        <div style={styles.toolbar}>
          <div style={styles.searchBox}>
            <Search size={18} color="#9CA3AF" style={styles.searchIcon} />
            <input
              type="text"
              placeholder="Buscar por nome..."
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

        {/* Filtro por tag: "Todos" + uma etiqueta por tag, cada uma com "x" para excluí-la */}
        <div style={styles.tagFilter}>
          <button
            onClick={() => setIdTagSelecionada(null)}
            style={{ ...styles.chip, padding: '0.3rem 0.75rem', ...(idTagSelecionada === null ? styles.chipAtivo : {}) }}
          >
            Todos
          </button>
          {tags.map((tag) => (
            <span key={tag.id} style={{ ...styles.chip, ...(idTagSelecionada === tag.id ? styles.chipAtivo : {}) }}>
              <button onClick={() => setIdTagSelecionada(tag.id)} style={styles.chipNome}>{tag.nome}</button>
              <button
                onClick={() => handleExcluirTag(tag)}
                style={styles.chipX}
                aria-label={`Excluir a tag ${tag.nome}`}
                title="Excluir tag"
              >
                <X size={12} />
              </button>
            </span>
          ))}
        </div>

        {erro && <div role="alert" style={styles.alertError}>{erro}</div>}

        <div style={styles.list}>
          {clientes.map((cliente) => {
            const isSelected = idFicha === cliente.id;

            return (
              <div
                key={cliente.id}
                style={{
                  ...styles.card,
                  borderColor: isSelected ? '#4F46E5' : '#E5E7EB',
                  borderWidth: isSelected ? '2px' : '1px',
                  opacity: cliente.ativo ? 1 : 0.7,
                }}
                // A ação de abrir a ficha volta para o CARD INTEIRO (conforme o teu pedido)
                onClick={() => setIdFicha(cliente.id)}
              >
                <div style={styles.cardHeader}>
                  <h2 style={styles.clientName}>{cliente.nome}</h2>
                  {!cliente.ativo && <span style={styles.badgeInativo}>Inativo</span>}
                </div>

                <p style={styles.clientPhone}>{formatarTelefone(cliente.telefone)}</p>

                <div style={styles.cardFooter}>
                  <div style={styles.tagsContainer}>
                    {cliente.tags.map((tag) => (
                      <span key={tag.id} style={styles.tagBadge}>{tag.nome}</span>
                    ))}
                  </div>

                  {cliente.ativo ? (
                    <button onClick={(e) => handleWhatsAppList(e, cliente)} style={styles.btnWhatsApp}>
                      <MessageSquare size={16} />
                      WhatsApp
                    </button>
                  ) : (
                    <button onClick={(e) => handleReativar(e, cliente)} style={styles.btnReativar}>
                      Reativar
                    </button>
                  )}
                </div>
              </div>
            );
          })}
          {carregando && <p style={styles.emptyState}>Carregando clientes...</p>}
          {!carregando && !erro && clientes.length === 0 && (
            <p style={styles.emptyState}>Nenhum cliente encontrado.</p>
          )}
        </div>
      </div>

      {idFicha && (
        <FichaCliente
          key={idFicha}
          idCliente={idFicha}
          onClose={() => setIdFicha(null)}
          onEditar={(cliente) => setModal(cliente)}
          onAlterado={carregarClientes}
        />
      )}

      {modal && (
        <ModalCliente
          cliente={modal.id ? modal : null}
          tagsExistentes={tags}
          onClose={() => setModal(null)}
          onSaved={(salvo) => {
            setModal(null);
            carregarTags();
            carregarClientes();
            // Reabre a ficha para refletir os dados salvos.
            setIdFicha(null);
            setTimeout(() => setIdFicha(salvo.id), 0);
          }}
        />
      )}
    </div>
  );
}

// Cadastro e edição. Telefone digitado com máscara e enviado só com dígitos e DDI 55 (regra 6.4).
// As tags são enviadas pelo nome; o back cria na hora as que ainda não existem.
function ModalCliente({ cliente, tagsExistentes, onClose, onSaved }) {
  const [form, setForm] = useState({
    nome: cliente?.nome ?? '',
    telefone: cliente ? telefoneParaCampo(cliente.telefone) : '',
    email: cliente?.email ?? '',
    cpfCnpj: cliente?.cpfCnpj ?? '',
    cidade: cliente?.cidade ?? '',
    uf: cliente?.uf ?? '',
  });
  const [tags, setTags] = useState(cliente ? cliente.tags.map((t) => t.nome) : []);
  const [novaTag, setNovaTag] = useState('');
  const [erros, setErros] = useState({});
  const [erroGeral, setErroGeral] = useState('');
  const [salvando, setSalvando] = useState(false);

  const limparErro = (campo) => setErros((atual) => ({ ...atual, [campo]: undefined }));

  const handleChange = (e) => {
    const { name, value } = e.target;
    const novo = name === 'telefone' ? mascararTelefone(value) : name === 'uf' ? value.toUpperCase().slice(0, 2) : value;
    setForm({ ...form, [name]: novo });
    limparErro(name);
  };

  // Reaproveita a tag existente sem diferenciar maiúsculas (regra 6.4) e evita repetir na lista.
  const adicionarTag = () => {
    const nome = novaTag.trim();
    if (!nome) return;
    const existente = tagsExistentes.find((t) => t.nome.toLowerCase() === nome.toLowerCase());
    const final = existente ? existente.nome : nome;
    if (!tags.some((t) => t.toLowerCase() === final.toLowerCase())) setTags([...tags, final]);
    setNovaTag('');
    limparErro('tags');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErroGeral('');

    const novosErros = {};
    if (!form.nome.trim()) novosErros.nome = 'O nome é obrigatório';
    const digitosTel = soDigitos(form.telefone);
    if (digitosTel.length < 10 || digitosTel.length > 11) {
      novosErros.telefone = 'Informe o DDD e o número (ex.: (62) 99999-8888)';
    }
    const digitosDoc = soDigitos(form.cpfCnpj);
    if (digitosDoc && digitosDoc.length !== 11 && digitosDoc.length !== 14) {
      novosErros.cpfCnpj = 'O CPF deve ter 11 dígitos e o CNPJ, 14';
    }
    if (form.uf && !/^[A-Z]{2}$/.test(form.uf)) novosErros.uf = 'Informe a UF com 2 letras (ex.: GO)';
    if (Object.keys(novosErros).length > 0) {
      setErros(novosErros);
      return;
    }

    // Tag digitada e ainda não adicionada entra junto, para não se perder ao salvar.
    const tagsEnvio = novaTag.trim() && !tags.some((t) => t.toLowerCase() === novaTag.trim().toLowerCase())
      ? [...tags, novaTag.trim()]
      : tags;

    const dados = {
      nome: form.nome.trim(),
      telefone: telefoneParaApi(form.telefone),
      email: form.email.trim() || null,
      cpfCnpj: digitosDoc || null,
      cidade: form.cidade.trim() || null,
      uf: form.uf || null,
      tags: tagsEnvio,
    };

    setSalvando(true);
    try {
      const salvo = await (cliente ? atualizarCliente(cliente.id, dados) : criarCliente(dados));
      onSaved(salvo);
    } catch (err) {
      // "campos" da API: cada erro de validação vai abaixo do campo correspondente.
      const campos = err.detalhes?.campos ?? [];
      if (err.status === 409) {
        // CPF/CNPJ já cadastrado: a API devolve 409 sem "campos"; o erro vai abaixo do campo.
        setErros({ cpfCnpj: err.message });
      } else if (campos.length > 0) {
        setErros(Object.fromEntries(campos.map((c) => [c.campo.startsWith('tags') ? 'tags' : c.campo, c.mensagem])));
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
          <h2 style={styles.modalTitle}>{cliente ? 'Editar Cliente' : 'Novo Cliente'}</h2>
          <button type="button" onClick={onClose} aria-label="Fechar" style={styles.closeBtn}><X size={20} color="#6B7280" /></button>
        </div>

        <form onSubmit={handleSubmit} noValidate style={styles.form}>
          <div style={styles.inputGroup}>
            <label style={styles.label}>NOME *</label>
            <input name="nome" type="text" value={form.nome} onChange={handleChange} style={styles.input} />
            {campoErro('nome')}
          </div>

          <div style={styles.row}>
            <div style={{ ...styles.inputGroup, flex: 1 }}>
              <label style={styles.label}>TELEFONE (WHATSAPP) *</label>
              <input
                name="telefone" type="text" inputMode="tel" placeholder="(62) 99999-8888"
                value={form.telefone} onChange={handleChange} style={styles.input}
              />
              {campoErro('telefone')}
            </div>
            <div style={{ ...styles.inputGroup, flex: 1 }}>
              <label style={styles.label}>E-MAIL</label>
              <input name="email" type="text" value={form.email} onChange={handleChange} style={styles.input} />
              {campoErro('email')}
            </div>
          </div>

          <div style={styles.inputGroup}>
            <label style={styles.label}>CPF/CNPJ</label>
            <input name="cpfCnpj" type="text" value={form.cpfCnpj} onChange={handleChange} style={styles.input} />
            {campoErro('cpfCnpj')}
          </div>

          <div style={styles.row}>
            <div style={{ ...styles.inputGroup, flex: 3 }}>
              <label style={styles.label}>CIDADE</label>
              <input name="cidade" type="text" value={form.cidade} onChange={handleChange} style={styles.input} />
              {campoErro('cidade')}
            </div>
            <div style={{ ...styles.inputGroup, flex: 1 }}>
              <label style={styles.label}>UF</label>
              <input name="uf" type="text" placeholder="GO" value={form.uf} onChange={handleChange} style={styles.input} />
              {campoErro('uf')}
            </div>
          </div>

          <div style={styles.inputGroup}>
            <label style={styles.label}>TAGS</label>
            <div style={styles.tagsContainer}>
              {tags.map((tag) => (
                <span key={tag} style={styles.tagBadge}>
                  {tag}
                  <button
                    type="button"
                    onClick={() => setTags(tags.filter((t) => t !== tag))}
                    style={styles.tagRemover}
                    aria-label={`Remover a tag ${tag}`}
                  >
                    <X size={12} />
                  </button>
                </span>
              ))}
            </div>
            <div style={styles.row}>
              <input
                type="text" list="tags-existentes" placeholder="Digite uma tag e pressione Enter"
                value={novaTag} onChange={(e) => setNovaTag(e.target.value)}
                onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); adicionarTag(); } }}
                style={styles.input}
              />
              <datalist id="tags-existentes">
                {tagsExistentes.map((t) => <option key={t.id} value={t.nome} />)}
              </datalist>
              <button type="button" onClick={adicionarTag} style={styles.btnCancel}>Adicionar</button>
            </div>
            {campoErro('tags')}
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

const styles = {
  layoutWrapper: { display: 'flex', gap: '1.5rem', height: '100%', alignItems: 'flex-start' },
  mainContainer: { flex: 1, fontFamily: 'sans-serif' },
  header: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' },
  title: { fontSize: '1.5rem', fontWeight: '600', color: '#111827', margin: '0 0 0.25rem 0' },
  subtitle: { fontSize: '0.875rem', color: '#6B7280', margin: 0 },
  btnPrimary: { display: 'flex', alignItems: 'center', gap: '0.5rem', backgroundColor: '#4F46E5', color: 'white', border: 'none', padding: '0.5rem 1rem', borderRadius: '6px', fontSize: '0.875rem', fontWeight: '500', cursor: 'pointer' },
  toolbar: { display: 'flex', alignItems: 'center', gap: '1.5rem', marginBottom: '1rem', flexWrap: 'wrap' },
  searchBox: { position: 'relative', flex: 1, minWidth: '250px' },
  searchIcon: { position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)' },
  searchInput: { width: '100%', padding: '0.5rem 1rem 0.5rem 2.5rem', border: '1px solid #D1D5DB', borderRadius: '6px', fontSize: '0.875rem', outline: 'none', boxSizing: 'border-box' },
  toggle: { display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.875rem', color: '#374151' },
  tagFilter: { display: 'flex', gap: '0.5rem', flexWrap: 'wrap', marginBottom: '1.5rem' },
  chip: { display: 'inline-flex', alignItems: 'center', backgroundColor: 'white', border: '1px solid #D1D5DB', borderRadius: '9999px', fontSize: '0.8rem', color: '#374151', padding: 0, cursor: 'pointer' },
  chipAtivo: { backgroundColor: '#EEF2FF', borderColor: '#4F46E5', color: '#4F46E5' },
  chipNome: { background: 'none', border: 'none', color: 'inherit', fontSize: 'inherit', padding: '0.3rem 0.5rem 0.3rem 0.75rem', cursor: 'pointer' },
  chipX: { display: 'flex', alignItems: 'center', background: 'none', border: 'none', color: '#DC2626', padding: '0.3rem 0.5rem 0.3rem 0.1rem', cursor: 'pointer' },
  list: { display: 'flex', flexDirection: 'column', gap: '1rem' },
  card: { backgroundColor: 'white', borderRadius: '8px', padding: '1.25rem', boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.05)', cursor: 'pointer', transition: 'all 0.2s', borderStyle: 'solid' },
  cardHeader: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.25rem' },
  clientName: { fontSize: '1rem', fontWeight: '600', color: '#111827', margin: 0 },
  badgeInativo: { backgroundColor: '#F3F4F6', color: '#6B7280', padding: '0.25rem 0.5rem', borderRadius: '4px', fontSize: '0.75rem', fontWeight: '500' },
  clientPhone: { fontSize: '0.875rem', color: '#6B7280', margin: '0 0 1rem 0' },
  cardFooter: { display: 'flex', justifyContent: 'space-between', alignItems: 'center' },
  tagsContainer: { display: 'flex', gap: '0.5rem', flexWrap: 'wrap' },
  tagBadge: { display: 'inline-flex', alignItems: 'center', gap: '0.25rem', backgroundColor: '#EEF2FF', color: '#4F46E5', padding: '0.25rem 0.5rem', borderRadius: '4px', fontSize: '0.75rem', fontWeight: '500' },
  tagRemover: { display: 'flex', alignItems: 'center', background: 'none', border: 'none', color: '#4F46E5', padding: 0, cursor: 'pointer' },
  btnWhatsApp: { display: 'flex', alignItems: 'center', gap: '0.35rem', backgroundColor: '#10B981', color: 'white', border: 'none', padding: '0.35rem 0.75rem', borderRadius: '6px', fontSize: '0.75rem', fontWeight: '500', cursor: 'pointer' },
  btnReativar: { backgroundColor: 'white', color: '#374151', border: '1px solid #D1D5DB', padding: '0.35rem 0.75rem', borderRadius: '6px', fontSize: '0.75rem', fontWeight: '500', cursor: 'pointer' },
  emptyState: { textAlign: 'center', color: '#6B7280', padding: '2rem 0' },
  alertError: { backgroundColor: '#FEF2F2', border: '1px solid #FECACA', color: '#B91C1C', padding: '0.75rem 1rem', borderRadius: '6px', fontSize: '0.875rem', marginBottom: '1rem' },
  fieldError: { color: '#DC2626', fontSize: '0.75rem', margin: 0 },

  // Modal
  modalOverlay: { position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 50 },
  modalCard: { backgroundColor: 'white', borderRadius: '8px', width: '100%', maxWidth: '560px', padding: '1.5rem', maxHeight: '90vh', overflowY: 'auto' },
  modalHeader: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' },
  modalTitle: { fontSize: '1.25rem', fontWeight: '600', color: '#111827', margin: 0 },
  closeBtn: { background: 'none', border: 'none', cursor: 'pointer', padding: 0 },
  form: { display: 'flex', flexDirection: 'column', gap: '1.25rem' },
  inputGroup: { display: 'flex', flexDirection: 'column', gap: '0.5rem' },
  row: { display: 'flex', gap: '1rem', alignItems: 'flex-start' },
  label: { fontSize: '0.75rem', fontWeight: '600', color: '#6B7280', letterSpacing: '0.05em' },
  input: { padding: '0.75rem', border: '1px solid #D1D5DB', borderRadius: '6px', fontSize: '0.875rem', outline: 'none', width: '100%', boxSizing: 'border-box' },
  modalFooter: { display: 'flex', justifyContent: 'flex-end', gap: '1rem', marginTop: '1rem' },
  btnCancel: { backgroundColor: 'white', border: '1px solid #D1D5DB', color: '#374151', padding: '0.6rem 1rem', borderRadius: '6px', cursor: 'pointer', fontSize: '0.875rem', fontWeight: '500', whiteSpace: 'nowrap' },
  btnSubmit: { backgroundColor: '#4F46E5', color: 'white', border: 'none', padding: '0.5rem 1.5rem', borderRadius: '6px', cursor: 'pointer', fontSize: '0.875rem', fontWeight: '500' },
};
