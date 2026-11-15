package br.com.lrrepresentacoes.connectcrm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dados para criar uma negociação: o cliente e a descrição do card.
 * A etapa não é enviada: toda negociação nasce em PRIMEIRO_CONTATO.
 */
public record NegociacaoRequest(
        @NotNull(message = "O cliente é obrigatório")
        Long idCliente,

        @NotBlank(message = "A descrição é obrigatória")
        @Size(max = 200, message = "A descrição pode ter no máximo 200 caracteres")
        String descricao
) {

    // Normaliza antes da validação (@Valid roda depois que o JSON vira objeto): sem espaços nas pontas.
    public NegociacaoRequest {
        descricao = descricao == null ? null : descricao.trim();
    }
}
