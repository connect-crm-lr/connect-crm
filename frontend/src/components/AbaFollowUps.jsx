import { useEffect, useState } from 'react';
import { X } from 'lucide-react';
import { listarFollowUps, criarFollowUp, concluirFollowUp } from '../services/followUps';
import { formatarData, formatarDataHora, hojeLocal } from '../utils/formatadores';
import { estilosFicha as s } from '../utils/estilosFicha';

// Aba Follow-ups da ficha: próximas ações a fazer (lista + criar + concluir).
// Pendente = sem data de conclusão. Concluir é feito pela API, que grava a data/hora atual (regra 6.4).
export default function AbaFollowUps({ idCliente }) {
  const [followUps, setFollowUps] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState('');
  const [modalAberto, setModalAberto] = useState(false);
  const [concluindoId, setConcluindoId] = useState(null);

  useEffect(() => {
    let cancelado = false;
    listarFollowUps(idCliente)
      .then((dados) => { if (!cancelado) setFollowUps(dados); })
      .catch((e) => { if (!cancelado) setErro(e.message); })
      .finally(() => { if (!cancelado) setCarregando(false); });
    return () => { cancelado = true; };
  }, [idCliente]);

  const concluir = async (followUp) => {
    // Duplo clique mandaria 2 PATCH e o 2º voltaria erro ("já foi concluído"): ignora enquanto um está em andamento.
    if (concluindoId) return;
    setConcluindoId(followUp.id);
    setErro('');
    try {
      const atualizado = await concluirFollowUp(followUp.id);
      setFollowUps((atual) => atual.map((f) => (f.id === atualizado.id ? atualizado : f)));
    } catch (e) {
      setErro(e.message);
    } finally {
      setConcluindoId(null);
    }
  };

  const pendentes = followUps
    .filter((f) => !f.dataConclusao)
    .sort((a, b) => a.dataPrevista.localeCompare(b.dataPrevista) || a.id - b.id);
  const concluidos = followUps
    .filter((f) => f.dataConclusao)
    .sort((a, b) => b.dataConclusao.localeCompare(a.dataConclusao));
  const hoje = hojeLocal();

  return (
    <div>
      <button style={s.btnAcao} onClick={() => setModalAberto(true)}>+ Novo Follow-up</button>
      {erro && <p role="alert" style={s.erro}>{erro}</p>}
      {carregando && <p style={s.placeholder}>Carregando follow-ups...</p>}
      {!carregando && !erro && followUps.length === 0 && <p style={s.placeholder}>Nenhum follow-up registrado.</p>}
      {!carregando && pendentes.length === 0 && concluidos.length > 0 && <p style={s.placeholder}>Nenhuma pendência.</p>}

      {pendentes.length > 0 && <p style={s.secao}>PENDENTES</p>}
      <div style={s.lista}>
        {pendentes.map((f) => (
          <div key={f.id} style={s.item}>
            <div style={s.itemTopo}>
              <span style={s.itemData}>
                Previsto para {formatarData(f.dataPrevista)}
              </span>
              {/* Pendências de hoje = prevista até hoje (AGENTS 6.4) */}
              {f.dataPrevista <= hoje && (
                <span style={s.badgeAtraso}>{f.dataPrevista === hoje ? 'HOJE' : 'ATRASADO'}</span>
              )}
            </div>
            <p style={s.itemTexto}>{f.descricao}</p>
            <div style={{ marginTop: '0.5rem' }}>
              <button style={s.btnConcluir} disabled={concluindoId === f.id} onClick={() => concluir(f)}>
                {concluindoId === f.id ? 'Concluindo...' : 'Concluir'}
              </button>
            </div>
          </div>
        ))}
      </div>

      {concluidos.length > 0 && <p style={s.secao}>CONCLUÍDOS</p>}
      <div style={s.lista}>
        {concluidos.map((f) => (
          <div key={f.id} style={{ ...s.item, opacity: 0.7 }}>
            <div style={s.itemTopo}>
              <span style={s.itemData}>Previsto para {formatarData(f.dataPrevista)}</span>
              <span style={s.itemData}>Concluído em {formatarDataHora(f.dataConclusao)}</span>
            </div>
            <p style={s.itemTexto}>{f.descricao}</p>
          </div>
        ))}
      </div>

      {modalAberto && (
        <ModalFollowUp
          idCliente={idCliente}
          onClose={() => setModalAberto(false)}
          onSaved={(novo) => {
            setModalAberto(false);
            setFollowUps((atual) => [...atual, novo]);
          }}
        />
      )}
    </div>
  );
}

// Modal Novo Follow-up: data prevista e descrição da próxima ação. Erros aparecem abaixo do campo.
function ModalFollowUp({ idCliente, onClose, onSaved }) {
  const [data, setData] = useState(hojeLocal);
  const [descricao, setDescricao] = useState('');
  const [erros, setErros] = useState({});
  const [erroGeral, setErroGeral] = useState('');
  const [salvando, setSalvando] = useState(false);

  const limparErro = (campo) => setErros((atual) => ({ ...atual, [campo]: undefined }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErroGeral('');

    const novosErros = {};
    if (!data) novosErros.dataPrevista = 'A data prevista é obrigatória';
    if (!descricao.trim()) novosErros.descricao = 'A descrição é obrigatória';
    if (Object.keys(novosErros).length > 0) {
      setErros(novosErros);
      return;
    }

    setSalvando(true);
    try {
      onSaved(await criarFollowUp(idCliente, { dataPrevista: data, descricao: descricao.trim() }));
    } catch (err) {
      // "campos" da API: cada erro de validação vai abaixo do campo correspondente.
      const campos = err.detalhes?.campos ?? [];
      if (campos.length > 0) {
        setErros(Object.fromEntries(campos.map((c) => [c.campo, c.mensagem])));
      } else {
        setErroGeral(err.message);
      }
    } finally {
      setSalvando(false);
    }
  };

  const campoErro = (nome) => erros[nome] && <p role="alert" style={s.fieldError}>{erros[nome]}</p>;

  return (
    <div style={s.modalOverlay}>
      <div style={s.modalCard}>
        <div style={s.modalHeader}>
          <h2 style={s.modalTitle}>Novo Follow-up</h2>
          <button type="button" onClick={onClose} aria-label="Fechar" style={s.closeBtn}><X size={20} color="#6B7280" /></button>
        </div>

        <form onSubmit={handleSubmit} noValidate style={s.form}>
          <div style={s.inputGroup}>
            <label style={s.label}>DATA PREVISTA *</label>
            <input
              type="date" value={data}
              onChange={(e) => { setData(e.target.value); limparErro('dataPrevista'); }}
              style={s.input}
            />
            {campoErro('dataPrevista')}
          </div>

          <div style={s.inputGroup}>
            <label style={s.label}>DESCRIÇÃO *</label>
            <textarea
              rows={4} placeholder="Ex: Ligar para confirmar o mostruário"
              value={descricao}
              onChange={(e) => { setDescricao(e.target.value); limparErro('descricao'); }}
              style={s.input}
            />
            {campoErro('descricao')}
          </div>

          {erroGeral && <p role="alert" style={s.fieldError}>{erroGeral}</p>}

          <div style={s.modalFooter}>
            <button type="button" onClick={onClose} style={s.btnCancel}>Cancelar</button>
            <button type="submit" disabled={salvando} style={s.btnSubmit}>{salvando ? 'Salvando...' : 'Salvar'}</button>
          </div>
        </form>
      </div>
    </div>
  );
}
