package br.com.lrrepresentacoes.connectcrm.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Dados para cadastrar ou alterar um tecido. Só nome e preço por metro são
 * obrigatórios (AGENTS 7.3, modal Novo Tecido). Os limites seguem as colunas da V1.
 */
public record TecidoRequest(
        @NotBlank(message = "O nome do tecido é obrigatório")
        @Size(max = 100, message = "O nome pode ter no máximo 100 caracteres")
        String nome,

        @Size(max = 150, message = "A composição pode ter no máximo 150 caracteres")
        String composicao,

        @Positive(message = "A gramatura deve ser maior que zero")
        Integer gramatura,

        @Positive(message = "A largura deve ser maior que zero")
        @Digits(integer = 2, fraction = 2, message = "A largura deve ter até 2 dígitos inteiros e 2 casas decimais")
        BigDecimal largura,

        @NotNull(message = "O preço por metro é obrigatório")
        @Positive(message = "O preço por metro deve ser maior que zero")
        @Digits(integer = 8, fraction = 2, message = "O preço deve ter no máximo 2 casas decimais")
        BigDecimal precoMetro,

        @Size(max = 255, message = "A URL da foto pode ter no máximo 255 caracteres")
        String fotoUrl
) {
}
