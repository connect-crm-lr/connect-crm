    import React from 'react';

export default function Dashboard() {
  // Dados simulados baseados no protótipo para a montagem visual
  const dataAtual = "Segunda-feira, 28 de setembro de 2026";
  const vendasMes = "R$ 2.720,00";
  const mesReferencia = "Setembro de 2026";

  const negociacoes = [
    { etapa: 'Primeiro contato', count: 2, color: '#DBEAFE', textColor: '#1E40AF' },
    { etapa: 'Mostruário enviado', count: 2, color: '#E0F2FE', textColor: '#0369A1' },
    { etapa: 'Aguardando resposta', count: 1, color: '#FFEDD5', textColor: '#C2410C' },
    { etapa: 'Fechada', count: 1, color: '#F97316', textColor: 'white' },
    { etapa: 'Perdida', count: 1, color: '#F3F4F6', textColor: '#6B7280' },
  ];

  const pendencias = [
    { cliente: 'Ateliê Bella Noiva', descricao: 'Retornar sobre disponibilidade de Tule na cor marfim.' },
    { cliente: 'Casa Têxtil Moreno', descricao: 'Enviar tabela de preços atualizada.' },
  ];

  const estoqueBaixo = [
    { tecido: 'Linho Natural', cor: 'Natural', metragem: 15 },
    { tecido: 'Tule de Festa', cor: 'Marfim', metragem: 8.5 },
  ];

  return (
    <div style={styles.container}>
      {/* Cabeçalho */}
      <div style={styles.header}>
        <h1 style={styles.title}>Dashboard</h1>
        <p style={styles.date}>{dataAtual}</p>
      </div>

      {/* Grid de Cartões */}
      <div style={styles.grid}>
        
        {/* Cartão 1: Vendas do Mês */}
        <div style={styles.card}>
          <h2 style={styles.cardTitle}>VENDAS DO MÊS</h2>
          <p style={styles.salesValue}>{vendasMes}</p>
          <p style={styles.salesMonth}>{mesReferencia}</p>
        </div>

        {/* Cartão 2: Negociações por Etapa */}
        <div style={styles.card}>
          <h2 style={styles.cardTitle}>NEGOCIAÇÕES POR ETAPA</h2>
          <div style={styles.list}>
            {negociacoes.map((item, index) => (
              <div key={index} style={styles.listItem}>
                <span style={{ 
                  ...styles.badge, 
                  backgroundColor: item.color, 
                  color: item.textColor 
                }}>
                  {item.etapa}
                </span>
                <span style={styles.count}>{item.count}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Cartão 3: Pendências de Hoje */}
        <div style={styles.card}>
          <h2 style={styles.cardTitle}>PENDÊNCIAS DE HOJE</h2>
          <div style={styles.list}>
            {pendencias.map((item, index) => (
              <div key={index} style={styles.pendenciaItem}>
                <div>
                  <p style={styles.clienteName}>{item.cliente}</p>
                  <p style={styles.descricao}>{item.descricao}</p>
                </div>
                <button style={styles.btnConcluir}>Concluir</button>
              </div>
            ))}
          </div>
        </div>

        {/* Cartão 4: Tecidos com Estoque Baixo */}
        <div style={styles.card}>
          <h2 style={styles.cardTitle}>TECIDOS COM ESTOQUE BAIXO</h2>
          <div style={styles.list}>
            {estoqueBaixo.map((item, index) => (
              <div key={index} style={styles.estoqueItem}>
                <div>
                  <p style={styles.tecidoName}>{item.tecido}</p>
                  <p style={styles.corName}>{item.cor}</p>
                </div>
                <div style={styles.metragemBadge}>
                  {item.metragem} m
                </div>
              </div>
            ))}
          </div>
        </div>

      </div>
    </div>
  );
}

// Estilos in-line
const styles = {
  container: {
    fontFamily: 'sans-serif',
  },
  header: {
    marginBottom: '2rem',
  },
  title: {
    fontSize: '1.5rem',
    fontWeight: '600',
    color: '#111827',
    margin: '0 0 0.5rem 0',
  },
  date: {
    fontSize: '0.875rem',
    color: '#6B7280',
    margin: 0,
  },
  grid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))',
    gap: '1.5rem',
  },
  card: {
    backgroundColor: 'white',
    borderRadius: '8px',
    padding: '1.5rem',
    boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.1)',
  },
  cardTitle: {
    fontSize: '0.75rem',
    fontWeight: '600',
    color: '#9CA3AF',
    letterSpacing: '0.05em',
    marginBottom: '1.5rem',
    textTransform: 'uppercase',
  },
  salesValue: {
    fontSize: '2.25rem',
    fontWeight: '700',
    color: '#111827',
    margin: '0 0 0.5rem 0',
  },
  salesMonth: {
    fontSize: '0.875rem',
    color: '#6B7280',
    margin: 0,
  },
  list: {
    display: 'flex',
    flexDirection: 'column',
    gap: '1rem',
  },
  listItem: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  badge: {
    padding: '0.25rem 0.75rem',
    borderRadius: '9999px',
    fontSize: '0.75rem',
    fontWeight: '500',
  },
  count: {
    fontSize: '0.875rem',
    color: '#374151',
    fontWeight: '500',
  },
  pendenciaItem: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    borderBottom: '1px solid #F3F4F6',
    paddingBottom: '1rem',
  },
  clienteName: {
    fontSize: '0.875rem',
    fontWeight: '500',
    color: '#111827',
    margin: '0 0 0.25rem 0',
  },
  descricao: {
    fontSize: '0.875rem',
    color: '#6B7280',
    margin: 0,
  },
  btnConcluir: {
    backgroundColor: 'transparent',
    border: '1px solid #10B981', // Verde
    color: '#10B981',
    padding: '0.25rem 0.75rem',
    borderRadius: '4px',
    fontSize: '0.75rem',
    fontWeight: '500',
    cursor: 'pointer',
  },
  estoqueItem: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    borderBottom: '1px solid #F3F4F6',
    paddingBottom: '1rem',
  },
  tecidoName: {
    fontSize: '0.875rem',
    fontWeight: '500',
    color: '#111827',
    margin: '0 0 0.25rem 0',
  },
  corName: {
    fontSize: '0.875rem',
    color: '#6B7280',
    margin: 0,
  },
  metragemBadge: {
    backgroundColor: '#FEF3C7', // Amarelo claro
    color: '#D97706', // Laranja escuro
    padding: '0.25rem 0.75rem',
    borderRadius: '9999px',
    fontSize: '0.75rem',
    fontWeight: '600',
  },
};