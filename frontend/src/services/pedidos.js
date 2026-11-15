import { chamarApi } from './api';

// Chamadas da API de pedidos (/pedidos). Não existe DELETE: cancelar muda o status e devolve o estoque (regra 6.1).
export function listarPedidos({ idCliente, status } = {}) {
  const params = new URLSearchParams();
  if (idCliente) params.set('clienteId', String(idCliente));
  if (status) params.set('status', status);
  const query = params.toString();
  return chamarApi(`/pedidos${query ? `?${query}` : ''}`);
}

// Confirmar pedido: o back valida cliente/tecidos ativos e o saldo, grava pedido e itens e desconta o estoque.
// "dados": { idCliente, idNegociacao?, formaPagamento, pagoNoAto, itens: [{ idEstoque, metragem }] }.
// O total e o preço dos itens são calculados pelo back (regra 6.2).
export function criarPedido(dados) {
  return chamarApi('/pedidos', { method: 'POST', body: JSON.stringify(dados) });
}

// Dados do formulário de Novo Pedido: linhas de estoque (tecido + cor, só de tecido ativo) e o preço por metro
// de cada tecido (o item grava o preço da época da venda, regra 6.2).
export function listarEstoqueParaPedido() {
  return chamarApi('/estoque');
}

export function listarTecidosParaPedido() {
  return chamarApi('/tecidos');
}

export function pagarPedido(id) {
  return chamarApi(`/pedidos/${id}/pagar`, { method: 'PATCH' });
}

export function cancelarPedido(id) {
  return chamarApi(`/pedidos/${id}/cancelar`, { method: 'PATCH' });
}
