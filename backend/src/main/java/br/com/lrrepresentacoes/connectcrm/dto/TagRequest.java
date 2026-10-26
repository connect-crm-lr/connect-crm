package br.com.lrrepresentacoes.connectcrm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TagRequest(
        @NotBlank(message = "Informe o nome da tag")
        @Size(max = 50, message = "O nome da tag pode ter no máximo 50 caracteres")
        String nome
) {
}
