package br.com.lrrepresentacoes.connectcrm.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Locale;

/**
 * Dados do cadastro e da edição do cliente.
 * O construtor normaliza os campos ANTES da validação (@Valid roda depois que o JSON
 * vira objeto), então as regras abaixo valem para o valor que será gravado:
 * - telefone e CPF/CNPJ: a máscara é só visual (regra 6.4), então tudo que não é dígito sai;
 * - textos: sem espaços nas pontas; opcional em branco vira null;
 * - UF: em maiúsculas.
 */
public record ClienteRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 150, message = "O nome pode ter no máximo 150 caracteres")
        String nome,

        // 55 + DDD + número de 8 (fixo) ou 9 (celular) dígitos: 12 ou 13 dígitos (VARCHAR(13) na V1).
        // O 55 não é acrescentado pela API: se faltar, o cadastro é recusado.
        @NotBlank(message = "O telefone é obrigatório")
        @Pattern(regexp = "55\\d{10,11}",
                message = "Informe o telefone com DDI 55, DDD e número (ex.: 55 62 99999-8888)")
        String telefone,

        @Email(message = "Informe um e-mail válido")
        @Size(max = 150, message = "O e-mail pode ter no máximo 150 caracteres")
        String email,

        // Opcional; 11 dígitos (CPF) ou 14 (CNPJ). O dígito verificador não é conferido (decisão da equipe).
        @Pattern(regexp = "\\d{11}|\\d{14}", message = "O CPF deve ter 11 dígitos e o CNPJ, 14")
        String cpfCnpj,

        @Size(max = 100, message = "A cidade pode ter no máximo 100 caracteres")
        String cidade,

        @Pattern(regexp = "[A-Z]{2}", message = "Informe a UF com 2 letras (ex.: GO)")
        String uf,

        // Nomes das tags; as que não existem são criadas na hora (regra 6.4).
        List<@NotBlank(message = "O nome da tag é obrigatório")
             @Size(max = 50, message = "O nome da tag pode ter no máximo 50 caracteres") String> tags
) {

    public ClienteRequest {
        nome = texto(nome);
        telefone = soDigitos(telefone);
        email = texto(email);
        cpfCnpj = soDigitos(cpfCnpj);
        cidade = texto(cidade);
        uf = uf == null ? null : texto(uf.toUpperCase(Locale.ROOT));
        tags = tags == null ? List.of() : tags.stream().map(t -> t == null ? null : t.trim()).toList();
    }

    private static String texto(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }

    private static String soDigitos(String valor) {
        return valor == null ? null : texto(valor.replaceAll("\\D", ""));
    }
}
