import { chamarApi } from './api';

// Chamadas da API de tags (/tags). Criar a tag na hora é feito pelo cadastro do cliente (por nome).
export function listarTags() {
  return chamarApi('/tags');
}

// Quantos clientes usam a tag; usado na confirmação antes de excluir.
export function consultarUsoTag(id) {
  return chamarApi(`/tags/${id}/uso`);
}

// A exclusão remove os vínculos da tag com os clientes (regra 6.4).
export function excluirTag(id) {
  return chamarApi(`/tags/${id}`, { method: 'DELETE' });
}
