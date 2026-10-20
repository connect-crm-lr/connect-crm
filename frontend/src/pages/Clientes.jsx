import React, { useState } from 'react';
import { Search, Plus, MessageSquare } from 'lucide-react';

export default function Clientes() {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedTag, setSelectedTag] = useState('Todos');

  // Dados simulados baseados no protótipo e regras de negócio
  const clientesData = [
    {
      id: 1,
      nome: 'Ateliê Bella Noiva',
      telefone: '5511982345678', // Só dígitos, já com DDI 55 (RD-016); usado direto no link do WhatsApp
      telefoneDisplay: '(11) 98234-5678',
      tags: ['Moda Festa'],
      pedidoEmAberto: true,
    },
    {
      id: 2,
      nome: 'Casa Têxtil Moreno',
      telefone: '551134567890',
      telefoneDisplay: '(11) 3456-7890',
      tags: ['Linho', 'Estofaria'],
      pedidoEmAberto: false,
    },
    {
      id: 3,
      nome: 'Studio Decor SP',
      telefone: '5511976543210',
      telefoneDisplay: '(11) 97654-3210',
      tags: ['Estofaria'],
      pedidoEmAberto: true,
    },
  ];

  const todasTags = ['Moda Festa', 'Linho', 'Estofaria', 'Malha'];

  // Lógica de filtragem
  const clientesFiltrados = clientesData.filter(cliente => {
    const matchName = cliente.nome.toLowerCase().includes(searchTerm.toLowerCase());
    const matchTag = selectedTag === 'Todos' || cliente.tags.includes(selectedTag);
    return matchName && matchTag;
  });

  // Função para abrir o WhatsApp conforme a regra de negócio (RF05)
  const handleWhatsApp = (cliente) => {
    let mensagem = `Olá ${cliente.nome}, como podemos ajudar hoje?`;
    
    // Se houver uma tag selecionada, usa o texto sugerido nos requisitos
    if (selectedTag !== 'Todos') {
      mensagem = `Olá ${cliente.nome}, acabou de chegar um novo mostruário de ${selectedTag}, posso agendar uma visita?`;
    }

    const mensagemCodificada = encodeURIComponent(mensagem);
    // Formato exigido: https://wa.me/<telefone>?text=<mensagem codificada>
    const url = `https://wa.me/${cliente.telefone}?text=${mensagemCodificada}`;
    window.open(url, '_blank');
  };

  const handleExcluirTag = (tagParaExcluir) => {
     // Mockup: Apenas alerta, pois no futuro chamará a API e pedirá confirmação
     alert(`Ao excluir a tag "${tagParaExcluir}", os vínculos serão removidos. Confirmar?`);
     if(selectedTag === tagParaExcluir) setSelectedTag('Todos');
  };

  return (
    <div style={styles.container}>
      {/* Cabeçalho */}
      <div style={styles.header}>
        <div>
          <h1 style={styles.title}>Clientes</h1>
          <p style={styles.subtitle}>{clientesData.length} clientes</p>
        </div>
        <button style={styles.btnPrimary}>
          <Plus size={18} />
          Novo Cliente
        </button>
      </div>

      {/* Barra de Pesquisa e Filtros */}
      <div style={styles.toolbar}>
        <div style={styles.searchBox}>
          <Search size={18} color="#9CA3AF" style={styles.searchIcon} />
          <input
            type="text"
            placeholder="Buscar por nome..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            style={styles.searchInput}
          />
        </div>

        <div style={styles.filterBox}>
          <select 
            value={selectedTag} 
            onChange={(e) => setSelectedTag(e.target.value)}
            style={styles.selectInput}
          >
            <option value="Todos">Todas as Tags</option>
            {todasTags.map(tag => (
              <option key={tag} value={tag}>{tag}</option>
            ))}
          </select>
          
          {/* Mostra o "x" para excluir a tag se uma tag específica estiver selecionada */}
          {selectedTag !== 'Todos' && (
             <button 
                onClick={() => handleExcluirTag(selectedTag)}
                style={styles.btnDeleteTag}
                title="Excluir Tag do Sistema"
             >
                ✕ Excluir tag
             </button>
          )}
        </div>
      </div>

      {/* Lista de Clientes */}
      <div style={styles.list}>
        {clientesFiltrados.map((cliente) => (
          <div key={cliente.id} style={styles.card}>
            <div style={styles.cardHeader}>
              <h2 style={styles.clientName}>{cliente.nome}</h2>
              {cliente.pedidoEmAberto && (
                <span style={styles.badgeWarning}>Pedido em aberto</span>
              )}
            </div>
            
            <p style={styles.clientPhone}>{cliente.telefoneDisplay}</p>
            
            <div style={styles.cardFooter}>
              <div style={styles.tagsContainer}>
                {cliente.tags.map(tag => (
                  <span key={tag} style={styles.tagBadge}>{tag}</span>
                ))}
              </div>
              
              <button 
                onClick={() => handleWhatsApp(cliente)}
                style={styles.btnWhatsApp}
              >
                <MessageSquare size={16} />
                WhatsApp
              </button>
            </div>
          </div>
        ))}
        {clientesFiltrados.length === 0 && (
          <p style={styles.emptyState}>Nenhum cliente encontrado.</p>
        )}
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
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '1.5rem',
  },
  title: {
    fontSize: '1.5rem',
    fontWeight: '600',
    color: '#111827',
    margin: '0 0 0.25rem 0',
  },
  subtitle: {
    fontSize: '0.875rem',
    color: '#6B7280',
    margin: 0,
  },
  btnPrimary: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.5rem',
    backgroundColor: '#4F46E5', // Azul principal
    color: 'white',
    border: 'none',
    padding: '0.5rem 1rem',
    borderRadius: '6px',
    fontSize: '0.875rem',
    fontWeight: '500',
    cursor: 'pointer',
  },
  toolbar: {
    display: 'flex',
    gap: '1rem',
    marginBottom: '2rem',
    flexWrap: 'wrap',
  },
  searchBox: {
    position: 'relative',
    flex: 1,
    minWidth: '250px',
  },
  searchIcon: {
    position: 'absolute',
    left: '0.75rem',
    top: '50%',
    transform: 'translateY(-50%)',
  },
  searchInput: {
    width: '100%',
    padding: '0.5rem 1rem 0.5rem 2.5rem',
    border: '1px solid #D1D5DB',
    borderRadius: '6px',
    fontSize: '0.875rem',
    outline: 'none',
  },
  filterBox: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.5rem',
  },
  selectInput: {
    padding: '0.5rem 2rem 0.5rem 1rem',
    border: '1px solid #D1D5DB',
    borderRadius: '6px',
    fontSize: '0.875rem',
    outline: 'none',
    backgroundColor: 'white',
  },
  btnDeleteTag: {
    backgroundColor: '#FEE2E2', // Vermelho claro
    color: '#DC2626', // Vermelho escuro
    border: 'none',
    padding: '0.5rem 0.75rem',
    borderRadius: '6px',
    fontSize: '0.75rem',
    fontWeight: '600',
    cursor: 'pointer',
  },
  list: {
    display: 'flex',
    flexDirection: 'column',
    gap: '1rem',
  },
  card: {
    backgroundColor: 'white',
    border: '1px solid #E5E7EB',
    borderRadius: '8px',
    padding: '1.25rem',
    boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.05)',
  },
  cardHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '0.25rem',
  },
  clientName: {
    fontSize: '1rem',
    fontWeight: '600',
    color: '#111827',
    margin: 0,
  },
  badgeWarning: {
    backgroundColor: '#FEF3C7',
    color: '#D97706',
    padding: '0.25rem 0.5rem',
    borderRadius: '4px',
    fontSize: '0.75rem',
    fontWeight: '500',
  },
  clientPhone: {
    fontSize: '0.875rem',
    color: '#6B7280',
    margin: '0 0 1rem 0',
  },
  cardFooter: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  tagsContainer: {
    display: 'flex',
    gap: '0.5rem',
    flexWrap: 'wrap',
  },
  tagBadge: {
    backgroundColor: '#EEF2FF', // Azul bem clarinho
    color: '#4F46E5', // Azul texto
    padding: '0.25rem 0.5rem',
    borderRadius: '4px',
    fontSize: '0.75rem',
    fontWeight: '500',
  },
  btnWhatsApp: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.35rem',
    backgroundColor: '#10B981', // Verde WhatsApp
    color: 'white',
    border: 'none',
    padding: '0.35rem 0.75rem',
    borderRadius: '6px',
    fontSize: '0.75rem',
    fontWeight: '500',
    cursor: 'pointer',
  },
  emptyState: {
    textAlign: 'center',
    color: '#6B7280',
    padding: '2rem 0',
  }
};