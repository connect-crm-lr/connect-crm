import React, { useState } from 'react';
import { Search, CheckCircle, XCircle, FileText, Calendar } from 'lucide-react';

export default function HistoricoVendas() {
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('TODOS');

  // Mocks de vendas com base no modelo de dados do CRM
  const [pedidos, setPedidos] = useState([
    {
      id: 1001,
      cliente: 'Ateliê Bella Noiva',
      data: '28/10/2026',
      formaPagamento: 'A_VISTA',
      valorTotal: 289.00,
      status: 'EM_ABERTO',
      itens: [
        { tecido: 'Crepe Georgette', cor: 'Off-white', metragem: 10, precoMetro: 28.90 }
      ]
    },
    {
      id: 1002,
      cliente: 'Studio Decor SP',
      data: '25/10/2026',
      formaPagamento: 'BOLETO',
      valorTotal: 1275.00,
      status: 'PAGO',
      itens: [
        { tecido: 'Linho Natural', cor: 'Natural', metragem: 30, precoMetro: 42.50 }
      ]
    },
    {
      id: 1003,
      cliente: 'Casa Têxtil Moreno',
      data: '20/10/2026',
      formaPagamento: 'CARTAO',
      valorTotal: 350.00,
      status: 'CANCELADO',
      itens: [
        { tecido: 'Tule de Festa', cor: 'Marfim', metragem: 10, precoMetro: 35.00 }
      ]
    }
  ]);

  // Regra RN: Marcar como pago (Altera status para PAGO)
  const handleMarcarComoPago = (id) => {
    setPedidos(pedidos.map(p => {
      if (p.id === id) {
        return { ...p, status: 'PAGO' };
      }
      return p;
    }));
    alert(`Pedido #${id} marcado como PAGO com sucesso!`);
  };

  // Regra RN: Cancelar pedido (Devolve metragem ao estoque e altera status para CANCELADO)
  const handleCancelarPedido = (id) => {
    const pedido = pedidos.find(p => p.id === id);
    if (!pedido) return;

    if (window.confirm(`Tem certeza que deseja cancelar o Pedido #${id}?\n\nEsta ação devolverá a metragem dos tecidos ao estoque e alterará o status para CANCELADO.`)) {
      setPedidos(pedidos.map(p => {
        if (p.id === id) {
          return { ...p, status: 'CANCELADO' };
        }
        return p;
      }));
      alert(`Pedido #${id} foi cancelado e a metragem retornou ao estoque.`);
    }
  };

  // Filtragem de pedidos
  const pedidosFiltrados = pedidos.filter(p => {
    const matchSearch = p.cliente.toLowerCase().includes(searchTerm.toLowerCase()) || p.id.toString().includes(searchTerm);
    const matchStatus = statusFilter === 'TODOS' || p.status === statusFilter;
    return matchSearch && matchStatus;
  });

  // Função para formatar exibição das formas de pagamento
  const formatFormaPagamento = (forma) => {
    const mapa = {
      'A_VISTA': 'À Vista',
      'CARTAO': 'Cartão',
      'BOLETO': 'Boleto',
      'CHEQUE': 'Cheque'
    };
    return mapa[forma] || forma;
  };

  // Badge de Status
  const renderStatusBadge = (status) => {
    switch (status) {
      case 'PAGO':
        return <span style={styles.badgePago}>PAGO</span>;
      case 'EM_ABERTO':
        return <span style={styles.badgeEmAberto}>EM ABERTO</span>;
      case 'CANCELADO':
        return <span style={styles.badgeCancelado}>CANCELADO</span>;
      default:
        return null;
    }
  };

  return (
    <div style={styles.container}>
      {/* Cabeçalho */}
      <div style={styles.header}>
        <div>
          <h1 style={styles.title}>Histórico de Vendas</h1>
          <p style={styles.subtitle}>Consulte e gerencie todos os pedidos realizados</p>
        </div>
      </div>

      {/* Toolbar: Filtros e Busca */}
      <div style={styles.toolbar}>
        <div style={styles.searchBox}>
          <Search size={18} color="#9CA3AF" style={styles.searchIcon} />
          <input
            type="text"
            placeholder="Buscar por cliente ou Nº do pedido..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            style={styles.searchInput}
          />
        </div>

        <div style={styles.filterBox}>
          <select 
            value={statusFilter} 
            onChange={(e) => setStatusFilter(e.target.value)}
            style={styles.selectInput}
          >
            <option value="TODOS">Todos os Status</option>
            <option value="EM_ABERTO">Em Aberto</option>
            <option value="PAGO">Pago</option>
            <option value="CANCELADO">Cancelado</option>
          </select>
        </div>
      </div>

      {/* Tabela de Vendas / Pedidos */}
      <div style={styles.tableCard}>
        <table style={styles.table}>
          <thead>
            <tr>
              <th style={styles.th}>PEDIDO</th>
              <th style={styles.th}>CLIENTE</th>
              <th style={styles.th}>DATA</th>
              <th style={styles.th}>PAGAMENTO</th>
              <th style={styles.th}>VALOR TOTAL</th>
              <th style={styles.th}>STATUS</th>
              <th style={{ ...styles.th, textAlign: 'right' }}>AÇÕES</th>
            </tr>
          </thead>
          <tbody>
            {pedidosFiltrados.map((pedido) => (
              <tr key={pedido.id} style={styles.tr}>
                <td style={styles.tdBold}>#{pedido.id}</td>
                <td style={styles.td}>{pedido.cliente}</td>
                <td style={styles.td}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                    <Calendar size={14} color="#6B7280" />
                    {pedido.data}
                  </div>
                </td>
                <td style={styles.td}>{formatFormaPagamento(pedido.formaPagamento)}</td>
                <td style={styles.tdBold}>R$ {pedido.valorTotal.toFixed(2)}</td>
                <td style={styles.td}>{renderStatusBadge(pedido.status)}</td>
                <td style={{ ...styles.td, textAlign: 'right' }}>
                  <div style={styles.actionsContainer}>
                    {/* Botão: Marcar como Pago (Disponível apenas para pedidos EM_ABERTO) */}
                    {pedido.status === 'EM_ABERTO' && (
                      <button 
                        onClick={() => handleMarcarComoPago(pedido.id)}
                        style={styles.btnPago}
                        title="Marcar como Pago"
                      >
                        <CheckCircle size={15} /> Marcar Pago
                      </button>
                    )}

                    {/* Botão: Cancelar Pedido (Disponível para pedidos não cancelados) */}
                    {pedido.status !== 'CANCELADO' && (
                      <button 
                        onClick={() => handleCancelarPedido(pedido.id)}
                        style={styles.btnCancelar}
                        title="Cancelar Pedido"
                      >
                        <XCircle size={15} /> Cancelar
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {pedidosFiltrados.length === 0 && (
          <div style={styles.emptyState}>
            <FileText size={32} color="#9CA3AF" />
            <p>Nenhum pedido encontrado para os filtros selecionados.</p>
          </div>
        )}
      </div>
    </div>
  );
}

// Estilos in-line mantendo o padrão visual das telas anteriores
const styles = {
  container: { fontFamily: 'sans-serif', paddingBottom: '3rem' },
  header: { marginBottom: '1.5rem' },
  title: { fontSize: '1.5rem', fontWeight: '600', color: '#111827', margin: '0 0 0.25rem 0' },
  subtitle: { fontSize: '0.875rem', color: '#6B7280', margin: 0 },
  toolbar: { display: 'flex', gap: '1rem', marginBottom: '1.5rem', flexWrap: 'wrap' },
  searchBox: { position: 'relative', flex: 1, minWidth: '280px' },
  searchIcon: { position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)' },
  searchInput: { width: '100%', padding: '0.6rem 1rem 0.6rem 2.5rem', border: '1px solid #D1D5DB', borderRadius: '6px', fontSize: '0.875rem', outline: 'none', boxSizing: 'border-box' },
  filterBox: { display: 'flex', alignItems: 'center' },
  selectInput: { padding: '0.6rem 2rem 0.6rem 1rem', border: '1px solid #D1D5DB', borderRadius: '6px', fontSize: '0.875rem', outline: 'none', backgroundColor: 'white' },
  tableCard: { backgroundColor: 'white', border: '1px solid #E5E7EB', borderRadius: '8px', overflow: 'hidden', boxShadow: '0 1px 2px rgba(0,0,0,0.05)' },
  table: { width: '100%', borderCollapse: 'collapse', textAlign: 'left' },
  th: { padding: '0.875rem 1rem', borderBottom: '1px solid #E5E7EB', backgroundColor: '#F9FAFB', fontSize: '0.75rem', fontWeight: '600', color: '#6B7280', letterSpacing: '0.05em' },
  tr: { borderBottom: '1px solid #E5E7EB' },
  td: { padding: '1rem', fontSize: '0.875rem', color: '#374151' },
  tdBold: { padding: '1rem', fontSize: '0.875rem', color: '#111827', fontWeight: '600' },
  
  // Badges de Status
  badgeEmAberto: { backgroundColor: '#FEF3C7', color: '#D97706', padding: '0.25rem 0.6rem', borderRadius: '9999px', fontSize: '0.75rem', fontWeight: '600' },
  badgePago: { backgroundColor: '#D1FAE5', color: '#065F46', padding: '0.25rem 0.6rem', borderRadius: '9999px', fontSize: '0.75rem', fontWeight: '600' },
  badgeCancelado: { backgroundColor: '#FEE2E2', color: '#DC2626', padding: '0.25rem 0.6rem', borderRadius: '9999px', fontSize: '0.75rem', fontWeight: '600' },

  // Botões de Ação
  actionsContainer: { display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' },
  btnPago: { display: 'inline-flex', alignItems: 'center', gap: '0.25rem', backgroundColor: '#ECFDF5', color: '#059669', border: '1px solid #A7F3D0', padding: '0.35rem 0.65rem', borderRadius: '6px', fontSize: '0.75rem', fontWeight: '600', cursor: 'pointer' },
  btnCancelar: { display: 'inline-flex', alignItems: 'center', gap: '0.25rem', backgroundColor: '#FEF2F2', color: '#DC2626', border: '1px solid #FECACA', padding: '0.35rem 0.65rem', borderRadius: '6px', fontSize: '0.75rem', fontWeight: '600', cursor: 'pointer' },

  emptyState: { display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '3rem', color: '#6B7280', gap: '0.5rem' }
};