import { useEffect, useState } from 'react';
import { MessageSquare, X } from 'lucide-react';
import AlertaPedidoEmAberto from './AlertaPedidoEmAberto';
import AbaPedidos from './AbaPedidos';
import { buscarCliente, desativarCliente, reativarCliente } from '../services/clientes';
import { listarPedidos } from '../services/pedidos';
import { formatarDocumento, formatarTelefone } from '../utils/formatadores';

const ABAS = ['Dados', 'Interações', 'Follow-ups', 'Pedidos'];

// Resumo do banner "Pedido em aberto", calculado por consulta (regra 6.2): valor e quantidade dos EM_ABERTO;
// nº de compras e última compra consideram só os pedidos não cancelados.
function calcularResumo(pedidos) {
  const emAberto = pedidos.filter((p) => p.status === 'EM_ABERTO');
  if (emAberto.length === 0) return null;
  const compras = pedidos.filter((p) => p.status !== 'CANCELADO');
  const ultima = compras.map((p) => p.dataPedido).sort().at(-1);
  return {
    quantidade: emAberto.length,
    valorTotal: emAberto.reduce((soma, p) => soma + Number(p.total), 0),
    numeroCompras: compras.length,
    dataUltimaCompra: ultima,
  };
}

// A ficha lê o cliente da API ao abrir (o pai usa key={id} para recarregar ao trocar de cliente).
export default function FichaCliente({ idCliente, onClose, onEditar, onAlterado }) {
  const [abaAtiva, setAbaAtiva] = useState('Dados');
  const [cliente, setCliente] = useState(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState('');
  const [pedidos, setPedidos] = useState([]);
  const [carregandoPedidos, setCarregandoPedidos] = useState(true);
  const [erroPedidos, setErroPedidos] = useState('');

  useEffect(() => {
    let cancelado = false;
    buscarCliente(idCliente)
      .then((dados) => { if (!cancelado) setCliente(dados); })
      .catch((e) => { if (!cancelado) setErro(e.message); })
      .finally(() => { if (!cancelado) setCarregando(false); });
    listarPedidos({ idCliente })
      .then((dados) => { if (!cancelado) setPedidos(dados); })
      .catch((e) => { if (!cancelado) setErroPedidos(e.message); })
      .finally(() => { if (!cancelado) setCarregandoPedidos(false); });
    return () => { cancelado = true; };
  }, [idCliente]);

  // Depois de pagar ou cancelar: recarrega os pedidos (banner e aba) e avisa a lista de clientes.
  const recarregarPedidos = async () => {
    setPedidos(await listarPedidos({ idCliente }));
    onAlterado();
  };

  const handleWhatsApp = () => {
    // wa.me/<telefone> exatamente como gravado (já tem o DDI 55), sem acrescentar nada.
    const mensagem = encodeURIComponent(`Olá ${cliente.nome}, tudo bem?`);
    window.open(`https://wa.me/${cliente.telefone}?text=${mensagem}`, '_blank', 'noopener,noreferrer');
  };

  // Desativar pode ser recusado pela API (pedido em aberto ou negociação em andamento): mostra a mensagem dela.
  const alterarSituacao = async () => {
    setErro('');
    try {
      const atualizado = await (cliente.ativo ? desativarCliente(cliente.id) : reativarCliente(cliente.id));
      setCliente(atualizado);
      onAlterado();
    } catch (e) {
      setErro(e.message);
    }
  };

  return (
    <div style={styles.container}>
      {/* Botão de Fechar */}
      <button onClick={onClose} style={styles.closeBtn} title="Fechar Ficha" aria-label="Fechar ficha">
        <X size={20} color="#6B7280" />
      </button>

      {carregando && <p style={styles.placeholder}>Carregando ficha...</p>}
      {!carregando && !cliente && <p role="alert" style={styles.erro}>{erro}</p>}

      {cliente && (
        <>
          <AlertaPedidoEmAberto resumo={calcularResumo(pedidos)} />

          {/* Cabeçalho da Ficha */}
          <div style={styles.header}>
            <div>
              <h2 style={styles.nome}>{cliente.nome}</h2>
              <p style={styles.telefone}>{formatarTelefone(cliente.telefone)}</p>
              {!cliente.ativo && <span style={styles.inativoBadge}>Inativo</span>}
            </div>
            {/* Igual à lista: cliente inativo não recebe contato por WhatsApp. */}
            {cliente.ativo && (
              <button onClick={handleWhatsApp} style={styles.btnWhatsApp}>
                <MessageSquare size={16} />
                Enviar WhatsApp
              </button>
            )}
          </div>

          <div style={styles.acoes}>
            <button onClick={() => onEditar(cliente)} style={styles.btnAcao}>Editar</button>
            <button onClick={alterarSituacao} style={cliente.ativo ? styles.btnPerigo : styles.btnAcao}>
              {cliente.ativo ? 'Desativar' : 'Reativar'}
            </button>
          </div>
          {erro && <p role="alert" style={styles.erro}>{erro}</p>}

          {/* Navegação por Abas */}
          <div style={styles.tabsContainer}>
            {ABAS.map((aba) => (
              <button
                key={aba}
                onClick={() => setAbaAtiva(aba)}
                style={{
                  ...styles.tabBtn,
                  ...(abaAtiva === aba ? styles.tabAtiva : {}),
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
                  <span style={styles.dadoValor}>{cliente.cpfCnpj ? formatarDocumento(cliente.cpfCnpj) : 'Não informado'}</span>
                </div>
                <div style={styles.dadoItem}>
                  <span style={styles.dadoLabel}>CIDADE/UF</span>
                  <span style={styles.dadoValor}>
                    {[cliente.cidade, cliente.uf].filter(Boolean).join(' / ') || 'Não informado'}
                  </span>
                </div>
                <div style={styles.dadoItem}>
                  <span style={styles.dadoLabel}>TAGS</span>
                  <div style={styles.tagsContainer}>
                    {cliente.tags.length === 0 && <span style={styles.dadoValor}>Nenhuma tag</span>}
                    {cliente.tags.map((tag) => (
                      <span key={tag.id} style={styles.tagBadge}>{tag.nome}</span>
                    ))}
                  </div>
                </div>
              </div>
            )}

            {/* Placeholders das abas que entram na #46 */}
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
            {abaAtiva === 'Pedidos' && (
              <AbaPedidos
                pedidos={pedidos}
                carregando={carregandoPedidos}
                erroCarga={erroPedidos}
                onAlterado={recarregarPedidos}
              />
            )}
          </div>
        </>
      )}
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
  header: {
    display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1rem'
  },
  nome: { fontSize: '1.25rem', fontWeight: '600', color: '#111827', margin: '0 0 0.25rem 0' },
  telefone: { fontSize: '0.875rem', color: '#6B7280', margin: 0 },
  inativoBadge: { display: 'inline-block', marginTop: '0.5rem', padding: '0.125rem 0.5rem', borderRadius: '9999px', backgroundColor: '#F3F4F6', color: '#6B7280', fontSize: '0.75rem' },
  btnWhatsApp: {
    display: 'flex', alignItems: 'center', gap: '0.5rem', backgroundColor: '#10B981', color: 'white', border: 'none', padding: '0.5rem 0.75rem', borderRadius: '6px', fontSize: '0.875rem', cursor: 'pointer', fontWeight: '500'
  },
  acoes: { display: 'flex', gap: '0.5rem', marginBottom: '1rem' },
  btnPerigo: { backgroundColor: 'transparent', border: '1px solid #DC2626', color: '#DC2626', padding: '0.5rem 1rem', borderRadius: '6px', fontSize: '0.875rem', cursor: 'pointer', marginBottom: '1rem' },
  erro: { color: '#DC2626', fontSize: '0.8rem', margin: '0 0 1rem 0' },
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
