import React, { useState } from 'react';
import { Search, Plus, X, Edit, Trash2, Image as ImageIcon } from 'lucide-react';

export default function Catalogo() {
  const [searchTerm, setSearchTerm] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);

  // Dados simulados baseados no protótipo do Figma[cite: 5]
  const [catalogoData, setCatalogoData] = useState([
    {
      id: 1,
      foto: 'https://images.unsplash.com/photo-1560281358-15b225906f0f?ixlib=rb-4.0.3&auto=format&fit=crop&w=300&q=80',
      nome: 'Crepe Georgette',
      composicao: '100% Poliéster',
      gramatura: '80',
      largura: '1,50',
      preco: '28,90'
    },
    {
      id: 2,
      foto: 'https://images.unsplash.com/photo-1584100936595-c0654b35a113?ixlib=rb-4.0.3&auto=format&fit=crop&w=300&q=80',
      nome: 'Linho Natural',
      composicao: '100% Linho',
      gramatura: '185',
      largura: '1,40',
      preco: '42,50'
    },
    {
      id: 3,
      foto: 'https://images.unsplash.com/photo-1618220179428-22790b46a0eb?ixlib=rb-4.0.3&auto=format&fit=crop&w=300&q=80',
      nome: 'Suede Automotivo',
      composicao: '80% Poliéster / 20% Poliamida',
      gramatura: '270',
      largura: '1,40',
      preco: '55,00'
    }
  ]);

  // Filtro por nome ou composição (RF08)[cite: 5]
  const produtosFiltrados = catalogoData.filter(item => 
    item.nome.toLowerCase().includes(searchTerm.toLowerCase()) ||
    item.composicao.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div style={styles.container}>
      {/* Cabeçalho */}
      <div style={styles.header}>
        <div>
          <h1 style={styles.title}>Catálogo de Tecidos</h1>
          <p style={styles.subtitle}>{catalogoData.length} tecidos cadastrados</p>
        </div>
        <button onClick={() => setIsModalOpen(true)} style={styles.btnPrimary}>
          <Plus size={18} />
          Novo Tecido
        </button>
      </div>

      {/* Barra de Pesquisa */}
      <div style={styles.toolbar}>
        <div style={styles.searchBox}>
          <Search size={18} color="#9CA3AF" style={styles.searchIcon} />
          <input
            type="text"
            placeholder="Buscar por nome ou composição..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            style={styles.searchInput}
          />
        </div>
      </div>

      {/* Grade de Produtos */}
      <div style={styles.grid}>
        {produtosFiltrados.map((item) => (
          <div key={item.id} style={styles.card}>
            <div style={styles.imageContainer}>
              {item.foto ? (
                <img src={item.foto} alt={item.nome} style={styles.image} />
              ) : (
                <div style={styles.noImage}><ImageIcon size={32} color="#9CA3AF" /></div>
              )}
            </div>
            
            <div style={styles.cardContent}>
              <h2 style={styles.productName}>{item.nome}</h2>
              <p style={styles.productSpecs}>{item.composicao}</p>
              <p style={styles.productSpecs}>{item.gramatura} g/m² · {item.largura} m</p>
              <p style={styles.productPrice}>R$ {item.preco}<span style={styles.priceUnit}>/m</span></p>
            </div>

            <div style={styles.cardFooter}>
              <button style={styles.btnAction}><Edit size={16} /> Editar</button>
              <button style={styles.btnActionDanger}><Trash2 size={16} /> Excluir</button>
            </div>
          </div>
        ))}
      </div>
      {produtosFiltrados.length === 0 && (
        <p style={styles.emptyState}>Nenhum tecido encontrado.</p>
      )}

      {/* Modal de Novo Tecido */}
      {isModalOpen && (
        <ModalNovoTecido 
          onClose={() => setIsModalOpen(false)} 
          onSave={(novoTecido) => {
            setCatalogoData([{...novoTecido, id: Date.now()}, ...catalogoData]);
            setIsModalOpen(false);
          }} 
        />
      )}
    </div>
  );
}

// Modal conforme as regras de negócio
function ModalNovoTecido({ onClose, onSave }) {
  const [formData, setFormData] = useState({
    foto: '',
    nome: '',
    composicao: '',
    gramatura: '',
    largura: '',
    preco: ''
  });

  const handleChange = (e) => {
    setFormData({...formData, [e.target.name]: e.target.value});
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    onSave(formData);
  };

  return (
    <div style={styles.modalOverlay}>
      <div style={styles.modalCard}>
        <div style={styles.modalHeader}>
          <h2 style={styles.modalTitle}>Novo Tecido</h2>
          <button onClick={onClose} style={styles.closeBtn}><X size={20} color="#6B7280" /></button>
        </div>

        <form onSubmit={handleSubmit} style={styles.form}>
          <div style={styles.inputGroup}>
            <label style={styles.label}>URL DA FOTO (OPCIONAL)</label>
            <input 
              name="foto" type="url" placeholder="https://..." 
              value={formData.foto} onChange={handleChange} style={styles.input}
            />
          </div>

          <div style={styles.inputGroup}>
            <label style={styles.label}>NOME *</label>
            <input 
              name="nome" type="text" required
              value={formData.nome} onChange={handleChange} style={styles.input}
            />
          </div>

          <div style={styles.inputGroup}>
            <label style={styles.label}>COMPOSIÇÃO *</label>
            <input 
              name="composicao" type="text" placeholder="Ex: 100% Algodão" required
              value={formData.composicao} onChange={handleChange} style={styles.input}
            />
          </div>

          <div style={styles.row}>
            <div style={{...styles.inputGroup, flex: 1}}>
              <label style={styles.label}>GRAMATURA (g/m²) *</label>
              <input 
                name="gramatura" type="number" required
                value={formData.gramatura} onChange={handleChange} style={styles.input}
              />
            </div>
            <div style={{...styles.inputGroup, flex: 1}}>
              <label style={styles.label}>LARGURA (m) *</label>
              <input 
                name="largura" type="number" step="0.01" required
                value={formData.largura} onChange={handleChange} style={styles.input}
              />
            </div>
          </div>

          <div style={styles.row}>
            <div style={{...styles.inputGroup, flex: 1}}>
              <label style={styles.label}>PREÇO POR METRO (R$) *</label>
              <input 
                name="preco" type="number" step="0.01" required
                value={formData.preco} onChange={handleChange} style={styles.input}
              />
            </div>
          </div>

          <div style={styles.modalFooter}>
            <button type="button" onClick={onClose} style={styles.btnCancel}>Cancelar</button>
            <button type="submit" style={styles.btnSubmit}>Salvar</button>
          </div>
        </form>
      </div>
    </div>
  );
}

// Estilos in-line
const styles = {
  container: { fontFamily: 'sans-serif' },
  header: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' },
  title: { fontSize: '1.5rem', fontWeight: '600', color: '#111827', margin: '0 0 0.25rem 0' },
  subtitle: { fontSize: '0.875rem', color: '#6B7280', margin: 0 },
  btnPrimary: { display: 'flex', alignItems: 'center', gap: '0.5rem', backgroundColor: '#4F46E5', color: 'white', border: 'none', padding: '0.5rem 1rem', borderRadius: '6px', fontSize: '0.875rem', fontWeight: '500', cursor: 'pointer' },
  toolbar: { marginBottom: '1.5rem' },
  searchBox: { position: 'relative', width: '100%', maxWidth: '400px' },
  searchIcon: { position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)' },
  searchInput: { width: '100%', padding: '0.5rem 1rem 0.5rem 2.5rem', border: '1px solid #D1D5DB', borderRadius: '6px', fontSize: '0.875rem', outline: 'none' },
  
  grid: { display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: '1.5rem' },
  card: { backgroundColor: 'white', border: '1px solid #E5E7EB', borderRadius: '8px', overflow: 'hidden', display: 'flex', flexDirection: 'column' },
  imageContainer: { width: '100%', height: '160px', backgroundColor: '#F3F4F6' },
  image: { width: '100%', height: '100%', objectFit: 'cover' },
  noImage: { width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center' },
  cardContent: { padding: '1.25rem', flex: 1 },
  productName: { fontSize: '1rem', fontWeight: '600', color: '#111827', margin: '0 0 0.25rem 0' },
  productSpecs: { fontSize: '0.875rem', color: '#6B7280', margin: '0 0 0.25rem 0' },
  productPrice: { fontSize: '1.125rem', fontWeight: '600', color: '#4F46E5', margin: '0.75rem 0 0 0' },
  priceUnit: { fontSize: '0.875rem', color: '#6B7280', fontWeight: 'normal' },
  cardFooter: { display: 'flex', borderTop: '1px solid #E5E7EB' },
  btnAction: { flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem', padding: '0.75rem', background: 'none', border: 'none', borderRight: '1px solid #E5E7EB', color: '#374151', fontSize: '0.875rem', cursor: 'pointer' },
  btnActionDanger: { flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem', padding: '0.75rem', background: 'none', border: 'none', color: '#DC2626', fontSize: '0.875rem', cursor: 'pointer' },
  emptyState: { textAlign: 'center', padding: '2rem', color: '#6B7280', fontSize: '0.875rem' },

  // Modal
  modalOverlay: { position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 50 },
  modalCard: { backgroundColor: 'white', borderRadius: '8px', width: '100%', maxWidth: '500px', padding: '1.5rem', maxHeight: '90vh', overflowY: 'auto' },
  modalHeader: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' },
  modalTitle: { fontSize: '1.25rem', fontWeight: '600', color: '#111827', margin: 0 },
  closeBtn: { background: 'none', border: 'none', cursor: 'pointer', padding: 0 },
  form: { display: 'flex', flexDirection: 'column', gap: '1.25rem' },
  inputGroup: { display: 'flex', flexDirection: 'column', gap: '0.5rem' },
  row: { display: 'flex', gap: '1rem' },
  label: { fontSize: '0.75rem', fontWeight: '600', color: '#6B7280', letterSpacing: '0.05em' },
  input: { padding: '0.75rem', border: '1px solid #D1D5DB', borderRadius: '6px', fontSize: '0.875rem', outline: 'none', width: '100%', boxSizing: 'border-box' },
  modalFooter: { display: 'flex', justifyContent: 'flex-end', gap: '1rem', marginTop: '1rem' },
  btnCancel: { backgroundColor: 'white', border: '1px solid #D1D5DB', color: '#374151', padding: '0.5rem 1rem', borderRadius: '6px', cursor: 'pointer', fontSize: '0.875rem', fontWeight: '500' },
  btnSubmit: { backgroundColor: '#4F46E5', color: 'white', border: 'none', padding: '0.5rem 1.5rem', borderRadius: '6px', cursor: 'pointer', fontSize: '0.875rem', fontWeight: '500' }
};