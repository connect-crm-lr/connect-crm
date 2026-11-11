package br.com.lrrepresentacoes.connectcrm.dto;

import br.com.lrrepresentacoes.connectcrm.model.EntradaEstoque;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Uma entrada do histórico. "saldoAtual" é o saldo da linha de estoque depois da entrada,
 * para o front atualizar a tabela sem consultar de novo.
 */
public record EntradaEstoqueResponse(
        Long id,
        Long idEstoque,
        String tecido,
        String corCodigo,
        BigDecimal metragem,
        LocalDate dataEntrada,
        BigDecimal saldoAtual
) {

    // Padrão Factory Method: único ponto de conversão da entrada para o DTO de resposta.
    public static EntradaEstoqueResponse de(EntradaEstoque entrada) {
        return new EntradaEstoqueResponse(
                entrada.getId(),
                entrada.getEstoque().getId(),
                entrada.getEstoque().getTecido().getNome(),
                entrada.getEstoque().getCorCodigo(),
                entrada.getMetragem(),
                entrada.getDataEntrada(),
                entrada.getEstoque().getMetragem());
    }
}
