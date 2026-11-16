import { useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { Plus, Trash2, CheckCircle } from 'lucide-react';
import { listarClientes } from '../services/clientes';
import {
  criarPedido,
  listarEstoqueParaPedido,
  listarTecidosParaPedido,
} from '../services/pedidos';
import { formatarDinheiro, formatarMetragem, formatarNumeroPedido } from '../utils/formatadores';

const FORMAS = [
  { valor: 'A_VISTA', texto: 'À vista' },
  { valor: 'CARTAO', texto: 'Cartão' },
  { valor: 'BOLETO', texto: 'Boleto' },
  { valor: 'CHEQUE', texto: 'Cheque' },
];

const arredondar = (n) => Math.round(n * 100) / 100;

export default function NovoPedido() {
  const location = useLocation();

  // Vindo de uma negociação FECHADA (Funil): o cliente já vem definido e o pedido fica ligado à negociação.
  const idClienteDoFunil = location.state?.idCliente ? String(location.state.idCliente) : '';
  const nomeClienteDoFunil = location.state?.clientePreSelecionado || '';
  const idNegociacao = location.state?.idNegociacao ?? null;

  const [clientes, setClientes] = useState([]);
  const [estoque, setEstoque] = useState([]);
  const [precos, setPrecos] = useState({}); // idTecido -> precoMetro
  const [carregando, setCarregando] = useState(true);
  const [erroCarga, setErroCarga] = useState('');

  const [idCliente, setIdCliente] = useState(idClienteDoFunil);
  const [formaPagamento, setFormaPagamento] = useState('A_VISTA');
  const [pagoNoAto, setPagoNoAto] = useState(false);
  const [itens, setItens] = useState([]); // { idEstoque, metragem }
  const [linhaSel, setLinhaSel] = useState('');
  const [metragemSel, setMetragemSel] = useState('');

  const [erros, setErros] = useState({});
  const [erroGeral, setErroGeral] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [sucesso, setSucesso] = useState('');

  useEffect(() => {
    let cancelado = false;
    Promise.all([listarClientes(), listarEstoqueParaPedido(), listarTecidosParaPedido()])
      .then(([listaClientes, linhas, tecidos]) => {
        if (cancelado) return;
        setClientes(listaClientes);
        setEstoque(linhas);
        setPrecos(Object.fromEntries(tecidos.map((t) => [t.id, t.precoMetro])));
        // Compatibilidade com o Funil, que ainda manda só o nome do cliente.
        if (!idClienteDoFunil && nomeClienteDoFunil) {
          const achado = listaClientes.find((c) => c.nome === nomeClienteDoFunil);
          if (achado) setIdCliente(String(achado.id));
        }
      })
      .catch((e) => { if (!cancelado) setErroCarga(e.message); })
      .finally(() => { if (!cancelado) setCarregando(false); });
    return () => { cancelado = true; };
  }, [idClienteDoFunil, nomeClienteDoFunil]);

  const linhaPorId = (id) => estoque.find((l) => l.id === id);
  const precoDaLinha = (linha) => Number(precos[linha?.idTecido] ?? 0);

  const limparErro = (campo) => setErros((atual) => ({ ...atual, [campo]: undefined }));

  // Cada tecido + cor aparece uma vez por pedido: se já está na lista, soma a metragem na linha existente (regra 6.2).
  const adicionarItem = () => {
    const novosErros = {};
    if (!linhaSel) novosErros.linha = 'Selecione o tecido e a cor';
    if (metragemSel === '' || Number(metragemSel) <= 0) novosErros.metragemSel = 'A metragem deve ser maior que zero';
    if (Object.keys(novosErros).length > 0) {
      setErros((atual) => ({ ...atual, ...novosErros }));
      return;
    }
    const id = Number(linhaSel);
    const metragem = Number(metragemSel);
    setItens((atual) => (atual.some((i) => i.idEstoque === id)
      ? atual.map((i) => (i.idEstoque === id ? { ...i, metragem: arredondar(i.metragem + metragem) } : i))
      : [...atual, { idEstoque: id, metragem }]));
    setLinhaSel('');
    setMetragemSel('');
    setErros((atual) => ({ ...atual, linha: undefined, metragemSel: undefined, itens: undefined }));
    setErroGeral(''); // a lista mudou: o erro da última tentativa (ex.: estoque insuficiente) já não vale
  };

  // Corrigir a metragem tira o erro daquela linha e o erro geral da última tentativa
  // (ex.: "Estoque insuficiente"), para a tela não parecer bloqueada depois da correção.
  const alterarMetragem = (idEstoque, valor) => {
    const indice = itens.findIndex((i) => i.idEstoque === idEstoque);
    setItens(itens.map((i) => (i.idEstoque === idEstoque ? { ...i, metragem: valor } : i)));
    setErros((atual) => ({ ...atual, itens: undefined, [`item${indice}`]: undefined }));
    setErroGeral('');
  };

  const removerItem = (idEstoque) => {
    setItens(itens.filter((i) => i.idEstoque !== idEstoque));
    setErroGeral('');
  };

  // Prévia do total: metragem × preço do tecido. O valor oficial é calculado e gravado pelo back ao confirmar.
  const valorTotal = itens.reduce(
    (soma, i) => soma + Number(i.metragem || 0) * precoDaLinha(linhaPorId(i.idEstoque)),
    0,
  );

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErroGeral('');
    setSucesso('');

    const novosErros = {};
    if (!idCliente) novosErros.cliente = 'Selecione o cliente';
    if (itens.length === 0) novosErros.itens = 'O pedido precisa de pelo menos um item';
    itens.forEach((i, idx) => {
      if (!(Number(i.metragem) > 0)) novosErros[`item${idx}`] = 'A metragem deve ser maior que zero';
    });
    if (Object.keys(novosErros).length > 0) {
      setErros(novosErros);
      return;
    }

    setEnviando(true);
    try {
      const pedido = await criarPedido({
        idCliente: Number(idCliente),
        idNegociacao,
        formaPagamento,
        pagoNoAto,
        itens: itens.map((i) => ({ idEstoque: i.idEstoque, metragem: Number(i.metragem) })),
      });
      setSucesso(`Pedido ${formatarNumeroPedido(pedido.id)} criado: ${formatarDinheiro(pedido.total)} — ${pedido.status === 'PAGO' ? 'Pago' : 'Em aberto'}.`);
      setItens([]);
      setPagoNoAto(false);
      setErros({});
      await atualizarEstoque();
    } catch (err) {
      // "campos" da API: cada erro de validação vai abaixo do campo correspondente.
      const campos = err.detalhes?.campos ?? [];
      if (campos.length > 0) {
        const mapa = {};
        const semLugar = []; // campo que não existe na tela (ex.: idNegociacao): não pode falhar em silêncio
        campos.forEach((c) => {
          const item = /^itens\[(\d+)\]/.exec(c.campo);
          if (item) mapa[`item${item[1]}`] = c.mensagem;
          else if (c.campo === 'idCliente') mapa.cliente = c.mensagem;
          else if (c.campo === 'itens' || c.campo === 'formaPagamento') mapa[c.campo] = c.mensagem;
          else semLugar.push(c.mensagem);
        });
        setErros(mapa);
        if (semLugar.length > 0) setErroGeral(semLugar.join(' '));
      } else {
        // Ex.: "Estoque insuficiente: Linho Bege 102 tem 18 m" — mensagem do back.
        setErroGeral(err.message);
      }
    } finally {
      setEnviando(false);
    }
  };

  // O saldo mudou: recarrega as linhas de estoque. Nunca lança: se este GET falhar, o pedido já foi criado
  // e não deve aparecer erro; a lista só fica desatualizada até a próxima abertura da tela.
  const atualizarEstoque = async () => {
    try {
      setEstoque(await listarEstoqueParaPedido());
    } catch {
      // ignorado de propósito
    }
  };

  const campoErro = (nome) => erros[nome] && <p role="alert" style={styles.fieldError}>{erros[nome]}</p>;

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <h1 style={styles.title}>Novo Pedido</h1>
        <p style={styles.subtitle}>Preencha os dados para registrar uma nova venda</p>
      </div>

      {erroCarga && <div role="alert" style={styles.alertError}>{erroCarga}</div>}
      {carregando && <p style={styles.subtitle}>Carregando dados...</p>}
      {sucesso && <div role="status" style={styles.alertOk}>{sucesso}</div>}

      <form onSubmit={handleSubmit} noValidate style={styles.form}>

        {/* Cliente */}
        <div style={styles.card}>
          <h2 style={styles.cardTitle}>1. CLIENTE</h2>
          <div style={styles.inputGroup}>
            <label style={styles.label}>SELECIONE O CLIENTE *</label>
            <select
              value={idCliente}
              onChange={(e) => { setIdCliente(e.target.value); limparErro('cliente'); }}
              disabled={Boolean(idNegociacao)}
              style={styles.input}
            >
              <option value="">-- Escolha um cliente --</option>
              {clientes.map((c) => (
                <option key={c.id} value={c.id}>{c.nome}</option>
              ))}
            </select>
            {idNegociacao && <p style={styles.hint}>Pedido da negociação #{idNegociacao}: o cliente é o da negociação.</p>}
            {campoErro('cliente')}
            {campoErro('idCliente')}
          </div>
        </div>

        {/* Itens */}
        <div style={styles.card}>
          <h2 style={styles.cardTitle}>2. ITENS DO PEDIDO (TECIDOS)</h2>

          <div style={styles.addRow}>
            <div style={{ flex: 3 }}>
              <label style={styles.subLabel}>Tecido e cor</label>
              <select
                value={linhaSel}
                onChange={(e) => { setLinhaSel(e.target.value); limparErro('linha'); }}
                style={styles.input}
              >
                <option value="">Selecione o tecido e a cor</option>
                {estoque.map((l) => (
                  <option key={l.id} value={l.id}>
                    {l.tecido} — {l.corCodigo} ({formatarMetragem(l.metragem)} em estoque)
                  </option>
                ))}
              </select>
              {campoErro('linha')}
            </div>
            <div style={{ flex: 1 }}>
              <label style={styles.subLabel}>Metragem (m)</label>
              <input
                type="number" step="0.01" placeholder="0,00" value={metragemSel}
                onChange={(e) => { setMetragemSel(e.target.value); limparErro('metragemSel'); }}
                style={styles.input}
              />
              {campoErro('metragemSel')}
            </div>
            <button type="button" onClick={adicionarItem} style={styles.btnAddItem}>
              <Plus size={16} /> Adicionar Tecido
            </button>
          </div>

          <div style={styles.itemsList}>
            {itens.length === 0 && <p style={styles.hint}>Nenhum tecido adicionado.</p>}
            {itens.map((item, idx) => {
              const linha = linhaPorId(item.idEstoque);
              const preco = precoDaLinha(linha);
              const subtotal = Number(item.metragem || 0) * preco;
              return (
                <div key={item.idEstoque}>
                  <div style={styles.itemRow}>
                    <div style={{ flex: 3 }}>
                      <span style={styles.itemNome}>{linha?.tecido}</span>
                      <span style={styles.itemCor}> — {linha?.corCodigo}</span>
                    </div>
                    <div style={{ flex: 1 }}>
                      <label style={styles.subLabel}>Metragem (m)</label>
                      <input
                        type="number" step="0.01" value={item.metragem}
                        onChange={(e) => alterarMetragem(item.idEstoque, e.target.value)}
                        style={styles.input}
                      />
                    </div>
                    <div style={{ flex: 1 }}>
                      <span style={styles.subLabel}>Preço/m</span>
                      <span style={styles.itemPreco}>{formatarDinheiro(preco)}</span>
                    </div>
                    <div style={{ flex: 1 }}>
                      <span style={styles.subLabel}>Subtotal</span>
                      <span style={styles.subtotalText}>{formatarDinheiro(subtotal)}</span>
                    </div>
                    <button
                      type="button" onClick={() => removerItem(item.idEstoque)}
                      style={styles.btnRemove} aria-label={`Remover ${linha?.tecido ?? 'item'}`}
                    >
                      <Trash2 size={18} color="#DC2626" />
                    </button>
                  </div>
                  {campoErro(`item${idx}`)}
                </div>
              );
            })}
          </div>
          {campoErro('itens')}
        </div>

        {/* Pagamento e Total */}
        <div style={styles.card}>
          <h2 style={styles.cardTitle}>3. PAGAMENTO E TOTAL</h2>

          <div style={styles.paymentRow}>
            <div style={{ flex: 1, ...styles.inputGroup }}>
              <label style={styles.label}>FORMA DE PAGAMENTO *</label>
              <select
                value={formaPagamento}
                onChange={(e) => { setFormaPagamento(e.target.value); limparErro('formaPagamento'); }}
                style={styles.input}
              >
                {FORMAS.map((f) => <option key={f.valor} value={f.valor}>{f.texto}</option>)}
              </select>
              {campoErro('formaPagamento')}
              <label style={styles.check}>
                <input type="checkbox" checked={pagoNoAto} onChange={(e) => setPagoNoAto(e.target.checked)} />
                Pago no ato
              </label>
            </div>

            <div style={styles.totalBox}>
              <span style={styles.totalLabel}>TOTAL DO PEDIDO:</span>
              <span style={styles.totalValue}>{formatarDinheiro(valorTotal)}</span>
            </div>
          </div>
        </div>

        {erroGeral && <div role="alert" style={styles.alertError}>{erroGeral}</div>}

        <div style={styles.footerActions}>
          <button type="submit" disabled={enviando || carregando} style={styles.btnSubmit}>
            <CheckCircle size={18} /> {enviando ? 'Criando...' : 'Finalizar e Criar Pedido'}
          </button>
        </div>

      </form>
    </div>
  );
}

const styles = {
  container: { fontFamily: 'sans-serif', paddingBottom: '3rem' },
  header: { marginBottom: '1.5rem' },
  title: { fontSize: '1.5rem', fontWeight: '600', color: '#111827', margin: '0 0 0.25rem 0' },
  subtitle: { fontSize: '0.875rem', color: '#6B7280', margin: 0 },
  form: { display: 'flex', flexDirection: 'column', gap: '1.5rem' },
  card: { backgroundColor: 'white', border: '1px solid #E5E7EB', borderRadius: '8px', padding: '1.5rem', boxShadow: '0 1px 2px rgba(0,0,0,0.05)' },
  cardTitle: { fontSize: '0.75rem', fontWeight: '600', color: '#9CA3AF', letterSpacing: '0.05em', marginBottom: '1rem', textTransform: 'uppercase' },
  inputGroup: { display: 'flex', flexDirection: 'column', gap: '0.5rem' },
  label: { fontSize: '0.75rem', fontWeight: '600', color: '#6B7280', letterSpacing: '0.05em' },
  subLabel: { fontSize: '0.7rem', fontWeight: '500', color: '#6B7280', marginBottom: '0.25rem', display: 'block' },
  input: { padding: '0.75rem', border: '1px solid #D1D5DB', borderRadius: '6px', fontSize: '0.875rem', outline: 'none', width: '100%', boxSizing: 'border-box', backgroundColor: 'white' },
  addRow: { display: 'flex', gap: '1rem', alignItems: 'flex-start', marginBottom: '1rem', flexWrap: 'wrap' },
  btnAddItem: { backgroundColor: '#EEF2FF', color: '#4F46E5', border: 'none', padding: '0.75rem 0.9rem', borderRadius: '6px', fontSize: '0.8rem', fontWeight: '600', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '0.35rem', marginTop: '1.1rem', whiteSpace: 'nowrap' },
  itemsList: { display: 'flex', flexDirection: 'column', gap: '0.75rem' },
  itemRow: { display: 'flex', gap: '1rem', alignItems: 'center', backgroundColor: '#F9FAFB', padding: '1rem', borderRadius: '6px', border: '1px solid #E5E7EB' },
  itemNome: { fontSize: '0.875rem', fontWeight: '600', color: '#111827' },
  itemCor: { fontSize: '0.875rem', color: '#6B7280' },
  itemPreco: { fontSize: '0.875rem', color: '#374151' },
  subtotalText: { fontSize: '0.875rem', fontWeight: '600', color: '#111827' },
  btnRemove: { background: 'none', border: 'none', cursor: 'pointer', padding: '0.5rem' },
  paymentRow: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '2rem', flexWrap: 'wrap' },
  check: { display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.875rem', color: '#374151' },
  totalBox: { display: 'flex', flexDirection: 'column', alignItems: 'flex-end', backgroundColor: '#EEF2FF', padding: '1rem 1.5rem', borderRadius: '8px' },
  totalLabel: { fontSize: '0.75rem', fontWeight: '600', color: '#4F46E5' },
  totalValue: { fontSize: '1.5rem', fontWeight: '700', color: '#312E81' },
  footerActions: { display: 'flex', justifyContent: 'flex-end' },
  btnSubmit: { display: 'flex', alignItems: 'center', gap: '0.5rem', backgroundColor: '#10B981', color: 'white', border: 'none', padding: '0.75rem 1.5rem', borderRadius: '6px', fontSize: '1rem', fontWeight: '600', cursor: 'pointer' },
  hint: { fontSize: '0.8rem', color: '#6B7280', margin: 0 },
  fieldError: { color: '#DC2626', fontSize: '0.75rem', margin: '0.25rem 0 0 0' },
  alertError: { backgroundColor: '#FEF2F2', border: '1px solid #FECACA', color: '#B91C1C', padding: '0.75rem 1rem', borderRadius: '6px', fontSize: '0.875rem', marginBottom: '1rem' },
  alertOk: { backgroundColor: '#ECFDF5', border: '1px solid #A7F3D0', color: '#047857', padding: '0.75rem 1rem', borderRadius: '6px', fontSize: '0.875rem', marginBottom: '1rem' },
};
