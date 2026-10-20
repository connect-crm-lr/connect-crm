import React, { useState } from 'react';
import { Search, Plus, X } from 'lucide-react';

export default function Estoque() {
  const [searchTerm, setSearchTerm] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);

  // Dados simulados baseados no protótipo e na regra de metragem
  const [estoqueData, setEstoqueData] = useState([
    { id: 1, tecido: 'Crepe Georgette', cor: 'Off-white', metragem: 245, pecas: 3 },
    { id: 2, tecido: 'Crepe Georgette', cor: 'Rose', metragem: 180, pecas: 2 },
    { id: 3, tecido: 'Linho Natural', cor: 'Natural', metragem: 15, pecas: 1 }, // < 20 = BAIXO
    { id: 4, tecido: 'Suede Automotivo', cor: 'Cinza Chumbo', metragem: 320, pecas: 4 },
    { id: 5, tecido: 'Suede Automotivo', cor: 'Bege', metragem: 140, pecas: 2 },
    { id: 6, tecido: 'Malha Canelada', cor: 'Cru', metragem: 210, pecas: 3 },
    { id: 7, tecido: 'Tule de Festa', cor: 'Marfim', metragem: 8.5, pecas: 1 }, // < 20 = BAIXO
  ]);

  const linhasFiltradas = estoqueData.filter(item => 
    item.tecido.toLowerCase().includes(searchTerm.toLowerCase()) ||
    item.cor.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div style={styles.container}>
      {/* Cabeçalho */}
      <div style={styles.header}>
        <div>
          <h1 style={styles.title}>Estoque</h1>
          <p style={styles.subtitle}>{estoqueData.length} linhas de estoque</p>
        </div>
        <button onClick={() => setIsModalOpen(true)} style={styles.btnPrimary}>
          <Plus size={18} />
          Registrar entrada
        </button>
      </div>

      {/* Barra de Pesquisa */}
      <div style={styles.toolbar}>
        <div style={styles.searchBox}>
          <Search size={18} color="#9CA3AF" style={styles.searchIcon} />
          <input
            type="text"
            placeholder="Buscar por nome do tecido ou cor..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            style={styles.searchInput}
          />
        </div>
      </div>

      {/* Tabela de Estoque */}
      <div style={styles.tableContainer}>
        <table style={styles.table}>
          <thead>
            <tr>
              <th style={styles.th}>TECIDO</th>
              <th style={styles.th}>COR / CÓDIGO</th>
              <th style={styles.th}>METRAGEM DISPONÍVEL</th>
              <th style={styles.th}>PEÇAS (ROLOS)</th>
            </tr>
          </thead>
          <tbody>
            {linhasFiltradas.map((item) => {
              // Regra de Estoque Baixo: < 20 metros[cite: 5]
              const isEstoqueBaixo = item.metragem < 20; 
              
              return (
                <tr key={item.id} style={{
                  ...styles.tr,
                  backgroundColor: isEstoqueBaixo ? '#FFFBEB' : 'white' // Fundo amarelado para alerta visual
                }}>
                  <td style={styles.td}>{item.tecido}</td>
                  <td style={styles.td}>{item.cor}</td>
                  <td style={styles.td}>
                    {item.metragem} m
                    {isEstoqueBaixo && <span style={styles.badgeBaixo}>BAIXO</span>}
                  </td>
                  <td style={styles.td}>{item.pecas}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
        {linhasFiltradas.length === 0 && (
          <p style={styles.emptyState}>Nenhum item encontrado.</p>
        )}
      </div>

      {/* Modal de Nova Entrada */}
      {isModalOpen && (
        <ModalNovaEntrada 
          onClose={() => setIsModalOpen(false)} 
          onSave={(novaEntrada) => {
            // Mockup: Adiciona visualmente (na API real a lógica "Registrar entrada de estoque" procuraria a linha existente e somaria)[cite: 5]
            setEstoqueData([{...novaEntrada, id: Date.now()}, ...estoqueData]);
            setIsModalOpen(false);
          }} 
        />
      )}
    </div>
  );
}

// Componente do Modal
function ModalNovaEntrada({ onClose, onSave }) {
  // O modal pede tecido, cor, metragem[cite: 5]
  const [tecido, setTecido] = useState('');
  const [cor, setCor] = useState('');
  const [metragem, setMetragem] = useState('');
  const [pecas, setPecas] = useState('1'); // Mantido do protótipo

  const handleSubmit = (e) => {
    e.preventDefault();
    if(tecido && cor && metragem) {
      onSave({
        tecido,
        cor,
        metragem: parseFloat(metragem),
        pecas: parseInt(pecas, 10)
      });
    }
  };

  return (
    <div style={styles.modalOverlay}>
      <div style={styles.modalCard}>
        <div style={styles.modalHeader}>
          <h2 style={styles.modalTitle}>Registrar entrada de estoque</h2>
          <button onClick={onClose} style={styles.closeBtn}><X size={20} color="#6B7280" /></button>
        </div>

        <form onSubmit={handleSubmit} style={styles.form}>
          <div style={styles.inputGroup}>
            <label style={styles.label}>TECIDO</label>
            <select 
              value={tecido} 
              onChange={(e) => setTecido(e.target.value)}
              style={styles.input}
              required
            >
              <option value="">Selecione o tecido</option>
              <option value="Crepe Georgette">Crepe Georgette</option>
              <option value="Linho Natural">Linho Natural</option>
              <option value="Tule de Festa">Tule de Festa</option>
            </select>
          </div>

          <div style={styles.inputGroup}>
            <label style={styles.label}>COR / CÓDIGO</label>
            <input 
              type="text" 
              placeholder="Ex: Off-white, #CR-001" 
              value={cor}
              onChange={(e) => setCor(e.target.value)}
              style={styles.input}
              required
            />
          </div>

          <div style={{ display: 'flex', gap: '1rem' }}>
            <div style={{...styles.inputGroup, flex: 1}}>
              <label style={styles.label}>METRAGEM (M)</label>
              <input 
                type="number" 
                step="0.01"
                placeholder="0,00" 
                value={metragem}
                onChange={(e) => setMetragem(e.target.value)}
                style={styles.input}
                required
              />
            </div>
            <div style={{...styles.inputGroup, flex: 1}}>
              <label style={styles.label}>Nº DE PEÇAS</label>
              <input 
                type="number" 
                min="1"
                value={pecas}
                onChange={(e) => setPecas(e.target.value)}
                style={styles.input}
              />
            </div>
          </div>

          <div style={styles.modalFooter}>
            <button type="button" onClick={onClose} style={styles.btnCancel}>Cancelar</button>
            <button type="submit" style={styles.btnSubmit}>Registrar</button>
          </div>
        </form>
      </div>
    </div>
  );
}

// Estilos In-line
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
  tableContainer: { backgroundColor: 'white', border: '1px solid #E5E7EB', borderRadius: '8px', overflow: 'hidden' },
  table: { width: '100%', borderCollapse: 'collapse', textAlign: 'left' },
  th: { padding: '1rem', borderBottom: '1px solid #E5E7EB', backgroundColor: '#F9FAFB', fontSize: '0.75rem', fontWeight: '600', color: '#6B7280', letterSpacing: '0.05em' },
  tr: { borderBottom: '1px solid #E5E7EB' },
  td: { padding: '1rem', fontSize: '0.875rem', color: '#111827' },
  badgeBaixo: { backgroundColor: '#FEF3C7', color: '#D97706', padding: '0.15rem 0.4rem', borderRadius: '4px', fontSize: '0.65rem', fontWeight: '600', marginLeft: '0.5rem' },
  emptyState: { textAlign: 'center', padding: '2rem', color: '#6B7280', fontSize: '0.875rem' },
  
  // Estilos do Modal
  modalOverlay: { position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 50 },
  modalCard: { backgroundColor: 'white', borderRadius: '8px', width: '100%', maxWidth: '500px', padding: '1.5rem', boxShadow: '0 4px 6px rgba(0,0,0,0.1)' },
  modalHeader: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' },
  modalTitle: { fontSize: '1.25rem', fontWeight: '600', color: '#111827', margin: 0 },
  closeBtn: { background: 'none', border: 'none', cursor: 'pointer', padding: 0 },
  form: { display: 'flex', flexDirection: 'column', gap: '1.25rem' },
  inputGroup: { display: 'flex', flexDirection: 'column', gap: '0.5rem' },
  label: { fontSize: '0.75rem', fontWeight: '600', color: '#6B7280', letterSpacing: '0.05em' },
  input: { padding: '0.75rem', border: '1px solid #D1D5DB', borderRadius: '6px', fontSize: '0.875rem', outline: 'none', width: '100%', boxSizing: 'border-box' },
  modalFooter: { display: 'flex', justifyContent: 'flex-end', gap: '1rem', marginTop: '1rem' },
  btnCancel: { backgroundColor: 'white', border: '1px solid #D1D5DB', color: '#374151', padding: '0.5rem 1rem', borderRadius: '6px', cursor: 'pointer', fontSize: '0.875rem', fontWeight: '500' },
  btnSubmit: { backgroundColor: '#A78BFA', color: 'white', border: 'none', padding: '0.5rem 1.5rem', borderRadius: '6px', cursor: 'pointer', fontSize: '0.875rem', fontWeight: '500' } // Cor roxa baseada no botão de registrar do protótipo
};