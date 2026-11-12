package br.com.lrrepresentacoes.connectcrm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Modal "Novo Follow-up": data prevista e descrição da próxima ação.
 * O follow-up sempre nasce pendente, então a data de conclusão não faz parte do cadastro.
 */
public record FollowUpRequest(
        @NotNull(message = "A data prevista é obrigatória")
        LocalDate dataPrevista,

        @NotBlank(message = "A descrição é obrigatória")
        String descricao
) {

    // Normaliza antes da validação (@Valid roda depois que o JSON vira objeto): sem espaços nas pontas.
    public FollowUpRequest {
        descricao = descricao == null ? null : descricao.trim();
    }
}
