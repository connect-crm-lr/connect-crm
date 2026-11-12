package br.com.lrrepresentacoes.connectcrm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Etapa de destino do card (PATCH /negociacoes/{id}/etapa).
 * O estágio chega como texto para o valor inválido virar erro do campo (400 com "campos"),
 * e não um erro genérico de JSON. Os valores aceitos são os do enum EstagioNegociacao.
 */
public record MoverEtapaRequest(
        @NotBlank(message = "O estágio é obrigatório")
        @Pattern(regexp = "PRIMEIRO_CONTATO|MOSTRUARIO_ENVIADO|AGUARDANDO_RESPOSTA|FECHADA|PERDIDA",
                message = "Estágio inválido. Use PRIMEIRO_CONTATO, MOSTRUARIO_ENVIADO, "
                        + "AGUARDANDO_RESPOSTA, FECHADA ou PERDIDA")
        String estagio
) {

    // Normaliza antes da validação (@Valid roda depois que o JSON vira objeto): sem espaços nas pontas.
    public MoverEtapaRequest {
        estagio = estagio == null ? null : estagio.trim();
    }
}
