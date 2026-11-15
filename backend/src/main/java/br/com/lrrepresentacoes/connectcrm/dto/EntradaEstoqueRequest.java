package br.com.lrrepresentacoes.connectcrm.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Modal "Registrar entrada": tecido, cor, metragem e data (AGENTS 7.3).
 * Sem data, o service usa a data de hoje no fuso America/Sao_Paulo.
 */
public record EntradaEstoqueRequest(
        @NotNull(message = "O tecido é obrigatório")
        Long idTecido,

        @NotBlank(message = "A cor/código é obrigatória")
        @Size(max = 50, message = "A cor/código pode ter no máximo 50 caracteres")
        String corCodigo,

        @NotNull(message = "A metragem é obrigatória")
        @Positive(message = "A metragem deve ser maior que zero")
        @Digits(integer = 8, fraction = 2, message = "A metragem deve ter no máximo 2 casas decimais")
        BigDecimal metragem,

        @PastOrPresent(message = "A data de entrada não pode ser no futuro")
        LocalDate dataEntrada
) {
}
