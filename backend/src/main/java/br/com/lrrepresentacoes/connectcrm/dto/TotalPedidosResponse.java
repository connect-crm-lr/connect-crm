package br.com.lrrepresentacoes.connectcrm.dto;

import java.math.BigDecimal;

/**
 * Total do histórico de vendas de um período (#49): soma dos pedidos que não são CANCELADO
 * e quantos pedidos entraram nessa soma.
 */
public record TotalPedidosResponse(BigDecimal total, long quantidadePedidos) {

    // Padrão Factory Method: ponto único para o resultado "sem pedidos" (total zero, escala de dinheiro).
    public static TotalPedidosResponse vazio() {
        return new TotalPedidosResponse(BigDecimal.ZERO.setScale(2), 0);
    }
}
