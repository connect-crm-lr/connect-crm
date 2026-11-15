import { formatarData, formatarDinheiro } from '../utils/formatadores';

// Banner "Este cliente tem pedido em aberto" da ficha do cliente.
// "resumo" = { quantidade, valorTotal, numeroCompras, dataUltimaCompra }, calculado a partir de GET /pedidos.
// Sem pedido em aberto (resumo null), não aparece.
export default function AlertaPedidoEmAberto({ resumo }) {
  if (!resumo) return null;

  const historico = resumo.numeroCompras > 0
    ? ` - já comprou ${resumo.numeroCompras}x, a última em ${formatarData(resumo.dataUltimaCompra)}`
    : '';
  return (
    <div style={styles.banner}>
      Este cliente tem {resumo.quantidade} {resumo.quantidade === 1 ? 'pedido' : 'pedidos'} em aberto — {formatarDinheiro(resumo.valorTotal)}{historico}
    </div>
  );
}

const styles = {
  banner: {
    backgroundColor: '#FEF3C7', color: '#B45309', padding: '1rem', borderRadius: '6px',
    fontSize: '0.875rem', marginBottom: '1.5rem', fontWeight: '500',
  },
};
