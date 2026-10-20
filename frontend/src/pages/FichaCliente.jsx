import React, { useState } from 'react';
import { MessageSquare, X } from 'lucide-react';

export default function FichaCliente({ cliente, onClose }) {
  const [abaAtiva, setAbaAtiva] = useState('Dados');

  if (!cliente) return null;

  const abas = ['Dados', 'Interações', 'Follow-ups', 'Pedidos'];

  const handleWhatsApp = () => {
    const mensagem = encodeURIComponent(`Olá ${cliente.nome}, tudo bem?`);
    window.open(`https://wa.me/55${cliente.telefone}?text=${mensagem}`, '_blank');
  };

  return (
    <div style={styles.container}>
      {/* Botão de Fechar */}
      <button onClick={onClose} style={styles.closeBtn} title="Fechar Ficha">
        <X size={20} color="#6B7280" />
      </button>

      {/* Alerta de Pedido em Aberto */}
      {cliente.pedidoEmAberto && (
        <div style={styles.alertaBanner}>
          Este cliente tem pedido em aberto — R$ 5.879,00 - já comprou 2x, a última em 20/07/2026
        </div>
      )}

      {/* Cabeçalho da Ficha */}
      <div style={styles.header}>
        <div>
          <h2 style={styles.nome}>{cliente.nome}</h2>
          <p style={styles.telefone}>{cliente.telefoneDisplay}</p>
        </div>
        <button onClick={handleWhatsApp} style={styles.btnWhatsApp}>
          <MessageSquare size={16} />
          Enviar WhatsApp
        </button>
      </div>

      {/* Navegação por Abas */}
      <div style={styles.tabsContainer}>
        {abas.map(aba => (
          <button 
            key={aba} 
            onClick={() => setAbaAtiva(aba)}
            style={{
              ...styles.tabBtn,
              ...(abaAtiva === aba ? styles.tabAtiva : {})
            }}
          >
            {aba}
          </button>
        ))}
      </div>

      {/* Conteúdo das Abas */}
      <div style={styles.tabContent}>
        {abaAtiva === 'Dados' && (
          <div style={styles.gridDados}>
            <div style={styles.dadoItem}>
              <span style={styles.dadoLabel}>E-MAIL</span>
              <span style={styles.dadoValor}>{cliente.email || 'Não informado'}</span>
            </div>
            <div style={styles.dadoItem}>
              <span style={styles.dadoLabel}>CPF/CNPJ</span>
              <span style={styles.dadoValor}>{cliente.documento || 'Não informado'}</span>
            </div>
            <div style={styles.dadoItem}>
              <span style={styles.dadoLabel}>CIDADE/UF</span>
              <span style={styles.dadoValor}>{cliente.cidade || 'Não informado'}</span>
            </div>
            <div style={styles.dadoItem}>
              <span style={styles.dadoLabel}>TAGS</span>
              <div style={styles.tagsContainer}>
                {cliente.tags.map(tag => (
                  <span key={tag} style={styles.tagBadge}>{tag}</span>
                ))}
              </div>
            </div>
          </div>
        )}

        {/* Placeholders para as outras abas do MVP */}
        {abaAtiva === 'Interações' && (
          <div>
            <button style={styles.btnAcao}>+ Registrar interação</button>
            <p style={styles.placeholder}>Nenhuma interação registrada.</p>
          </div>
        )}
        {abaAtiva === 'Follow-ups' && (
           <div>
             <button style={styles.btnAcao}>+ Novo Follow-up</button>
             <p style={styles.placeholder}>Nenhuma pendência.</p>
           </div>
        )}
        {abaAtiva === 'Pedidos' && <p style={styles.placeholder}>Histórico de pedidos vazio.</p>}
      </div>
    </div>
  );
}

const styles = {
  container: {
    width: '400px',
    backgroundColor: 'white',
    borderLeft: '1px solid #E5E7EB',
    padding: '1.5rem',
    display: 'flex',
    flexDirection: 'column',
    position: 'relative',
    height: '100%', // Para ocupar a altura da tela
    boxShadow: '-2px 0 5px rgba(0,0,0,0.05)',
  },
  closeBtn: {
    position: 'absolute', top: '10px', right: '10px', background: 'none', border: 'none', cursor: 'pointer'
  },
  alertaBanner: {
    backgroundColor: '#FEF3C7', color: '#B45309', padding: '1rem', borderRadius: '6px', fontSize: '0.875rem', marginBottom: '1.5rem', fontWeight: '500'
  },
  header: {
    display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1.5rem'
  },
  nome: { fontSize: '1.25rem', fontWeight: '600', color: '#111827', margin: '0 0 0.25rem 0' },
  telefone: { fontSize: '0.875rem', color: '#6B7280', margin: 0 },
  btnWhatsApp: {
    display: 'flex', alignItems: 'center', gap: '0.5rem', backgroundColor: '#10B981', color: 'white', border: 'none', padding: '0.5rem 0.75rem', borderRadius: '6px', fontSize: '0.875rem', cursor: 'pointer', fontWeight: '500'
  },
  tabsContainer: {
    display: 'flex', borderBottom: '1px solid #E5E7EB', marginBottom: '1.5rem', gap: '1rem'
  },
  tabBtn: {
    background: 'none', border: 'none', borderBottom: '2px solid transparent', padding: '0.5rem 0', fontSize: '0.875rem', color: '#6B7280', cursor: 'pointer', fontWeight: '500'
  },
  tabAtiva: {
    color: '#4F46E5', borderBottomColor: '#4F46E5'
  },
  tabContent: { flex: 1 },
  gridDados: { display: 'flex', flexDirection: 'column', gap: '1.5rem' },
  dadoItem: { display: 'flex', flexDirection: 'column', gap: '0.25rem' },
  dadoLabel: { fontSize: '0.75rem', color: '#9CA3AF', fontWeight: '600', letterSpacing: '0.05em' },
  dadoValor: { fontSize: '0.875rem', color: '#111827' },
  tagsContainer: { display: 'flex', gap: '0.5rem', flexWrap: 'wrap' },
  tagBadge: { backgroundColor: '#EEF2FF', color: '#4F46E5', padding: '0.25rem 0.5rem', borderRadius: '4px', fontSize: '0.75rem', fontWeight: '500' },
  btnAcao: { backgroundColor: 'transparent', border: '1px solid #4F46E5', color: '#4F46E5', padding: '0.5rem 1rem', borderRadius: '6px', fontSize: '0.875rem', cursor: 'pointer', marginBottom: '1rem' },
  placeholder: { fontSize: '0.875rem', color: '#9CA3AF', fontStyle: 'italic' }
};