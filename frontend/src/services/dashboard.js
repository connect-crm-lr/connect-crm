import { chamarApi } from './api';

// Painel inicial (RF-021): tudo é calculado pela API na hora da consulta.
// Resposta: { vendasMes, pedidosMes, negociacoesPorEtapa, pendenciasHoje, estoqueBaixo }.
export function buscarDashboard() {
  return chamarApi('/dashboard');
}

// Concluir uma pendência: a API preenche a data de conclusão com a data/hora atual (regra 6.4).
export function concluirPendencia(idFollowUp) {
  return chamarApi(`/follow-ups/${idFollowUp}/concluir`, { method: 'PATCH' });
}
