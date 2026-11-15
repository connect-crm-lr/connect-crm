import { useEffect, useState } from 'react';
import { X } from 'lucide-react';
import { listarInteracoes, registrarInteracao } from '../services/interacoes';
import { formatarData, hojeLocal } from '../utils/formatadores';
import { estilosFicha as s } from '../utils/estilosFicha';

// Valores gravados em MAIÚSCULO (TipoInteracao); a tela mostra o texto de exibição.
const TIPOS = [
  { valor: 'WHATSAPP', texto: 'WhatsApp' },
  { valor: 'LIGACAO', texto: 'Ligação' },
  { valor: 'VISITA', texto: 'Visita' },
  { valor: 'OUTRO', texto: 'Outro' },
];
const textoTipo = (valor) => TIPOS.find((t) => t.valor === valor)?.texto ?? valor;

// Aba Interações da ficha: conversas já realizadas com o cliente (lista + Registrar interação).
export default function AbaInteracoes({ idCliente }) {
  const [interacoes, setInteracoes] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState('');
  const [modalAberto, setModalAberto] = useState(false);

  useEffect(() => {
    let cancelado = false;
    listarInteracoes(idCliente)
      .then((dados) => { if (!cancelado) setInteracoes(dados); })
      .catch((e) => { if (!cancelado) setErro(e.message); })
      .finally(() => { if (!cancelado) setCarregando(false); });
    return () => { cancelado = true; };
  }, [idCliente]);

  // Mais recentes primeiro.
  const ordenadas = [...interacoes].sort(
    (a, b) => b.dataInteracao.localeCompare(a.dataInteracao) || b.id - a.id,
  );

  return (
    <div>
      <button style={s.btnAcao} onClick={() => setModalAberto(true)}>+ Registrar interação</button>
      {erro && <p role="alert" style={s.erro}>{erro}</p>}
      {carregando && <p style={s.placeholder}>Carregando interações...</p>}
      {!carregando && !erro && ordenadas.length === 0 && <p style={s.placeholder}>Nenhuma interação registrada.</p>}

      <div style={s.lista}>
        {ordenadas.map((i) => (
          <div key={i.id} style={s.item}>
            <div style={s.itemTopo}>
              <span style={s.itemTitulo}>{textoTipo(i.tipo)}</span>
              <span style={s.itemData}>{formatarData(i.dataInteracao)}</span>
            </div>
            <p style={s.itemTexto}>{i.descricao}</p>
          </div>
        ))}
      </div>

      {modalAberto && (
        <ModalInteracao
          idCliente={idCliente}
          onClose={() => setModalAberto(false)}
          onSaved={(nova) => {
            setModalAberto(false);
            setInteracoes((atual) => [...atual, nova]);
          }}
        />
      )}
    </div>
  );
}

// Modal Registrar interação: tipo, data e texto (AGENTS 7.3). Erros aparecem abaixo do campo.
function ModalInteracao({ idCliente, onClose, onSaved }) {
  const [tipo, setTipo] = useState('');
  const [data, setData] = useState(hojeLocal);
  const [descricao, setDescricao] = useState('');
  const [erros, setErros] = useState({});
  const [erroGeral, setErroGeral] = useState('');
  const [salvando, setSalvando] = useState(false);

  const limparErro = (campo) => setErros((atual) => ({ ...atual, [campo]: undefined }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErroGeral('');

    const novosErros = {};
    if (!tipo) novosErros.tipo = 'O tipo é obrigatório';
    if (!data) novosErros.dataInteracao = 'A data da interação é obrigatória';
    else if (data > hojeLocal()) novosErros.dataInteracao = 'A data da interação não pode ser no futuro';
    if (!descricao.trim()) novosErros.descricao = 'A descrição é obrigatória';
    if (Object.keys(novosErros).length > 0) {
      setErros(novosErros);
      return;
    }

    setSalvando(true);
    try {
      onSaved(await registrarInteracao(idCliente, { tipo, dataInteracao: data, descricao: descricao.trim() }));
    } catch (err) {
      // "campos" da API: cada erro de validação vai abaixo do campo correspondente.
      const campos = err.detalhes?.campos ?? [];
      if (campos.length > 0) {
        setErros(Object.fromEntries(campos.map((c) => [c.campo, c.mensagem])));
      } else {
        setErroGeral(err.message);
      }
    } finally {
      setSalvando(false);
    }
  };

  const campoErro = (nome) => erros[nome] && <p role="alert" style={s.fieldError}>{erros[nome]}</p>;

  return (
    <div style={s.modalOverlay}>
      <div style={s.modalCard}>
        <div style={s.modalHeader}>
          <h2 style={s.modalTitle}>Registrar interação</h2>
          <button type="button" onClick={onClose} aria-label="Fechar" style={s.closeBtn}><X size={20} color="#6B7280" /></button>
        </div>

        <form onSubmit={handleSubmit} noValidate style={s.form}>
          <div style={s.row}>
            <div style={{ ...s.inputGroup, flex: 1 }}>
              <label style={s.label}>TIPO *</label>
              <select value={tipo} onChange={(e) => { setTipo(e.target.value); limparErro('tipo'); }} style={s.input}>
                <option value="">Selecione o tipo</option>
                {TIPOS.map((t) => <option key={t.valor} value={t.valor}>{t.texto}</option>)}
              </select>
              {campoErro('tipo')}
            </div>
            <div style={{ ...s.inputGroup, flex: 1 }}>
              <label style={s.label}>DATA *</label>
              <input
                type="date" value={data} max={hojeLocal()}
                onChange={(e) => { setData(e.target.value); limparErro('dataInteracao'); }}
                style={s.input}
              />
              {campoErro('dataInteracao')}
            </div>
          </div>

          <div style={s.inputGroup}>
            <label style={s.label}>DESCRIÇÃO *</label>
            <textarea
              rows={4} value={descricao}
              onChange={(e) => { setDescricao(e.target.value); limparErro('descricao'); }}
              style={s.input}
            />
            {campoErro('descricao')}
          </div>

          {erroGeral && <p role="alert" style={s.fieldError}>{erroGeral}</p>}

          <div style={s.modalFooter}>
            <button type="button" onClick={onClose} style={s.btnCancel}>Cancelar</button>
            <button type="submit" disabled={salvando} style={s.btnSubmit}>{salvando ? 'Salvando...' : 'Registrar'}</button>
          </div>
        </form>
      </div>
    </div>
  );
}
