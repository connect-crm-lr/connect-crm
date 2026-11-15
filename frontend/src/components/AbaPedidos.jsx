import { useState } from 'react';
import { pagarPedido, cancelarPedido } from '../services/pedidos';
import { formatarData, formatarDinheiro, formatarMetragem, formatarNumeroPedido } from '../utils/formatadores';

const STATUS = {
  EM_ABERTO: { texto: 'Em aberto', cor: '#B45309', fundo: '#FEF3C7' },
  PAGO: { texto: 'Pago', cor: '#047857', fundo: '#D1FAE5' },
  CANCELADO: { texto: 'Cancelado', cor: '#6B7280', fundo: '#F3F4F6' },
};
const FORMAS = { A_VISTA: 'À vista', CARTAO: 'Cartão', BOLETO: 'Boleto', CHEQUE: 'Cheque' };

// Aba Pedidos da ficha: histórico do cliente com Marcar como pago e Cancelar pedido (AGENTS 7.3).
// "pedidos" vem de GET /pedidos?clienteId, carregado pela ficha; "onAlterado" recarrega depois de pagar/cancelar.
export default function AbaPedidos({ pedidos, carregando, erroCarga, onAlterado }) {
  const [processandoId, setProcessandoId] = useState(null);
  const [erro, setErro] = useState('');

  const executar = async (pedido, acao) => {
    if (processandoId) return; // evita duplo clique
    setProcessandoId(pedido.id);
    setErro('');
    try {
      await acao(pedido.id);
      await onAlterado();
    } catch (e) {
      setErro(e.message);
    } finally {
      setProcessandoId(null);
    }
  };

  const cancelar = (pedido) => {
    if (!window.confirm(`Cancelar o pedido ${formatarNumeroPedido(pedido.id)}? A metragem volta ao estoque e o cancelamento é definitivo.`)) {
      return;
    }
    executar(pedido, cancelarPedido);
  };

  // Mais recentes primeiro.
  const ordenados = [...pedidos].sort((a, b) => b.dataPedido.localeCompare(a.dataPedido) || b.id - a.id);

  return (
    <div>
      {(erro || erroCarga) && <p role="alert" style={styles.erro}>{erro || erroCarga}</p>}
      {carregando && <p style={styles.placeholder}>Carregando pedidos...</p>}
      {!carregando && !erroCarga && ordenados.length === 0 && <p style={styles.placeholder}>Nenhum pedido registrado.</p>}

      <div style={styles.lista}>
        {ordenados.map((p) => {
          const st = STATUS[p.status] ?? { texto: p.status, cor: '#374151', fundo: '#F3F4F6' };
          const ocupado = processandoId === p.id;
          return (
            <div key={p.id} style={{ ...styles.item, opacity: p.status === 'CANCELADO' ? 0.7 : 1 }}>
              <div style={styles.topo}>
                <span style={styles.numero}>{formatarNumeroPedido(p.id)}</span>
                <span style={{ ...styles.badge, color: st.cor, backgroundColor: st.fundo }}>{st.texto}</span>
              </div>
              <p style={styles.meta}>
                {formatarData(p.dataPedido)} · {FORMAS[p.formaPagamento] ?? p.formaPagamento} · {formatarDinheiro(p.total)}
              </p>
              <ul style={styles.itens}>
                {p.itens.map((i) => (
                  <li key={i.id}>
                    {i.tecido} ({i.corCodigo}) — {formatarMetragem(i.metragem)} × {formatarDinheiro(i.precoMetro)}
                  </li>
                ))}
              </ul>
              {p.status === 'EM_ABERTO' && (
                <div style={styles.acoes}>
                  <button style={styles.btnPagar} disabled={ocupado} onClick={() => executar(p, pagarPedido)}>
                    {ocupado ? 'Aguarde...' : 'Marcar como pago'}
                  </button>
                  <button style={styles.btnCancelar} disabled={ocupado} onClick={() => cancelar(p)}>Cancelar pedido</button>
                </div>
              )}
              {p.status === 'PAGO' && (
                <div style={styles.acoes}>
                  <button style={styles.btnCancelar} disabled={ocupado} onClick={() => cancelar(p)}>Cancelar pedido</button>
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}

const styles = {
  placeholder: { fontSize: '0.875rem', color: '#9CA3AF', fontStyle: 'italic' },
  erro: { color: '#DC2626', fontSize: '0.8rem', margin: '0 0 1rem 0' },
  lista: { display: 'flex', flexDirection: 'column', gap: '0.75rem' },
  item: { border: '1px solid #E5E7EB', borderRadius: '6px', padding: '0.75rem' },
  topo: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.25rem' },
  numero: { fontSize: '0.875rem', fontWeight: '600', color: '#111827' },
  badge: { padding: '0.1rem 0.5rem', borderRadius: '9999px', fontSize: '0.7rem', fontWeight: '600' },
  meta: { fontSize: '0.8rem', color: '#6B7280', margin: '0 0 0.5rem 0' },
  itens: { fontSize: '0.8rem', color: '#374151', margin: 0, paddingLeft: '1.1rem' },
  acoes: { display: 'flex', gap: '0.5rem', marginTop: '0.75rem', flexWrap: 'wrap' },
  btnPagar: { backgroundColor: '#10B981', color: 'white', border: 'none', padding: '0.35rem 0.75rem', borderRadius: '6px', fontSize: '0.75rem', cursor: 'pointer' },
  btnCancelar: { backgroundColor: 'white', color: '#DC2626', border: '1px solid #DC2626', padding: '0.35rem 0.75rem', borderRadius: '6px', fontSize: '0.75rem', cursor: 'pointer' },
};
