package br.com.lrrepresentacoes.connectcrm.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Item do Novo Pedido: a linha de estoque (tecido + cor) e a metragem.
 * O preço não é enviado: o sistema usa o preco_metro do tecido no momento da venda (regra 6.2).
 */
public record ItemPedidoRequest(
        @NotNull(message = "A linha de estoque (tecido + cor) é obrigatória")
        Long idEstoque,

        @NotNull(message = "A metragem é obrigatória")
        @Positive(message = "A metragem deve ser maior que zero")
        @Digits(integer = 8, fraction = 2, message = "A metragem deve ter no máximo 2 casas decimais")
        BigDecimal metragem
) {
}
