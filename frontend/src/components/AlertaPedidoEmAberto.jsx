// Banner "Este cliente tem pedido em aberto" da ficha do cliente.
// Fica fora da tela até a API de listagem de pedidos entrar (#71): enquanto "resumo" for null, não aparece.
// Para ligar: passar { quantidade, valorTotal, numeroCompras, dataUltimaCompra } calculado a partir de GET /pedidos.
export default function AlertaPedidoEmAberto({ resumo }) {
  if (!resumo) return null;

  const valor = Number(resumo.valorTotal).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  return (
    <div style={styles.banner}>
      Este cliente tem pedido em aberto — {valor} - já comprou {resumo.numeroCompras}x, a última em {resumo.dataUltimaCompra}
    </div>
  );
}

const styles = {
  banner: {
    backgroundColor: '#FEF3C7', color: '#B45309', padding: '1rem', borderRadius: '6px',
    fontSize: '0.875rem', marginBottom: '1.5rem', fontWeight: '500',
  },
};
