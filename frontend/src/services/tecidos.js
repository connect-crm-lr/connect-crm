import { chamarApi } from './api';

// Chamadas da API de catálogo (/tecidos). Não existe DELETE: "excluir" desativa (AGENTS 6.5).
export function listarTecidos({ busca, incluirInativos = false } = {}) {
  const params = new URLSearchParams();
  if (busca) params.set('busca', busca);
  if (incluirInativos) params.set('inativos', 'true');
  const query = params.toString();
  return chamarApi(`/tecidos${query ? `?${query}` : ''}`);
}

export function criarTecido(dados) {
  return chamarApi('/tecidos', { method: 'POST', body: JSON.stringify(dados) });
}

export function atualizarTecido(id, dados) {
  return chamarApi(`/tecidos/${id}`, { method: 'PUT', body: JSON.stringify(dados) });
}

export function desativarTecido(id) {
  return chamarApi(`/tecidos/${id}/desativar`, { method: 'PATCH' });
}

export function reativarTecido(id) {
  return chamarApi(`/tecidos/${id}/reativar`, { method: 'PATCH' });
}
