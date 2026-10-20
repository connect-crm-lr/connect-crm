import React, { useState } from 'react';
import { Plus, MessageCircle } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export default function Funil() {
  const navigate = useNavigate();

  // Dados simulados baseados no protótipo e regras do enum (MAIÚSCULAS)
  const [negociacoes, setNegociacoes] = useState([
    { id: 1, cliente: 'Casa Têxtil Moreno', descricao: 'Proposta de Linho Natural - 200m', data: '20/09/2026', etapa: 'PRIMEIRO_CONTATO' },
    { id: 2, cliente: 'Malhas Oliveira & Filhos', descricao: 'Reposição de Malha Canelada', data: '22/09/2026', etapa: 'PRIMEIRO_CONTATO' },
    { id: 3, cliente: 'Ateliê Bella Noiva', descricao: 'Tule de Festa e Crepe', data: '15/09/2026', etapa: 'MOSTRUARIO_ENVIADO' },
    { id: 4, cliente: 'Confecções Araújo ME', descricao: 'Jeans Stretch', data: '25/09/2026', etapa: 'AGUARDANDO_RESPOSTA' }
  ]);

  const etapas = [
    { id: 'PRIMEIRO_CONTATO', nome: 'PRIMEIRO CONTATO', color: '#DBEAFE' },
    { id: 'MOSTRUARIO_ENVIADO', nome: 'MOSTRUÁRIO ENVIADO', color: '#E0F2FE' },
    { id: 'AGUARDANDO_RESPOSTA', nome: 'AGUARDANDO RESPOSTA', color: '#FFEDD5' },
    { id: 'FECHADA', nome: 'FECHADA', color: '#F97316' },
    { id: 'PERDIDA', nome: 'PERDIDA', color: '#F3F4F6' }
  ];

  // Lógica de Drag & Drop
  const handleDragStart = (e, id) => {
    e.dataTransfer.setData("id_negociacao", id);
  };

  const handleDragOver = (e) => {
    e.preventDefault(); // Necessário para permitir o Drop
  };

  const handleDrop = (e, novaEtapa) => {
    const id = parseInt(e.dataTransfer.getData("id_negociacao"));
    const negociacao = negociacoes.find(n => n.id === id);

    if (negociacao && negociacao.etapa !== novaEtapa) {
      // Regra de Negócio: Mover para FECHADA
      if (novaEtapa === 'FECHADA') {
        const confirmar = window.confirm(`Criar pedido para o cliente: ${negociacao.cliente}?`);
        if (confirmar) {
          // Navega para a tela de Novo Pedido passando o cliente via state do Router
          navigate('/novo-pedido', { state: { clientePreSelecionado: negociacao.cliente } });
        }
      }

      // Atualiza o estado da negociação
      setNegociacoes(prev => 
        prev.map(n => n.id === id ? { ...n, etapa: novaEtapa } : n)
      );
    }
  };

  return (
    <div style={styles.container}>
      {/* Cabeçalho */}
      <div style={styles.header}>
        <div>
          <h1 style={styles.title}>Funil de Negociações</h1>
          <p style={styles.subtitle}>{negociacoes.length} negociações no total</p>
        </div>
        <button style={styles.btnPrimary}>
          <Plus size={18} />
          Nova Negociação
        </button>
      </div>

      {/* Quadro Kanban */}
      <div style={styles.board}>
        {etapas.map(etapa => {
          const cardsDaEtapa = negociacoes.filter(n => n.etapa === etapa.id);
          
          return (
            <div 
              key={etapa.id} 
              style={styles.column}
              onDragOver={handleDragOver}
              onDrop={(e) => handleDrop(e, etapa.id)}
            >
              {/* Título da Coluna */}
              <div style={{...styles.columnHeader, backgroundColor: etapa.color}}>
                <span style={styles.columnTitle}>{etapa.nome}</span>
                <span style={styles.columnCount}>{cardsDaEtapa.length}</span>
              </div>

              {/* Área dos Cartões */}
              <div style={styles.cardsContainer}>
                {cardsDaEtapa.map(card => (
                  <div 
                    key={card.id} 
                    style={styles.card}
                    draggable
                    onDragStart={(e) => handleDragStart(e, card.id)}
                  >
                    <div style={styles.cardHeader}>
                      <span style={styles.cardClient}>{card.cliente}</span>
                      {card.etapa === 'FECHADA' && <span style={styles.badgeGanho}>GANHO</span>}
                    </div>
                    <p style={styles.cardDesc}>{card.descricao}</p>
                    <div style={styles.cardFooter}>
                      <span style={styles.cardDate}>{card.data}</span>
                      <MessageCircle size={16} color="#10B981" style={{cursor: 'pointer'}} />
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )
        })}
      </div>
    </div>
  );
}

// Estilos In-line
const styles = {
  container: { display: 'flex', flexDirection: 'column', height: '100%', fontFamily: 'sans-serif' },
  header: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' },
  title: { fontSize: '1.5rem', fontWeight: '600', color: '#111827', margin: '0 0 0.25rem 0' },
  subtitle: { fontSize: '0.875rem', color: '#6B7280', margin: 0 },
  btnPrimary: { display: 'flex', alignItems: 'center', gap: '0.5rem', backgroundColor: '#4F46E5', color: 'white', border: 'none', padding: '0.5rem 1rem', borderRadius: '6px', fontSize: '0.875rem', fontWeight: '500', cursor: 'pointer' },
  board: { display: 'flex', gap: '1rem', overflowX: 'auto', flex: 1, paddingBottom: '1rem' },
  column: { minWidth: '280px', width: '280px', backgroundColor: '#F9FAFB', borderRadius: '8px', display: 'flex', flexDirection: 'column' },
  columnHeader: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.75rem 1rem', borderTopLeftRadius: '8px', borderTopRightRadius: '8px', marginBottom: '0.5rem' },
  columnTitle: { fontSize: '0.75rem', fontWeight: '600', color: '#374151', letterSpacing: '0.05em' },
  columnCount: { backgroundColor: 'rgba(255,255,255,0.5)', padding: '0.1rem 0.5rem', borderRadius: '999px', fontSize: '0.75rem', color: '#374151', fontWeight: '600' },
  cardsContainer: { padding: '0 0.5rem', display: 'flex', flexDirection: 'column', gap: '0.75rem', flex: 1, overflowY: 'auto' },
  card: { backgroundColor: 'white', padding: '1rem', borderRadius: '6px', boxShadow: '0 1px 2px rgba(0,0,0,0.05)', border: '1px solid #E5E7EB', cursor: 'grab' },
  cardHeader: { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.5rem' },
  cardClient: { fontSize: '0.875rem', fontWeight: '600', color: '#111827' },
  badgeGanho: { backgroundColor: '#D1FAE5', color: '#065F46', fontSize: '0.65rem', padding: '0.15rem 0.4rem', borderRadius: '4px', fontWeight: '600' },
  cardDesc: { fontSize: '0.8rem', color: '#6B7280', margin: '0 0 1rem 0', lineHeight: '1.4' },
  cardFooter: { display: 'flex', justifyContent: 'space-between', alignItems: 'center' },
  cardDate: { fontSize: '0.75rem', color: '#9CA3AF' }
};