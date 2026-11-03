package br.com.lrrepresentacoes.connectcrm.dto;

/**
 * Usado pelo front antes de excluir a tag, para a confirmação
 * "Esta tag está em N clientes. Deseja excluir?".
 */
public record TagUsoResponse(Long id, String nome, long quantidadeClientes) {
}
