import { chamarApi } from './api';

// Follow-ups: próxima ação a fazer, com data. Pertencem a um cliente.
export function listarFollowUps(idCliente) {
  return chamarApi(`/clientes/${idCliente}/follow-ups`);
}

// "dados": { dataPrevista: 'AAAA-MM-DD', descricao }. O follow-up sempre nasce pendente.
export function criarFollowUp(idCliente, dados) {
  return chamarApi(`/clientes/${idCliente}/follow-ups`, { method: 'POST', body: JSON.stringify(dados) });
}

// Concluir preenche a data de conclusão com a data/hora atual (regra 6.4), feito pela API.
export function concluirFollowUp(id) {
  return chamarApi(`/follow-ups/${id}/concluir`, { method: 'PATCH' });
}
