import { useCallback, useEffect, useState } from 'react';
import { buscarDashboard, concluirPendencia } from '../services/dashboard';

// Etapas do funil na ordem do enum (AGENTS 5.3), com o texto de exibição e as cores do protótipo.
const ETAPAS = [
  { chave: 'PRIMEIRO_CONTATO', texto: 'Primeiro contato', color: '#DBEAFE', textColor: '#1E40AF' },
  { chave: 'MOSTRUARIO_ENVIADO', texto: 'Mostruário enviado', color: '#E0F2FE', textColor: '#0369A1' },
  { chave: 'AGUARDANDO_RESPOSTA', texto: 'Aguardando resposta', color: '#FFEDD5', textColor: '#C2410C' },
  { chave: 'FECHADA', texto: 'Fechada', color: '#F97316', textColor: 'white' },
  { chave: 'PERDIDA', texto: 'Perdida', color: '#F3F4F6', textColor: '#6B7280' },
];

const plural = (n, singular, pluralTexto) => `${n} ${n === 1 ? singular : pluralTexto}`;
const formatarDinheiro = (valor) => Number(valor).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
const formatarMetragem = (valor) =>
  `${Number(valor).toLocaleString('pt-BR', { minimumFractionDigits: 0, maximumFractionDigits: 2 })} m`;

// 'AAAA-MM-DD' -> 'dd/mm/aaaa', sem Date (o fuso não pode mudar o dia).
function formatarData(dataIso) {
  const [ano, mes, dia] = String(dataIso).slice(0, 10).split('-');
  return `${dia}/${mes}/${ano}`;
}

// Data de hoje (AAAA-MM-DD) no fuso local.
function hojeLocal() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

const primeiraMaiuscula = (texto) => texto.charAt(0).toUpperCase() + texto.slice(1);

export default function Dashboard() {
  const [dados, setDados] = useState(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState('');
  const [concluindoId, setConcluindoId] = useState(null);
  const [erroPendencia, setErroPendencia] = useState('');

  const carregar = useCallback(async () => {
    setErro('');
    try {
      setDados(await buscarDashboard());
    } catch (e) {
      setErro(e.message);
    } finally {
      setCarregando(false);
    }
  }, []);

  useEffect(() => {
    let cancelado = false;
    buscarDashboard()
      .then((resposta) => { if (!cancelado) setDados(resposta); })
      .catch((e) => { if (!cancelado) setErro(e.message); })
      .finally(() => { if (!cancelado) setCarregando(false); });
    return () => { cancelado = true; };
  }, []);

  // Concluir uma pendência: ignora cliques enquanto uma conclusão está em andamento e recarrega o painel.
  const concluir = async (pendencia) => {
    if (concluindoId) return;
    setConcluindoId(pendencia.id);
    setErroPendencia('');
    try {
      await concluirPendencia(pendencia.id);
      await carregar();
    } catch (e) {
      setErroPendencia(e.message);
    } finally {
      setConcluindoId(null);
    }
  };

  const agora = new Date();
  const dataAtual = primeiraMaiuscula(
    agora.toLocaleDateString('pt-BR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }),
  );
  const mesReferencia = primeiraMaiuscula(agora.toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' }));

  const hoje = hojeLocal();
  const totalNegociacoes = dados
    ? ETAPAS.reduce((soma, e) => soma + Number(dados.negociacoesPorEtapa?.[e.chave] ?? 0), 0)
    : 0;

  return (
    <div style={styles.container}>
      {/* Cabeçalho */}
      <div style={styles.header}>
        <h1 style={styles.title}>Dashboard</h1>
        <p style={styles.date}>{dataAtual}</p>
      </div>

      {erro && <div role="alert" style={styles.alertError}>{erro}</div>}
      {carregando && <p style={styles.estado}>Carregando painel...</p>}

      {dados && (
        <div style={styles.grid}>

          {/* Cartão 1: Vendas do Mês (pedidos cancelados ficam de fora) */}
          <div style={styles.card}>
            <h2 style={styles.cardTitle}>VENDAS DO MÊS</h2>
            <p style={styles.salesValue}>{formatarDinheiro(dados.vendasMes)}</p>
            <p style={styles.salesMonth}>
              {mesReferencia} · {plural(dados.pedidosMes, 'pedido', 'pedidos')}
            </p>
          </div>

          {/* Cartão 2: Negociações por Etapa (as 5 etapas, inclusive as zeradas) */}
          <div style={styles.card}>
            <h2 style={styles.cardTitle}>NEGOCIAÇÕES POR ETAPA</h2>
            <div style={styles.list}>
              {ETAPAS.map((etapa) => (
                <div key={etapa.chave} style={styles.listItem}>
                  <span style={{ ...styles.badge, backgroundColor: etapa.color, color: etapa.textColor }}>
                    {etapa.texto}
                  </span>
                  <span style={styles.count}>{Number(dados.negociacoesPorEtapa?.[etapa.chave] ?? 0)}</span>
                </div>
              ))}
            </div>
            <p style={styles.rodape}>{plural(totalNegociacoes, 'negociação', 'negociações')}</p>
          </div>

          {/* Cartão 3: Pendências de Hoje (follow-ups de hoje ou atrasados, só de cliente ativo) */}
          <div style={styles.card}>
            <h2 style={styles.cardTitle}>PENDÊNCIAS DE HOJE</h2>
            {erroPendencia && <p role="alert" style={styles.erroPendencia}>{erroPendencia}</p>}
            <div style={styles.list}>
              {dados.pendenciasHoje.length === 0 && <p style={styles.estado}>Nenhuma pendência para hoje.</p>}
              {dados.pendenciasHoje.map((item) => (
                <div key={item.id} style={styles.pendenciaItem}>
                  <div>
                    <p style={styles.clienteName}>{item.cliente}</p>
                    <p style={styles.descricao}>{item.descricao}</p>
                    <p style={styles.dataPrevista}>
                      Previsto para {formatarData(item.dataPrevista)}
                      {item.dataPrevista < hoje && <span style={styles.atrasado}> · ATRASADO</span>}
                    </p>
                  </div>
                  <button
                    style={styles.btnConcluir}
                    disabled={concluindoId === item.id}
                    onClick={() => concluir(item)}
                  >
                    {concluindoId === item.id ? 'Concluindo...' : 'Concluir'}
                  </button>
                </div>
              ))}
            </div>
            {dados.pendenciasHoje.length > 0 && (
              <p style={styles.rodape}>{plural(dados.pendenciasHoje.length, 'pendência', 'pendências')}</p>
            )}
          </div>

          {/* Cartão 4: Tecidos com Estoque Baixo (menos de 20 m) */}
          <div style={styles.card}>
            <h2 style={styles.cardTitle}>TECIDOS COM ESTOQUE BAIXO</h2>
            <div style={styles.list}>
              {dados.estoqueBaixo.length === 0 && <p style={styles.estado}>Nenhum tecido com estoque baixo.</p>}
              {dados.estoqueBaixo.map((item) => (
                <div key={item.id} style={styles.estoqueItem}>
                  <div>
                    <p style={styles.tecidoName}>{item.tecido}</p>
                    <p style={styles.corName}>{item.descricaoCor}</p>
                  </div>
                  <div style={styles.metragemBadge}>
                    {formatarMetragem(item.metragem)}
                  </div>
                </div>
              ))}
            </div>
            {dados.estoqueBaixo.length > 0 && (
              <p style={styles.rodape}>{plural(dados.estoqueBaixo.length, 'linha de estoque', 'linhas de estoque')}</p>
            )}
          </div>

        </div>
      )}
    </div>
  );
}

// Estilos in-line
const styles = {
  container: { fontFamily: 'sans-serif' },
  header: { marginBottom: '2rem' },
  title: { fontSize: '1.5rem', fontWeight: '600', color: '#111827', margin: '0 0 0.5rem 0' },
  date: { fontSize: '0.875rem', color: '#6B7280', margin: 0 },
  grid: { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))', gap: '1.5rem' },
  card: { backgroundColor: 'white', borderRadius: '8px', padding: '1.5rem', boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.1)' },
  cardTitle: { fontSize: '0.75rem', fontWeight: '600', color: '#9CA3AF', letterSpacing: '0.05em', marginBottom: '1.5rem', textTransform: 'uppercase' },
  salesValue: { fontSize: '2.25rem', fontWeight: '700', color: '#111827', margin: '0 0 0.5rem 0' },
  salesMonth: { fontSize: '0.875rem', color: '#6B7280', margin: 0 },
  list: { display: 'flex', flexDirection: 'column', gap: '1rem' },
  listItem: { display: 'flex', justifyContent: 'space-between', alignItems: 'center' },
  badge: { padding: '0.25rem 0.75rem', borderRadius: '9999px', fontSize: '0.75rem', fontWeight: '500' },
  count: { fontSize: '0.875rem', color: '#374151', fontWeight: '500' },
  pendenciaItem: { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '1rem', borderBottom: '1px solid #F3F4F6', paddingBottom: '1rem' },
  clienteName: { fontSize: '0.875rem', fontWeight: '500', color: '#111827', margin: '0 0 0.25rem 0' },
  descricao: { fontSize: '0.875rem', color: '#6B7280', margin: 0 },
  dataPrevista: { fontSize: '0.75rem', color: '#9CA3AF', margin: '0.25rem 0 0 0' },
  atrasado: { color: '#D97706', fontWeight: '600' },
  btnConcluir: { backgroundColor: 'transparent', border: '1px solid #10B981', color: '#10B981', padding: '0.25rem 0.75rem', borderRadius: '4px', fontSize: '0.75rem', fontWeight: '500', cursor: 'pointer', whiteSpace: 'nowrap' },
  estoqueItem: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid #F3F4F6', paddingBottom: '1rem' },
  tecidoName: { fontSize: '0.875rem', fontWeight: '500', color: '#111827', margin: '0 0 0.25rem 0' },
  corName: { fontSize: '0.875rem', color: '#6B7280', margin: 0 },
  metragemBadge: { backgroundColor: '#FEF3C7', color: '#D97706', padding: '0.25rem 0.75rem', borderRadius: '9999px', fontSize: '0.75rem', fontWeight: '600' },
  rodape: { fontSize: '0.75rem', color: '#9CA3AF', margin: '1rem 0 0 0' },
  estado: { fontSize: '0.875rem', color: '#6B7280', margin: 0 },
  erroPendencia: { color: '#DC2626', fontSize: '0.8rem', margin: '0 0 1rem 0' },
  alertError: { backgroundColor: '#FEF2F2', border: '1px solid #FECACA', color: '#B91C1C', padding: '0.75rem 1rem', borderRadius: '6px', fontSize: '0.875rem', marginBottom: '1rem' },
};
