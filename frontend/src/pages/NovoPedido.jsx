import React, { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Plus, Trash2, CheckCircle } from 'lucide-react';

export default function NovoPedido() {
  const navigate = useNavigate();
  const location = useLocation();

  // Recebe o cliente pré-selecionado caso venha do Funil de Negociações[cite: 4]
  const clienteDoFunil = location.state?.clientePreSelecionado || '';

  const [cliente, setCliente] = useState(clienteDoFunil);
  const [formaPagamento, setFormaPagamento] = useState('A_VISTA');
  
  const [itens, setItens] = useState([
    { id: 1, tecido: 'Crepe Georgette', cor: 'Off-white', metragem: 10, precoMetro: 28.90 }
  ]);

  const listaClientes = ['Ateliê Bella Noiva', 'Casa Têxtil Moreno', 'Studio Decor SP', 'Malhas Oliveira'];
  const catalogoTecidos = [
    { tecido: 'Crepe Georgette', cor: 'Off-white', preco: 28.90 },
    { tecido: 'Linho Natural', cor: 'Natural', preco: 42.50 },
    { tecido: 'Tule de Festa', cor: 'Marfim', preco: 35.00 }
  ];

  const handleAddItem = () => {
    setItens([...itens, { id: Date.now(), tecido: 'Linho Natural', cor: 'Natural', metragem: 5, precoMetro: 42.50 }]);
  };

  const handleRemoveItem = (id) => {
    setItens(itens.filter(item => item.id !== id));
  };

  const handleItemChange = (id, field, value) => {
    setItens(itens.map(item => {
      if (item.id === id) {
        const atualizado = { ...item, [field]: value };
        if (field === 'tecido') {
          const tecidoEncontrado = catalogoTecidos.find(t => t.tecido === value);
          if (tecidoEncontrado) {
            atualizado.precoMetro = tecidoEncontrado.preco;
            atualizado.cor = tecidoEncontrado.cor;
          }
        }
        return atualizado;
      }
      return item;
    }));
  };

  // Cálculo do total: metragem × preco_metro[cite: 4]
  const valorTotal = itens.reduce((acc, item) => acc + (Number(item.metragem) * Number(item.precoMetro)), 0);

  const handleSubmitPedido = (e) => {
    e.preventDefault();
    if (!cliente) {
      alert('Por favor, selecione um cliente.');
      return;
    }
    if (itens.length === 0) {
      alert('Adicione pelo menos um item ao pedido.');
      return;
    }

    alert(`Pedido criado com sucesso para ${cliente}!\nStatus inicial: EM_ABERTO\nValor Total: R$ ${valorTotal.toFixed(2)}`);
    navigate('/historico-vendas');
  };

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <h1 style={styles.title}>Novo Pedido</h1>
        <p style={styles.subtitle}>Preencha os dados para registrar uma nova venda</p>
      </div>

      <form onSubmit={handleSubmitPedido} style={styles.form}>
        
        {/* Cliente */}
        <div style={styles.card}>
          <h2 style={styles.cardTitle}>1. CLIENTE</h2>
          <div style={styles.inputGroup}>
            <label style={styles.label}>SELECIONE O CLIENTE *</label>
            <select 
              value={cliente} 
              onChange={(e) => setCliente(e.target.value)}
              style={styles.input}
              required
            >
              <option value="">-- Escolha um cliente --</option>
              {listaClientes.map((c, idx) => (
                <option key={idx} value={c}>{c}</option>
              ))}
            </select>
          </div>
        </div>

        {/* Itens */}
        <div style={styles.card}>
          <div style={styles.cardHeaderRow}>
            <h2 style={styles.cardTitle}>2. ITENS DO PEDIDO (TECIDOS)</h2>
            <button type="button" onClick={handleAddItem} style={styles.btnAddItem}>
              <Plus size={16} /> Adicionar Tecido
            </button>
          </div>

          <div style={styles.itemsList}>
            {itens.map((item) => {
              const subtotal = Number(item.metragem) * Number(item.precoMetro);
              return (
                <div key={item.id} style={styles.itemRow}>
                  <div style={{ flex: 2 }}>
                    <label style={styles.subLabel}>Tecido</label>
                    <select 
                      value={item.tecido} 
                      onChange={(e) => handleItemChange(item.id, 'tecido', e.target.value)}
                      style={styles.input}
                    >
                      {catalogoTecidos.map((t, i) => (
                        <option key={i} value={t.tecido}>{t.tecido}</option>
                      ))}
                    </select>
                  </div>

                  <div style={{ flex: 1 }}>
                    <label style={styles.subLabel}>Cor / Código</label>
                    <input 
                      type="text" 
                      value={item.cor} 
                      onChange={(e) => handleItemChange(item.id, 'cor', e.target.value)}
                      style={styles.input} 
                    />
                  </div>

                  <div style={{ flex: 1 }}>
                    <label style={styles.subLabel}>Metragem (m)</label>
                    <input 
                      type="number" 
                      step="0.1" 
                      min="0.1"
                      value={item.metragem} 
                      onChange={(e) => handleItemChange(item.id, 'metragem', e.target.value)}
                      style={styles.input} 
                    />
                  </div>

                  <div style={{ flex: 1 }}>
                    <label style={styles.subLabel}>Preço/m (R$)</label>
                    <input 
                      type="number" 
                      step="0.01" 
                      value={item.precoMetro} 
                      onChange={(e) => handleItemChange(item.id, 'precoMetro', e.target.value)}
                      style={styles.input} 
                    />
                  </div>

                  <div style={{ flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'flex-end' }}>
                    <span style={styles.subtotalText}>Subtotal: R$ {subtotal.toFixed(2)}</span>
                  </div>

                  {itens.length > 1 && (
                    <button type="button" onClick={() => handleRemoveItem(item.id)} style={styles.btnRemove}>
                      <Trash2 size={18} color="#DC2626" />
                    </button>
                  )}
                </div>
              );
            })}
          </div>
        </div>

        {/* Pagamento e Total */}
        <div style={styles.card}>
          <h2 style={styles.cardTitle}>3. PAGAMENTO E TOTAL</h2>
          
          <div style={styles.paymentRow}>
            <div style={{ flex: 1, ...styles.inputGroup }}>
              <label style={styles.label}>FORMA DE PAGAMENTO *</label>
              <select 
                value={formaPagamento} 
                onChange={(e) => setFormaPagamento(e.target.value)}
                style={styles.input}
              >
                <option value="A_VISTA">À Vista</option>
                <option value="CARTAO">Cartão</option>
                <option value="BOLETO">Boleto</option>
                <option value="CHEQUE">Cheque</option>
              </select>
            </div>

            <div style={styles.totalBox}>
              <span style={styles.totalLabel}>TOTAL DO PEDIDO:</span>
              <span style={styles.totalValue}>R$ {valorTotal.toFixed(2)}</span>
            </div>
          </div>
        </div>

        <div style={styles.footerActions}>
          <button type="submit" style={styles.btnSubmit}>
            <CheckCircle size={18} /> Finalizar e Criar Pedido
          </button>
        </div>

      </form>
    </div>
  );
}

const styles = {
  container: { fontFamily: 'sans-serif', paddingBottom: '3rem' },
  header: { marginBottom: '1.5rem' },
  title: { fontSize: '1.5rem', fontWeight: '600', color: '#111827', margin: '0 0 0.25rem 0' },
  subtitle: { fontSize: '0.875rem', color: '#6B7280', margin: 0 },
  form: { display: 'flex', flexDirection: 'column', gap: '1.5rem' },
  card: { backgroundColor: 'white', border: '1px solid #E5E7EB', borderRadius: '8px', padding: '1.5rem', boxShadow: '0 1px 2px rgba(0,0,0,0.05)' },
  cardTitle: { fontSize: '0.75rem', fontWeight: '600', color: '#9CA3AF', letterSpacing: '0.05em', marginBottom: '1rem', textTransform: 'uppercase' },
  cardHeaderRow: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' },
  inputGroup: { display: 'flex', flexDirection: 'column', gap: '0.5rem' },
  label: { fontSize: '0.75rem', fontWeight: '600', color: '#6B7280', letterSpacing: '0.05em' },
  subLabel: { fontSize: '0.7rem', fontWeight: '500', color: '#6B7280', marginBottom: '0.25rem', display: 'block' },
  input: { padding: '0.75rem', border: '1px solid #D1D5DB', borderRadius: '6px', fontSize: '0.875rem', outline: 'none', width: '100%', boxSizing: 'border-box', backgroundColor: 'white' },
  btnAddItem: { backgroundColor: '#EEF2FF', color: '#4F46E5', border: 'none', padding: '0.4rem 0.75rem', borderRadius: '6px', fontSize: '0.75rem', fontWeight: '600', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '0.35rem' },
  itemsList: { display: 'flex', flexDirection: 'column', gap: '1rem' },
  itemRow: { display: 'flex', gap: '1rem', alignItems: 'center', backgroundColor: '#F9FAFB', padding: '1rem', borderRadius: '6px', border: '1px solid #E5E7EB' },
  subtotalText: { fontSize: '0.875rem', fontWeight: '600', color: '#111827', paddingTop: '1.2rem' },
  btnRemove: { background: 'none', border: 'none', cursor: 'pointer', padding: '0.5rem', marginTop: '1rem' },
  paymentRow: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '2rem' },
  totalBox: { display: 'flex', flexDirection: 'column', alignItems: 'flex-end', backgroundColor: '#EEF2FF', padding: '1rem 1.5rem', borderRadius: '8px' },
  totalLabel: { fontSize: '0.75rem', fontWeight: '600', color: '#4F46E5' },
  totalValue: { fontSize: '1.5rem', fontWeight: '700', color: '#312E81' },
  footerActions: { display: 'flex', justifyContent: 'flex-end' },
  btnSubmit: { display: 'flex', alignItems: 'center', gap: '0.5rem', backgroundColor: '#10B981', color: 'white', border: 'none', padding: '0.75rem 1.5rem', borderRadius: '6px', fontSize: '1rem', fontWeight: '600', cursor: 'pointer' }
};