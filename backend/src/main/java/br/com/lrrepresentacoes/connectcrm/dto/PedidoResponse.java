package br.com.lrrepresentacoes.connectcrm.dto;

import br.com.lrrepresentacoes.connectcrm.model.FormaPagamento;
import br.com.lrrepresentacoes.connectcrm.model.Pedido;
import br.com.lrrepresentacoes.connectcrm.model.StatusPedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Pedido com os itens. O número "#P-0000" da tela é montado pelo front a partir do id.
 */
public record PedidoResponse(
        Long id,
        Long idCliente,
        String cliente,
        Long idNegociacao,
        LocalDateTime dataPedido,
        FormaPagamento formaPagamento,
        StatusPedido status,
        BigDecimal total,
        List<ItemPedidoResponse> itens
) {

    // Padrão Factory Method: único ponto de conversão da entidade Pedido para o DTO de resposta.
    public static PedidoResponse de(Pedido pedido) {
        // Padrão Iterator: stream() percorre os itens convertendo cada um.
        List<ItemPedidoResponse> itens = pedido.getItens().stream()
                .map(ItemPedidoResponse::de)
                .toList();
        return new PedidoResponse(pedido.getId(), pedido.getCliente().getId(), pedido.getCliente().getNome(),
                pedido.getIdNegociacao(), pedido.getDataPedido(), pedido.getFormaPagamento(),
                pedido.getStatus(), pedido.getTotal(), itens);
    }
}
