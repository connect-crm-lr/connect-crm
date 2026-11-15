package br.com.lrrepresentacoes.connectcrm.dto;

import br.com.lrrepresentacoes.connectcrm.model.Estoque;

import java.math.BigDecimal;

/**
 * Linha da tela Estoque. "estoqueBaixo" é calculado no momento da consulta,
 * nunca gravado no banco (regra 6.6).
 */
public record EstoqueResponse(
        Long id,
        Long idTecido,
        String tecido,
        String corCodigo,
        BigDecimal metragem,
        boolean estoqueBaixo
) {

    // Padrão Factory Method: único ponto de conversão da linha de estoque para o DTO de resposta.
    public static EstoqueResponse de(Estoque estoque, BigDecimal limiteEstoqueBaixo) {
        return new EstoqueResponse(
                estoque.getId(),
                estoque.getTecido().getId(),
                estoque.getTecido().getNome(),
                estoque.getCorCodigo(),
                estoque.getMetragem(),
                estoque.getMetragem().compareTo(limiteEstoqueBaixo) < 0);
    }
}
