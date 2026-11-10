package br.com.lrrepresentacoes.connectcrm.dto;

import br.com.lrrepresentacoes.connectcrm.model.Tecido;

import java.math.BigDecimal;

public record TecidoResponse(
        Long id,
        String nome,
        String composicao,
        Integer gramatura,
        BigDecimal largura,
        BigDecimal precoMetro,
        String fotoUrl,
        boolean ativo
) {

    // Padrão Factory Method: único ponto de conversão da entidade Tecido para o DTO de resposta.
    public static TecidoResponse de(Tecido tecido) {
        return new TecidoResponse(
                tecido.getId(),
                tecido.getNome(),
                tecido.getComposicao(),
                tecido.getGramatura(),
                tecido.getLargura(),
                tecido.getPrecoMetro(),
                tecido.getFotoUrl(),
                tecido.isAtivo());
    }
}
