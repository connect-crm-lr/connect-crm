import React, { useState } from 'react';
import { Search, Plus, MessageSquare } from 'lucide-react';
import FichaCliente from '../components/FichaCliente';

export default function Clientes() {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedTag, setSelectedTag] = useState('Todos');
  const [clienteSelecionado, setClienteSelecionado] = useState(null);

  // MOCK DE DADOS: Declarado diretamente aqui para evitar criar o ficheiro src/mocks/clientes.js
  const clientesMock = [
    {
      id: 1,
      nome: 'Ateliê Bella Noiva',
      telefone: '11982345678', 
      telefoneDisplay: '(11) 98234-5678',
      email: 'contato@bellanoiva.com.br',
      documento: '28.947.613/0001-55',
      cidade: 'São Paulo / SP',
      tags: ['Moda Festa'],
      pedidoEmAberto: true,
      qtdPedidosEmAberto: 2,
      valorTotalAberto: '5.879,00',
      numeroCompras: 2,
      dataUltimaCompra: '20/07/2026'
    },
    {
      id: 2,
      nome: 'Casa Têxtil Moreno',
      telefone: '1134567890',
      telefoneDisplay: '(11) 3456-7890',
      email: 'contato@morenotextil.com.br',
      documento: '12.345.678/0001-99',
      cidade: 'Campinas / SP',
      tags: ['Linho', 'Estofaria'],
      pedidoEmAberto: false,
    },
    {
      id: 3,
      nome: 'Studio Decor SP',
      telefone: '11976543210',
      telefoneDisplay: '(11) 97654-3210',
      email: 'studio@decorsp.com.br',
      documento: '98.765.432/0001-11',
      cidade: 'São Paulo / SP',
      tags: ['Estofaria'],
      pedidoEmAberto: true,
      qtdPedidosEmAberto: 1,
      valorTotalAberto: '3.300,00',
      numeroCompras: 5,
      dataUltimaCompra: '05/08/2026'
    },
  ];

  const todasTags = ['Moda Festa', 'Linho', 'Estofaria', 'Malha'];

  const clientesFiltrados = clientesMock.filter(cliente => {
    const matchName = cliente.nome.toLowerCase().includes(searchTerm.toLowerCase());
    const matchTag = selectedTag === 'Todos' || cliente.tags.includes(selectedTag);
    return matchName && matchTag;
  });

  const handleWhatsAppList = (e, cliente) => {
    e.stopPropagation(); // Evita que o clique no botão abra a ficha
    let mensagem = `Olá ${cliente.nome}, como podemos ajudar hoje?`;
    if (selectedTag !== 'Todos') {
      mensagem = `Olá ${cliente.nome}, acabou de chegar um novo mostruário de ${selectedTag}, posso agendar uma visita?`;
    }
    const url = `https://wa.me/55${cliente.telefone}?text=${encodeURIComponent(mensagem)}`;
    window.open(url, '_blank');
  };

  const handleExcluirTag = (tagParaExcluir) => {
     alert(`Ao excluir a tag "${tagParaExcluir}", os vínculos serão removidos. Confirmar?`);
     if(selectedTag === tagParaExcluir) setSelectedTag('Todos');
  };

  return (
    <div style={styles.layoutWrapper}>
      
      <div style={styles.mainContainer}>
        <div style={styles.header}>
          <div>
            <h1 style={styles.title}>Clientes</h1>
            <p style={styles.subtitle}>{clientesMock.length} clientes</p>
          </div>
          <button style={styles.btnPrimary}>
            <Plus size={18} />
            Novo Cliente
          </button>
        </div>

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

        <div style={styles.list}>
          {clientesFiltrados.map((cliente) => {
            const isSelected = clienteSelecionado?.id === cliente.id;
            
            return (
              <div 
                key={cliente.id} 
                style={{
                  ...styles.card, 
                  borderColor: isSelected ? '#4F46E5' : '#E5E7EB', 
                  borderWidth: isSelected ? '2px' : '1px'
                }}
                // A ação de abrir a ficha volta para o CARD INTEIRO (conforme o teu pedido)
                onClick={() => setClienteSelecionado(cliente)} 
              >
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
                    onClick={(e) => handleWhatsAppList(e, cliente)}
                    style={styles.btnWhatsApp}
                  >
                    <MessageSquare size={16} />
                    WhatsApp
                  </button>
                </div>
              </div>
            )
          })}
          {clientesFiltrados.length === 0 && (
            <p style={styles.emptyState}>Nenhum cliente encontrado.</p>
          )}
        </div>
      </div>

      {clienteSelecionado && (
        <FichaCliente 
          cliente={clienteSelecionado} 
          onClose={() => setClienteSelecionado(null)} 
        />
      )}
      
    </div>
  );
}

const styles = {
  layoutWrapper: { display: 'flex', gap: '1.5rem', height: '100%', alignItems: 'flex-start' },
  mainContainer: { flex: 1, fontFamily: 'sans-serif' },
  header: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' },
  title: { fontSize: '1.5rem', fontWeight: '600', color: '#111827', margin: '0 0 0.25rem 0' },
  subtitle: { fontSize: '0.875rem', color: '#6B7280', margin: 0 },
  btnPrimary: { display: 'flex', alignItems: 'center', gap: '0.5rem', backgroundColor: '#4F46E5', color: 'white', border: 'none', padding: '0.5rem 1rem', borderRadius: '6px', fontSize: '0.875rem', fontWeight: '500', cursor: 'pointer' },
  toolbar: { display: 'flex', gap: '1rem', marginBottom: '2rem', flexWrap: 'wrap' },
  searchBox: { position: 'relative', flex: 1, minWidth: '250px' },
  searchIcon: { position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)' },
  searchInput: { width: '100%', padding: '0.5rem 1rem 0.5rem 2.5rem', border: '1px solid #D1D5DB', borderRadius: '6px', fontSize: '0.875rem', outline: 'none' },
  filterBox: { display: 'flex', alignItems: 'center', gap: '0.5rem' },
  selectInput: { padding: '0.5rem 2rem 0.5rem 1rem', border: '1px solid #D1D5DB', borderRadius: '6px', fontSize: '0.875rem', outline: 'none', backgroundColor: 'white' },
  btnDeleteTag: { backgroundColor: '#FEE2E2', color: '#DC2626', border: 'none', padding: '0.5rem 0.75rem', borderRadius: '6px', fontSize: '0.75rem', fontWeight: '600', cursor: 'pointer' },
  list: { display: 'flex', flexDirection: 'column', gap: '1rem' },
  card: { backgroundColor: 'white', borderRadius: '8px', padding: '1.25rem', boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.05)', cursor: 'pointer', transition: 'all 0.2s' },
  cardHeader: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.25rem' },
  clientName: { fontSize: '1rem', fontWeight: '600', color: '#111827', margin: 0 },
  badgeWarning: { backgroundColor: '#FEF3C7', color: '#D97706', padding: '0.25rem 0.5rem', borderRadius: '4px', fontSize: '0.75rem', fontWeight: '500' },
  clientPhone: { fontSize: '0.875rem', color: '#6B7280', margin: '0 0 1rem 0' },
  cardFooter: { display: 'flex', justifyContent: 'space-between', alignItems: 'center' },
  tagsContainer: { display: 'flex', gap: '0.5rem', flexWrap: 'wrap' },
  tagBadge: { backgroundColor: '#EEF2FF', color: '#4F46E5', padding: '0.25rem 0.5rem', borderRadius: '4px', fontSize: '0.75rem', fontWeight: '500' },
  btnWhatsApp: { display: 'flex', alignItems: 'center', gap: '0.35rem', backgroundColor: '#10B981', color: 'white', border: 'none', padding: '0.35rem 0.75rem', borderRadius: '6px', fontSize: '0.75rem', fontWeight: '500', cursor: 'pointer' },
  emptyState: { textAlign: 'center', color: '#6B7280', padding: '2rem 0' }
};