package br.com.lrrepresentacoes.connectcrm.dto;

import br.com.lrrepresentacoes.connectcrm.model.EstagioNegociacao;
import br.com.lrrepresentacoes.connectcrm.model.Negociacao;

import java.time.LocalDateTime;

/**
 * Card do funil. O id e o nome do cliente vão juntos porque, ao mover para FECHADA,
 * o front abre o Novo Pedido com o cliente da negociação já definido (regra 6.3).
 */
public record NegociacaoResponse(
        Long id,
        Long idCliente,
        String cliente,
        String descricao,
        EstagioNegociacao estagio,
        LocalDateTime dataCriacao,
        LocalDateTime dataUltimaMovimentacao,
        LocalDateTime dataFechamento
) {

    // Padrão Factory Method: único ponto de conversão da entidade Negociacao para o DTO de resposta.
    public static NegociacaoResponse de(Negociacao negociacao) {
        return new NegociacaoResponse(negociacao.getId(), negociacao.getCliente().getId(),
                negociacao.getCliente().getNome(), negociacao.getDescricao(), negociacao.getEstagio(),
                negociacao.getDataCriacao(), negociacao.getDataUltimaMovimentacao(),
                negociacao.getDataFechamento());
    }
}
