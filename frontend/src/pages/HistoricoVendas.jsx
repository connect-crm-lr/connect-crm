import { useEffect, useState } from 'react';
import { FileText, Calendar, RefreshCw, Search } from 'lucide-react';
import { chamarApi } from '../services/api';

// Histórico de vendas (RF-022): consulta os pedidos por período e status e mostra o total do período.
// Os dados vêm da API: GET /pedidos?inicio&fim&status (lista) e GET /pedidos/total?inicio&fim&status.
// O total não inclui pedido CANCELADO (regra 6.2); o pedido cancelado aparece na lista, com o status.

const STATUS = {
  EM_ABERTO: 'Em aberto',
  PAGO: 'Pago',
  CANCELADO: 'Cancelado',
};

const FORMAS_PAGAMENTO = {
  A_VISTA: 'À vista',
  CARTAO: 'Cartão',
  BOLETO: 'Boleto',
  CHEQUE: 'Cheque',
};

const formatoMoeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const formatoMetragem = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2 });

const dinheiro = (valor) => formatoMoeda.format(valor).split(String.fromCharCode(160)).join(' '); // o Intl separa "R$" do valor com espaço não separável (código 160); troca por espaço comum: "R$ 34,00"
const metros = (valor) => `${formatoMetragem.format(valor)} m`;

// "#P-0000": o número do pedido é o id, com zeros à esquerda.
const numeroPedido = (id) => `#P-${String(id).padStart(4, '0')}`;

// "2026-11-03T10:00:00" -> "03/11/2026". Quebra o texto em vez de usar Date, para o fuso do navegador não mudar o dia.
const dataBr = (dataHora) => {
  const [ano, mes, dia] = dataHora.slice(0, 10).split('-');
  return `${dia}/${mes}/${ano}`;
};

const doisDigitos = (n) => String(n).padStart(2, '0');
// yyyy-MM-dd no fuso local (toISOString usaria UTC e poderia trocar o dia à noite).
const dataIso = (data) =>
  `${data.getFullYear()}-${doisDigitos(data.getMonth() + 1)}-${doisDigitos(data.getDate())}`;

const contador = (n, singular, plural) => `${n} ${n === 1 ? singular : plural}`;

// Busca na própria tela: sem diferenciar maiúsculas nem acentos ("atelie" acha "Ateliê").
// NFD separa a letra do acento, e \p{M} remove os acentos (marcas combinantes).
const normalizar = (texto) => texto.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase();

// A busca vale para o nome do cliente e para o número do pedido.
// Número aceito como "6", "#P-0006" ou "p-0006" (compara com o id). Outro texto procura no nome
// e no número formatado ("#p-0006").
function combinaComBusca(pedido, termo) {
  const busca = normalizar(termo.trim());
  if (!busca) return true;
  const cliente = normalizar(pedido.cliente);
  const soNumero = busca.match(/^#?p?-?(\d+)$/);
  if (soNumero) {
    return Number(soNumero[1]) === pedido.id || cliente.includes(busca);
  }
  return cliente.includes(busca) || normalizar(numeroPedido(pedido.id)).includes(busca);
}

function periodoInicial() {
  const hoje = new Date();
  return {
    inicio: dataIso(new Date(hoje.getFullYear(), hoje.getMonth(), 1)),
    fim: dataIso(hoje),
  };
}

// Monta "?inicio=...&fim=...&status=..." só com os filtros preenchidos.
function montarFiltros({ inicio, fim, status }) {
  const params = new URLSearchParams();
  if (inicio) params.set('inicio', inicio);
  if (fim) params.set('fim', fim);
  if (status) params.set('status', status);
  const texto = params.toString();
  return texto ? `?${texto}` : '';
}

const mensagemDeErro = (erro) =>
  erro.status ? erro.message : 'Não foi possível falar com o servidor. Verifique a conexão e tente de novo.';

export default function HistoricoVendas() {
  const [inicio, setInicio] = useState(() => periodoInicial().inicio);
  const [fim, setFim] = useState(() => periodoInicial().fim);
  const [status, setStatus] = useState('');
  // Busca por cliente ou número do pedido: filtra só a lista que já veio da API (não chama a API de novo).
  const [busca, setBusca] = useState('');
  const [tentativa, setTentativa] = useState(0);
  // Resultado da última consulta, com a chave dos filtros a que pertence.
  const [resultado, setResultado] = useState(null);

  // Erro de validação aparece abaixo do campo; com o período inválido, nenhuma consulta é feita.
  const erroPeriodo = inicio && fim && inicio > fim ? 'A data inicial não pode ser depois da data final' : '';
  const filtros = montarFiltros({ inicio, fim, status });
  const chave = `${filtros}#${tentativa}`;

  useEffect(() => {
    if (erroPeriodo) return undefined;
    let ativo = true;
    Promise.all([chamarApi(`/pedidos${filtros}`), chamarApi(`/pedidos/total${filtros}`)])
      .then(([pedidos, total]) => {
        if (ativo) setResultado({ chave, pedidos, total });
      })
      .catch((erro) => {
        if (ativo) setResultado({ chave, erro: mensagemDeErro(erro) });
      });
    // Uma resposta antiga não pode sobrescrever a de filtros mais novos.
    return () => {
      ativo = false;
    };
  }, [filtros, chave, erroPeriodo]);

  const atual = !erroPeriodo && resultado?.chave === chave ? resultado : null;
  const carregando = !erroPeriodo && !atual;
  const pedidosDoPeriodo = atual?.pedidos ?? [];
  const pedidos = pedidosDoPeriodo.filter((pedido) => combinaComBusca(pedido, busca));

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <h1 style={styles.title}>Histórico de Vendas</h1>
        <p style={styles.subtitle}>Consulte os pedidos por período e status.</p>
      </div>

      <div style={styles.toolbar}>
        <div style={{ ...styles.campo, ...styles.campoBusca }}>
          <label htmlFor="hist-busca" style={styles.label}>Buscar</label>
          <div style={styles.searchBox}>
            <Search size={16} color="#9CA3AF" style={styles.searchIcon} />
            <input
              id="hist-busca"
              type="search"
              value={busca}
              onChange={(e) => setBusca(e.target.value)}
              placeholder="Buscar por cliente ou N do pedido..."
              style={styles.searchInput}
            />
          </div>
        </div>

        <div style={styles.campo}>
          <label htmlFor="hist-inicio" style={styles.label}>Data inicial</label>
          <input
            id="hist-inicio"
            type="date"
            value={inicio}
            max={fim || undefined}
            onChange={(e) => setInicio(e.target.value)}
            style={{ ...styles.input, ...(erroPeriodo ? styles.inputErro : {}) }}
            aria-invalid={erroPeriodo ? 'true' : 'false'}
            aria-describedby={erroPeriodo ? 'hist-erro-periodo' : undefined}
          />
          {erroPeriodo && (
            <span id="hist-erro-periodo" role="alert" style={styles.erroCampo}>{erroPeriodo}</span>
          )}
        </div>

        <div style={styles.campo}>
          <label htmlFor="hist-fim" style={styles.label}>Data final</label>
          <input
            id="hist-fim"
            type="date"
            value={fim}
            min={inicio || undefined}
            onChange={(e) => setFim(e.target.value)}
            style={{ ...styles.input, ...(erroPeriodo ? styles.inputErro : {}) }}
            aria-invalid={erroPeriodo ? 'true' : 'false'}
          />
        </div>

        <div style={styles.campo}>
          <label htmlFor="hist-status" style={styles.label}>Status</label>
          <select
            id="hist-status"
            value={status}
            onChange={(e) => setStatus(e.target.value)}
            style={styles.input}
          >
            <option value="">Todos</option>
            {Object.entries(STATUS).map(([valor, texto]) => (
              <option key={valor} value={valor}>{texto}</option>
            ))}
          </select>
        </div>
      </div>

      <div style={styles.resumo}>
        <div style={styles.resumoItem}>
          <span style={styles.resumoRotulo}>Total do período</span>
          <strong style={styles.resumoValor}>
            {atual?.total ? dinheiro(atual.total.total) : '—'}
          </strong>
          <span style={styles.resumoNota}>
            {atual?.total
              ? `${contador(atual.total.quantidadePedidos, 'pedido', 'pedidos')} no total · pedidos cancelados não entram`
              : 'Pedidos cancelados não entram no total'}
          </span>
          {/* O total vem da API, para o período e o status; a busca por cliente não o altera. */}
          <span style={styles.resumoNota}>total do período, sem a busca</span>
        </div>
        <div style={styles.resumoItem}>
          <span style={styles.resumoRotulo}>Na lista</span>
          <strong style={styles.resumoValor}>{atual?.pedidos ? contador(pedidos.length, 'pedido', 'pedidos') : '—'}</strong>
        </div>
      </div>

      {atual?.erro && (
        <div role="alert" style={styles.erroBox}>
          <span>{atual.erro}</span>
          <button type="button" onClick={() => setTentativa((n) => n + 1)} style={styles.btnTentar}>
            <RefreshCw size={14} /> Tentar de novo
          </button>
        </div>
      )}

      <div style={styles.tableCard}>
        <div style={styles.tableScroll}>
          <table style={styles.table}>
            <thead>
              <tr>
                <th style={styles.th}>PEDIDO</th>
                <th style={styles.th}>CLIENTE</th>
                <th style={styles.th}>DATA</th>
                <th style={styles.th}>ITENS</th>
                <th style={styles.th}>PAGAMENTO</th>
                <th style={{ ...styles.th, textAlign: 'right' }}>TOTAL</th>
                <th style={styles.th}>STATUS</th>
              </tr>
            </thead>
            <tbody>
              {pedidos.map((pedido) => (
                <tr key={pedido.id} style={styles.tr}>
                  <td style={styles.tdBold}>{numeroPedido(pedido.id)}</td>
                  <td style={styles.td}>{pedido.cliente}</td>
                  <td style={styles.td}>
                    <div style={styles.dataCelula}>
                      <Calendar size={14} color="#6B7280" />
                      {dataBr(pedido.dataPedido)}
                    </div>
                  </td>
                  <td style={styles.td}>
                    {pedido.itens.map((item) => (
                      <div key={item.id}>
                        {item.tecido} {item.corCodigo} — {metros(item.metragem)}
                      </div>
                    ))}
                  </td>
                  <td style={styles.td}>{FORMAS_PAGAMENTO[pedido.formaPagamento] ?? pedido.formaPagamento}</td>
                  <td style={{ ...styles.tdBold, textAlign: 'right' }}>{dinheiro(pedido.total)}</td>
                  <td style={styles.td}>
                    <span style={styles.badge[pedido.status] ?? styles.badge.EM_ABERTO}>
                      {STATUS[pedido.status] ?? pedido.status}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {carregando && <div style={styles.emptyState}><p>Carregando pedidos…</p></div>}

        {atual?.pedidos && pedidosDoPeriodo.length === 0 && (
          <div style={styles.emptyState}>
            <FileText size={32} color="#9CA3AF" />
            <p>Nenhum pedido encontrado para o período e o status selecionados.</p>
          </div>
        )}

        {/* Há pedidos no período, mas nenhum combina com a busca. */}
        {pedidosDoPeriodo.length > 0 && pedidos.length === 0 && (
          <div style={styles.emptyState}>
            <FileText size={32} color="#9CA3AF" />
            <p>Nenhum pedido encontrado para a busca.</p>
          </div>
        )}
      </div>
    </div>
  );
}

// Estilos in-line mantendo o padrão visual das telas anteriores
const badgeBase = { padding: '0.25rem 0.6rem', borderRadius: '9999px', fontSize: '0.75rem', fontWeight: '600', whiteSpace: 'nowrap' };

const styles = {
  container: { fontFamily: 'sans-serif', paddingBottom: '3rem' },
  header: { marginBottom: '1.5rem' },
  title: { fontSize: '1.5rem', fontWeight: '600', color: '#111827', margin: '0 0 0.25rem 0' },
  subtitle: { fontSize: '0.875rem', color: '#6B7280', margin: 0 },
  toolbar: { display: 'flex', gap: '1rem', marginBottom: '1.5rem', flexWrap: 'wrap', alignItems: 'flex-start' },
  campo: { display: 'flex', flexDirection: 'column', gap: '0.25rem', minWidth: '180px' },
  label: { fontSize: '0.75rem', fontWeight: '600', color: '#374151' },
  input: { padding: '0.6rem 0.75rem', border: '1px solid #D1D5DB', borderRadius: '6px', fontSize: '0.875rem', outline: 'none', backgroundColor: 'white', boxSizing: 'border-box' },
  inputErro: { borderColor: '#DC2626' },
  campoBusca: { flex: 1, minWidth: '280px' },
  searchBox: { position: 'relative' },
  searchIcon: { position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', pointerEvents: 'none' },
  searchInput: { width: '100%', padding: '0.6rem 1rem 0.6rem 2.5rem', border: '1px solid #D1D5DB', borderRadius: '6px', fontSize: '0.875rem', outline: 'none', backgroundColor: 'white', boxSizing: 'border-box' },
  erroCampo: { fontSize: '0.75rem', color: '#DC2626', maxWidth: '260px' },
  resumo: { display: 'flex', gap: '1rem', marginBottom: '1.5rem', flexWrap: 'wrap' },
  resumoItem: { display: 'flex', flexDirection: 'column', gap: '0.25rem', backgroundColor: 'white', border: '1px solid #E5E7EB', borderRadius: '8px', padding: '1rem 1.25rem', minWidth: '220px', boxShadow: '0 1px 2px rgba(0,0,0,0.05)' },
  resumoRotulo: { fontSize: '0.75rem', fontWeight: '600', color: '#6B7280' },
  resumoValor: { fontSize: '1.5rem', color: '#111827' },
  resumoNota: { fontSize: '0.75rem', color: '#6B7280' },
  erroBox: { display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '1rem', backgroundColor: '#FEF2F2', border: '1px solid #FECACA', color: '#B91C1C', borderRadius: '8px', padding: '0.75rem 1rem', marginBottom: '1rem', fontSize: '0.875rem' },
  btnTentar: { display: 'inline-flex', alignItems: 'center', gap: '0.25rem', backgroundColor: 'white', color: '#B91C1C', border: '1px solid #FECACA', padding: '0.35rem 0.65rem', borderRadius: '6px', fontSize: '0.75rem', fontWeight: '600', cursor: 'pointer' },
  tableCard: { backgroundColor: 'white', border: '1px solid #E5E7EB', borderRadius: '8px', boxShadow: '0 1px 2px rgba(0,0,0,0.05)' },
  // Em telas menores a tabela rola na horizontal (AGENTS 7.2).
  tableScroll: { overflowX: 'auto' },
  table: { width: '100%', borderCollapse: 'collapse', textAlign: 'left', minWidth: '860px' },
  th: { padding: '0.875rem 1rem', borderBottom: '1px solid #E5E7EB', backgroundColor: '#F9FAFB', fontSize: '0.75rem', fontWeight: '600', color: '#6B7280', letterSpacing: '0.05em' },
  tr: { borderBottom: '1px solid #E5E7EB' },
  td: { padding: '1rem', fontSize: '0.875rem', color: '#374151', verticalAlign: 'top' },
  tdBold: { padding: '1rem', fontSize: '0.875rem', color: '#111827', fontWeight: '600', verticalAlign: 'top' },
  dataCelula: { display: 'flex', alignItems: 'center', gap: '0.4rem', whiteSpace: 'nowrap' },

  // Badges de status
  badge: {
    EM_ABERTO: { ...badgeBase, backgroundColor: '#FEF3C7', color: '#B45309' },
    PAGO: { ...badgeBase, backgroundColor: '#D1FAE5', color: '#065F46' },
    CANCELADO: { ...badgeBase, backgroundColor: '#FEE2E2', color: '#B91C1C' },
  },

  emptyState: { display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '3rem', color: '#6B7280', gap: '0.5rem' },
};
