package br.com.lrrepresentacoes.connectcrm.dto;

import br.com.lrrepresentacoes.connectcrm.model.EstagioNegociacao;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Números do painel inicial (RF-021). Tudo é calculado na hora da consulta, nada é gravado.
 * negociacoesPorEtapa traz as 5 etapas na ordem do funil, inclusive as que estão com zero.
 */
public record DashboardResponse(
        BigDecimal vendasMes,
        long pedidosMes,
        Map<EstagioNegociacao, Long> negociacoesPorEtapa,
        List<FollowUpResponse> pendenciasHoje,
        List<EstoqueResponse> estoqueBaixo
) {
}
