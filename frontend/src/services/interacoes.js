import { chamarApi } from './api';

// Interações de um cliente (/clientes/{id}/interacoes): conversas já realizadas.
export function listarInteracoes(idCliente) {
  return chamarApi(`/clientes/${idCliente}/interacoes`);
}

// "dados": { tipo: WHATSAPP | LIGACAO | VISITA | OUTRO, dataInteracao: 'AAAA-MM-DD', descricao }
export function registrarInteracao(idCliente, dados) {
  return chamarApi(`/clientes/${idCliente}/interacoes`, { method: 'POST', body: JSON.stringify(dados) });
}
