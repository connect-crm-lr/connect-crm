package br.com.lrrepresentacoes.connectcrm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

/**
 * Modal "Registrar interação": tipo, data e texto (AGENTS 7.3).
 * O tipo chega como texto para o valor inválido virar erro do campo (400 com "campos"),
 * e não um erro genérico de JSON. Os valores aceitos são os do enum TipoInteracao.
 */
public record InteracaoRequest(
        @NotBlank(message = "O tipo é obrigatório")
        @Pattern(regexp = "WHATSAPP|LIGACAO|VISITA|OUTRO",
                message = "Tipo inválido. Use WHATSAPP, LIGACAO, VISITA ou OUTRO")
        String tipo,

        // A interação é uma conversa já realizada: a data não pode ser futura.
        @NotNull(message = "A data da interação é obrigatória")
        @PastOrPresent(message = "A data da interação não pode ser no futuro")
        LocalDate dataInteracao,

        @NotBlank(message = "A descrição é obrigatória")
        String descricao
) {

    // Normaliza antes da validação (@Valid roda depois que o JSON vira objeto): sem espaços nas pontas.
    public InteracaoRequest {
        tipo = tipo == null ? null : tipo.trim();
        descricao = descricao == null ? null : descricao.trim();
    }
}
