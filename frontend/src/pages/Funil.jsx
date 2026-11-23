import { useEffect, useState } from 'react';
import { Plus, MessageCircle, X } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { chamarApi } from '../services/api';

// Etapas fixas, na ordem do enum EstagioNegociacao (regra 6.3). O valor vai em MAIÚSCULO para a API.
const ETAPAS = [
  { id: 'PRIMEIRO_CONTATO', nome: 'Primeiro contato', cor: '#DBEAFE' },
  { id: 'MOSTRUARIO_ENVIADO', nome: 'Mostruário enviado', cor: '#E0F2FE' },
  { id: 'AGUARDANDO_RESPOSTA', nome: 'Aguardando resposta', cor: '#FFEDD5' },
  { id: 'FECHADA', nome: 'Fechada', cor: '#FED7AA' },
  { id: 'PERDIDA', nome: 'Perdida', cor: '#F3F4F6' },
];

const LIMITE_DESCRICAO = 200;

// "2026-11-15T10:30:00" -> "15/11/2026" (o back já grava no fuso America/Sao_Paulo).
function formatarData(dataHora) {
  if (!dataHora) return '';
  const [data] = dataHora.split('T');
  const [ano, mes, dia] = data.split('-');
  return `${dia}/${mes}/${ano}`;
}

function contarNegociacoes(total) {
  return total === 1 ? '1 negociação' : `${total} negociações`;
}

export default function Funil() {
  const navigate = useNavigate();

  const [negociacoes, setNegociacoes] = useState([]);
  // Clientes ativos: opções do "Nova negociação" e telefone para o link do WhatsApp.
  const [clientes, setClientes] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState('');
  const [movendo, setMovendo] = useState(null);
  const [colunaAlvo, setColunaAlvo] = useState(null);

  // Card que acabou de entrar em FECHADA: abre a pergunta "Criar pedido para este cliente?".
  const [fechada, setFechada] = useState(null);
  const [modalNova, setModalNova] = useState(false);

  // Muda a cada "Tentar de novo" e dispara uma nova carga.
  const [versao, setVersao] = useState(0);

  useEffect(() => {
    // Evita gravar a resposta de uma carga antiga se a tela sair ou recarregar antes dela chegar.
    let ativo = true;
    Promise.all([chamarApi('/negociacoes'), chamarApi('/clientes')])
      .then(([listaNegociacoes, listaClientes]) => {
        if (!ativo) return;
        setNegociacoes(listaNegociacoes);
        setClientes(listaClientes);
      })
      .catch((falha) => ativo && setErro(falha.message))
      .finally(() => ativo && setCarregando(false));
    return () => {
      ativo = false;
    };
  }, [versao]);

  function recarregar() {
    setCarregando(true);
    setErro('');
    setVersao((v) => v + 1);
  }

  const telefonePorCliente = Object.fromEntries(clientes.map((c) => [c.id, c.telefone]));

  // Move o card pela API. A tela só muda depois da resposta: quem atualiza
  // data_ultima_movimentacao e data_fechamento é o back.
  async function moverPara(negociacao, novoEstagio) {
    if (!negociacao || negociacao.estagio === novoEstagio || movendo) return;
    setMovendo(negociacao.id);
    setErro('');
    try {
      const atualizada = await chamarApi(`/negociacoes/${negociacao.id}/etapa`, {
        method: 'PATCH',
        body: JSON.stringify({ estagio: novoEstagio }),
      });
      setNegociacoes((lista) => lista.map((n) => (n.id === atualizada.id ? atualizada : n)));
      if (novoEstagio === 'FECHADA') {
        setFechada(atualizada);
      }
    } catch (falha) {
      setErro(falha.message);
    } finally {
      setMovendo(null);
    }
  }

  // Trocar a etapa pelo select pede confirmação, no mesmo padrão do "Desativar cliente" da ficha.
  // Cancelando, o select volta para a etapa anterior (o arrastar continua sem pergunta).
  function aoTrocarEtapa(evento, card) {
    const novoEstagio = evento.target.value;
    const etapa = ETAPAS.find((e) => e.id === novoEstagio);
    if (!window.confirm(`Mover para ${etapa.nome}?`)) {
      evento.target.value = card.estagio;
      return;
    }
    moverPara(card, novoEstagio);
  }

  function aoSoltar(evento, estagio) {
    evento.preventDefault();
    setColunaAlvo(null);
    const id = Number(evento.dataTransfer.getData('text/plain'));
    moverPara(negociacoes.find((n) => n.id === id), estagio);
  }

  function criarPedido() {
    // O Novo Pedido abre com o cliente da negociação já definido (regra 6.3).
    navigate('/novo-pedido', { state: { idCliente: fechada.idCliente, idNegociacao: fechada.id } });
  }

  function abrirWhatsApp(negociacao) {
    const telefone = telefonePorCliente[negociacao.idCliente];
    const mensagem = `Olá! Tudo bem? Sobre a negociação: ${negociacao.descricao}`;
    window.open(`https://wa.me/${telefone}?text=${encodeURIComponent(mensagem)}`, '_blank', 'noopener');
  }

  function aoCriar(nova) {
    setNegociacoes((lista) => [nova, ...lista]);
    setModalNova(false);
  }

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <div>
          <h1 style={styles.title}>Funil de Negociações</h1>
          <p style={styles.subtitle}>
            {carregando ? 'Carregando...' : `${contarNegociacoes(negociacoes.length)} no total`}
          </p>
        </div>
        <button type="button" style={styles.btnPrimary} onClick={() => setModalNova(true)} disabled={carregando}>
          <Plus size={18} />
          Nova Negociação
        </button>
      </div>

      {erro && (
        <div role="alert" style={styles.alerta}>
          <span>{erro}</span>
          <button type="button" style={styles.btnLink} onClick={recarregar}>Tentar de novo</button>
        </div>
      )}

      <div style={styles.board}>
        {ETAPAS.map((etapa) => {
          const cards = negociacoes.filter((n) => n.estagio === etapa.id);
          return (
            <div
              key={etapa.id}
              style={{ ...styles.column, ...(colunaAlvo === etapa.id ? styles.columnAlvo : {}) }}
              onDragOver={(e) => {
                e.preventDefault();
                setColunaAlvo(etapa.id);
              }}
              onDragLeave={() => setColunaAlvo(null)}
              onDrop={(e) => aoSoltar(e, etapa.id)}
            >
              <div style={{ ...styles.columnHeader, backgroundColor: etapa.cor }}>
                <span style={styles.columnTitle}>{etapa.nome.toUpperCase()}</span>
                <span style={styles.columnCount}>{cards.length}</span>
              </div>

              <div style={styles.cardsContainer}>
                {cards.map((card) => (
                  <div
                    key={card.id}
                    style={{ ...styles.card, ...(movendo === card.id ? styles.cardMovendo : {}) }}
                    draggable={!movendo}
                    onDragStart={(e) => {
                      e.dataTransfer.setData('text/plain', String(card.id));
                      e.dataTransfer.effectAllowed = 'move';
                    }}
                  >
                    <div style={styles.cardHeader}>
                      <span style={styles.cardClient}>{card.cliente}</span>
                      {card.estagio === 'FECHADA' && <span style={styles.badgeGanho}>GANHO</span>}
                    </div>
                    <p style={styles.cardDesc}>{card.descricao}</p>

                    {/* Alternativa ao arrastar, sempre visível (sem depender de hover). */}
                    <label style={styles.moverLabel}>
                      Etapa
                      <select
                        style={styles.moverSelect}
                        value={card.estagio}
                        disabled={movendo !== null}
                        onChange={(e) => aoTrocarEtapa(e, card)}
                      >
                        {ETAPAS.map((opcao) => (
                          <option key={opcao.id} value={opcao.id}>{opcao.nome}</option>
                        ))}
                      </select>
                    </label>

                    <div style={styles.cardFooter}>
                      <span style={styles.cardDate} title="Última movimentação">
                        {formatarData(card.dataUltimaMovimentacao)}
                      </span>
                      {telefonePorCliente[card.idCliente] && (
                        <button
                          type="button"
                          style={styles.btnIcone}
                          onClick={() => abrirWhatsApp(card)}
                          aria-label={`Abrir WhatsApp de ${card.cliente}`}
                          title="Abrir WhatsApp"
                        >
                          <MessageCircle size={16} color="#10B981" />
                        </button>
                      )}
                    </div>
                  </div>
                ))}
                {!carregando && cards.length === 0 && <p style={styles.vazio}>Nenhuma negociação</p>}
              </div>
            </div>
          );
        })}
      </div>

      {fechada && (
        <div style={styles.overlay} role="dialog" aria-modal="true" aria-labelledby="titulo-criar-pedido">
          <div style={styles.modal}>
            <h2 id="titulo-criar-pedido" style={styles.modalTitle}>Criar pedido para este cliente?</h2>
            <p style={styles.modalTexto}>
              A negociação com <strong>{fechada.cliente}</strong> foi movida para Fechada.
            </p>
            <div style={styles.modalAcoes}>
              <button type="button" style={styles.btnSecundario} onClick={() => setFechada(null)}>Agora não</button>
              <button type="button" style={styles.btnPrimary} onClick={criarPedido}>Criar pedido</button>
            </div>
          </div>
        </div>
      )}

      {modalNova && (
        <ModalNovaNegociacao clientes={clientes} onFechar={() => setModalNova(false)} onCriada={aoCriar} />
      )}
    </div>
  );
}

function ModalNovaNegociacao({ clientes, onFechar, onCriada }) {
  const [idCliente, setIdCliente] = useState('');
  const [descricao, setDescricao] = useState('');
  const [errosCampos, setErrosCampos] = useState({});
  const [erroGeral, setErroGeral] = useState('');
  const [salvando, setSalvando] = useState(false);

  // GET /clientes devolve só os ativos; o filtro reforça: cliente desativado não vira opção (regra 6.3).
  const opcoes = clientes.filter((c) => c.ativo);

  async function salvar(evento) {
    evento.preventDefault();
    const erros = {};
    if (!idCliente) erros.idCliente = 'Selecione o cliente';
    if (!descricao.trim()) erros.descricao = 'A descrição é obrigatória';
    else if (descricao.trim().length > LIMITE_DESCRICAO) {
      erros.descricao = `A descrição pode ter no máximo ${LIMITE_DESCRICAO} caracteres`;
    }
    setErrosCampos(erros);
    setErroGeral('');
    if (Object.keys(erros).length) return;

    setSalvando(true);
    try {
      const nova = await chamarApi('/negociacoes', {
        method: 'POST',
        body: JSON.stringify({ idCliente: Number(idCliente), descricao: descricao.trim() }),
      });
      onCriada(nova);
    } catch (falha) {
      if (falha.detalhes?.campos?.length) {
        setErrosCampos(Object.fromEntries(falha.detalhes.campos.map((c) => [c.campo, c.mensagem])));
      } else {
        setErroGeral(falha.message);
      }
    } finally {
      setSalvando(false);
    }
  }

  return (
    <div style={styles.overlay} role="dialog" aria-modal="true" aria-labelledby="titulo-nova-negociacao">
      <form style={styles.modal} onSubmit={salvar} noValidate>
        <div style={styles.modalTopo}>
          <h2 id="titulo-nova-negociacao" style={styles.modalTitle}>Nova Negociação</h2>
          <button type="button" style={styles.btnIcone} onClick={onFechar} aria-label="Fechar">
            <X size={18} />
          </button>
        </div>

        {erroGeral && <div role="alert" style={styles.alerta}>{erroGeral}</div>}

        <label style={styles.campoLabel} htmlFor="nova-cliente">Cliente *</label>
        <select
          id="nova-cliente"
          style={styles.input}
          value={idCliente}
          onChange={(e) => { setIdCliente(e.target.value); setErrosCampos((atual) => ({ ...atual, idCliente: undefined })); }}
        >
          <option value="">Selecione o cliente</option>
          {opcoes.map((c) => (
            <option key={c.id} value={c.id}>{c.nome}</option>
          ))}
        </select>
        {errosCampos.idCliente && <span style={styles.erroCampo}>{errosCampos.idCliente}</span>}

        <label style={styles.campoLabel} htmlFor="nova-descricao">Descrição *</label>
        <input
          id="nova-descricao"
          style={styles.input}
          value={descricao}
          maxLength={LIMITE_DESCRICAO}
          placeholder="Ex.: Proposta de Linho Natural - 200 m"
          onChange={(e) => { setDescricao(e.target.value); setErrosCampos((atual) => ({ ...atual, descricao: undefined })); }}
        />
        {errosCampos.descricao && <span style={styles.erroCampo}>{errosCampos.descricao}</span>}

        <p style={styles.modalTexto}>A negociação começa em Primeiro contato.</p>

        <div style={styles.modalAcoes}>
          <button type="button" style={styles.btnSecundario} onClick={onFechar}>Cancelar</button>
          <button type="submit" style={styles.btnPrimary} disabled={salvando}>
            {salvando ? 'Salvando...' : 'Criar negociação'}
          </button>
        </div>
      </form>
    </div>
  );
}

const styles = {
  // width 0 + minWidth 100%: o quadro não alarga a página (o menu lateral ficaria empurrando a coluna Perdida para fora);
  // quem rola na horizontal, com cabeçalho e contador de cada coluna visíveis, é o próprio quadro.
  container: { display: 'flex', flexDirection: 'column', height: '100%', fontFamily: 'sans-serif', width: 0, minWidth: '100%' },
  header: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' },
  title: { fontSize: '1.5rem', fontWeight: '600', color: '#111827', margin: '0 0 0.25rem 0' },
  subtitle: { fontSize: '0.875rem', color: '#6B7280', margin: 0 },
  btnPrimary: { display: 'flex', alignItems: 'center', gap: '0.5rem', backgroundColor: '#4F46E5', color: 'white', border: 'none', padding: '0.5rem 1rem', borderRadius: '6px', fontSize: '0.875rem', fontWeight: '500', cursor: 'pointer' },
  btnSecundario: { backgroundColor: 'white', color: '#374151', border: '1px solid #D1D5DB', padding: '0.5rem 1rem', borderRadius: '6px', fontSize: '0.875rem', fontWeight: '500', cursor: 'pointer' },
  btnLink: { background: 'none', border: 'none', color: '#991B1B', fontWeight: '600', textDecoration: 'underline', cursor: 'pointer', fontSize: '0.875rem' },
  btnIcone: { background: 'none', border: 'none', padding: '0.25rem', cursor: 'pointer', display: 'flex', alignItems: 'center' },
  alerta: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '1rem', backgroundColor: '#FEE2E2', color: '#991B1B', padding: '0.75rem 1rem', borderRadius: '6px', fontSize: '0.875rem', marginBottom: '1rem' },
  board: { display: 'flex', gap: '1rem', overflowX: 'auto', flex: 1, paddingBottom: '1rem' },
  column: { flex: '1 1 0', minWidth: '210px', backgroundColor: '#F9FAFB', borderRadius: '8px', display: 'flex', flexDirection: 'column', border: '2px solid transparent' },
  columnAlvo: { borderColor: '#A5B4FC' },
  columnHeader: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '0.5rem', padding: '0.75rem 1rem', borderTopLeftRadius: '6px', borderTopRightRadius: '6px', marginBottom: '0.5rem' },
  columnTitle: { minWidth: 0, fontSize: '0.75rem', fontWeight: '600', color: '#374151', letterSpacing: '0.05em' },
  columnCount: { flexShrink: 0, backgroundColor: 'rgba(255,255,255,0.6)', padding: '0.1rem 0.5rem', borderRadius: '999px', fontSize: '0.75rem', color: '#374151', fontWeight: '600' },
  cardsContainer: { padding: '0 0.5rem 0.5rem', display: 'flex', flexDirection: 'column', gap: '0.75rem', flex: 1, overflowY: 'auto' },
  card: { backgroundColor: 'white', padding: '1rem', borderRadius: '6px', boxShadow: '0 1px 2px rgba(0,0,0,0.05)', border: '1px solid #E5E7EB', cursor: 'grab' },
  cardMovendo: { opacity: 0.5 },
  cardHeader: { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '0.5rem', marginBottom: '0.5rem' },
  cardClient: { fontSize: '0.875rem', fontWeight: '600', color: '#111827' },
  badgeGanho: { backgroundColor: '#D1FAE5', color: '#065F46', fontSize: '0.65rem', padding: '0.15rem 0.4rem', borderRadius: '4px', fontWeight: '600' },
  cardDesc: { fontSize: '0.8rem', color: '#6B7280', margin: '0 0 0.75rem 0', lineHeight: '1.4' },
  moverLabel: { display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.75rem', color: '#6B7280', marginBottom: '0.75rem' },
  moverSelect: { flex: 1, fontSize: '0.75rem', padding: '0.25rem', border: '1px solid #D1D5DB', borderRadius: '4px', backgroundColor: 'white' },
  cardFooter: { display: 'flex', justifyContent: 'space-between', alignItems: 'center' },
  cardDate: { fontSize: '0.75rem', color: '#9CA3AF' },
  vazio: { fontSize: '0.8rem', color: '#9CA3AF', textAlign: 'center', margin: '1rem 0' },
  overlay: { position: 'fixed', inset: 0, backgroundColor: 'rgba(17,24,39,0.45)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 50 },
  modal: { backgroundColor: 'white', borderRadius: '8px', padding: '1.5rem', width: '100%', maxWidth: '440px', display: 'flex', flexDirection: 'column', boxShadow: '0 10px 25px rgba(0,0,0,0.15)' },
  modalTopo: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' },
  modalTitle: { fontSize: '1.125rem', fontWeight: '600', color: '#111827', margin: 0 },
  modalTexto: { fontSize: '0.875rem', color: '#4B5563', margin: '0.75rem 0 0' },
  modalAcoes: { display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1.5rem' },
  campoLabel: { fontSize: '0.875rem', fontWeight: '500', color: '#374151', margin: '0.75rem 0 0.25rem' },
  input: { padding: '0.5rem 0.75rem', border: '1px solid #D1D5DB', borderRadius: '6px', fontSize: '0.875rem' },
  erroCampo: { fontSize: '0.75rem', color: '#DC2626', marginTop: '0.25rem' },
};
