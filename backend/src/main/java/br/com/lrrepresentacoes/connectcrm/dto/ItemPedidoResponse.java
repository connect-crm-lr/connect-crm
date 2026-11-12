package br.com.lrrepresentacoes.connectcrm.dto;

import br.com.lrrepresentacoes.connectcrm.model.ItemPedido;

import java.math.BigDecimal;

/**
 * Item do pedido na resposta. "precoMetro" é o preço gravado no item (o da época da venda)
 * e "subtotal" é metragem × precoMetro.
 */
public record ItemPedidoResponse(
        Long id,
        Long idEstoque,
        String tecido,
        String corCodigo,
        BigDecimal metragem,
        BigDecimal precoMetro,
        BigDecimal subtotal
) {

    // Padrão Factory Method: único ponto de conversão do item para o DTO de resposta.
    public static ItemPedidoResponse de(ItemPedido item) {
        return new ItemPedidoResponse(
                item.getId(),
                item.getEstoque().getId(),
                item.getEstoque().getTecido().getNome(),
                item.getEstoque().getCorCodigo(),
                item.getMetragem(),
                item.getPrecoMetro(),
                item.getSubtotal());
    }
}
