import { chamarApi } from './api';

// Chamadas da API de clientes (/clientes). Cliente nunca é apagado: só desativado (AGENTS 6.5).
export function listarClientes({ busca, idTag, incluirInativos = false } = {}) {
  const params = new URLSearchParams();
  if (busca) params.set('busca', busca);
  if (idTag) params.set('tag', String(idTag));
  if (incluirInativos) params.set('inativos', 'true');
  const query = params.toString();
  return chamarApi(`/clientes${query ? `?${query}` : ''}`);
}

export function buscarCliente(id) {
  return chamarApi(`/clientes/${id}`);
}

// "dados" segue o ClienteRequest; as tags vão pelo nome e o back cria as que ainda não existem.
export function criarCliente(dados) {
  return chamarApi('/clientes', { method: 'POST', body: JSON.stringify(dados) });
}

export function atualizarCliente(id, dados) {
  return chamarApi(`/clientes/${id}`, { method: 'PUT', body: JSON.stringify(dados) });
}

export function desativarCliente(id) {
  return chamarApi(`/clientes/${id}/desativar`, { method: 'PATCH' });
}

export function reativarCliente(id) {
  return chamarApi(`/clientes/${id}/reativar`, { method: 'PATCH' });
}
