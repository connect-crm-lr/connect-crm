package br.com.lrrepresentacoes.connectcrm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

/**
 * Dados do Novo Pedido. O total e o preço dos itens não são enviados: quem calcula é o
 * sistema (regra 6.2).
 * - idNegociacao é opcional: vazio = venda direta;
 * - pagoNoAto é opcional: sem ele (ou false), o pedido nasce EM_ABERTO;
 * - a forma de pagamento chega como texto para o valor inválido virar erro do campo
 *   (400 com "campos"). Os valores aceitos são os do enum FormaPagamento.
 */
public record PedidoRequest(
        @NotNull(message = "O cliente é obrigatório")
        Long idCliente,

        Long idNegociacao,

        @NotBlank(message = "A forma de pagamento é obrigatória")
        @Pattern(regexp = "A_VISTA|CARTAO|BOLETO|CHEQUE",
                message = "Forma de pagamento inválida. Use A_VISTA, CARTAO, BOLETO ou CHEQUE")
        String formaPagamento,

        Boolean pagoNoAto,

        @NotEmpty(message = "O pedido precisa de pelo menos um item")
        List<@NotNull(message = "O item não pode ser vazio") @Valid ItemPedidoRequest> itens
) {

    // Normaliza antes da validação (@Valid roda depois que o JSON vira objeto).
    public PedidoRequest {
        formaPagamento = formaPagamento == null ? null : formaPagamento.trim();
        pagoNoAto = Boolean.TRUE.equals(pagoNoAto);
    }
}
