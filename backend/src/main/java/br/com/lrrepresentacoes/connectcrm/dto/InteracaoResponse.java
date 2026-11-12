package br.com.lrrepresentacoes.connectcrm.dto;

import br.com.lrrepresentacoes.connectcrm.model.Interacao;
import br.com.lrrepresentacoes.connectcrm.model.TipoInteracao;

import java.time.LocalDate;

public record InteracaoResponse(
        Long id,
        Long idCliente,
        TipoInteracao tipo,
        LocalDate dataInteracao,
        String descricao
) {

    // Padrão Factory Method: único ponto de conversão da entidade Interacao para o DTO de resposta.
    public static InteracaoResponse de(Interacao interacao) {
        return new InteracaoResponse(interacao.getId(), interacao.getCliente().getId(), interacao.getTipo(),
                interacao.getDataInteracao(), interacao.getDescricao());
    }
}
