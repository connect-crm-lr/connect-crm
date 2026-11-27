import { chamarApi } from './api';

// Chamadas da API de estoque (/estoque). A linha de estoque é um tecido + uma cor, em metros.
export function listarEstoque({ busca, somenteBaixo = false } = {}) {
  const params = new URLSearchParams();
  if (busca) params.set('busca', busca);
  if (somenteBaixo) params.set('baixo', 'true');
  const query = params.toString();
  return chamarApi(`/estoque${query ? `?${query}` : ''}`);
}

// Registra a entrada; o back cria a linha tecido + cor se ela não existir e soma a metragem.
// "cor" é obrigatória (máx. 50) e "codigo" é opcional (máx. 20); o back normaliza a cor (preto -> Preto)
// e o código (maiúsculas). O texto de exibição vem pronto em "descricaoCor" (ex.: '05 Marinho' ou 'Marinho').
export function registrarEntrada({ idTecido, cor, codigo, metragem, dataEntrada }) {
  return chamarApi('/estoque/entradas', {
    method: 'POST',
    body: JSON.stringify({ idTecido, cor, codigo: codigo || null, metragem, dataEntrada }),
  });
}

// Tecidos ativos para o seletor do modal (a API só devolve inativos com ?inativos=true).
export function listarTecidosAtivos() {
  return chamarApi('/tecidos');
}
