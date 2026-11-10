package br.com.lrrepresentacoes.connectcrm.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Normalização e validação do cadastro de cliente (regra 6.4 e decisões da equipe):
 * telefone e CPF/CNPJ perdem a máscara e são validados já como serão gravados.
 */
class ClienteRequestTest {

    private static jakarta.validation.ValidatorFactory fabrica;
    private static Validator validator;

    @BeforeAll
    static void criarValidator() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validator = fabrica.getValidator();
    }

    @AfterAll
    static void fecharValidator() {
        fabrica.close();
    }

    private static ClienteRequest comTelefoneECpf(String telefone, String cpfCnpj) {
        return new ClienteRequest("Ateliê", telefone, null, cpfCnpj, null, null, null);
    }

    private static Set<String> camposInvalidos(ClienteRequest request) {
        Set<ConstraintViolation<ClienteRequest>> erros = validator.validate(request);
        return erros.stream().map(e -> e.getPropertyPath().toString()).collect(java.util.stream.Collectors.toSet());
    }

    // Critério de aceitação: telefone gravado só com dígitos e DDI 55.
    @ParameterizedTest
    @ValueSource(strings = {"+55 (62) 99999-8888", "55 62 99999-8888", "5562999998888"})
    void telefoneComMascara_ficaSoComDigitosEValido(String digitado) {
        ClienteRequest request = comTelefoneECpf(digitado, null);

        assertThat(request.telefone()).isEqualTo("5562999998888");
        assertThat(camposInvalidos(request)).isEmpty();
    }

    // Telefone fixo: 55 + DDD + 8 dígitos (12 no total), como nos dados de teste.
    @Test
    void telefoneFixoCom12Digitos_eValido() {
        assertThat(camposInvalidos(comTelefoneECpf("55 (11) 3456-7890", null))).isEmpty();
    }

    // A API não acrescenta o 55: sem ele (ou com tamanho errado), o telefone é recusado.
    @ParameterizedTest
    @ValueSource(strings = {"(62) 99999-8888", "62999998888", "55629999988881", "5562999"})
    void telefoneSemDdi55OuComTamanhoErrado_eInvalido(String digitado) {
        assertThat(camposInvalidos(comTelefoneECpf(digitado, null))).containsExactly("telefone");
    }

    @Test
    void telefoneSemNenhumDigito_eObrigatorio() {
        ClienteRequest request = comTelefoneECpf("sem telefone", null);

        assertThat(request.telefone()).isNull();
        assertThat(camposInvalidos(request)).containsExactly("telefone");
    }

    // CPF (11) e CNPJ (14) perdem a máscara; o dígito verificador não é conferido.
    @Test
    void cpfECnpjComMascara_ficamSoComDigitos() {
        assertThat(comTelefoneECpf("5562999998888", "529.982.247-25").cpfCnpj()).isEqualTo("52998224725");
        assertThat(comTelefoneECpf("5562999998888", "11.222.333/0001-81").cpfCnpj()).isEqualTo("11222333000181");
        assertThat(camposInvalidos(comTelefoneECpf("5562999998888", "11.222.333/0001-81"))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"123.456.789", "1122233300018", "112223330001812"})
    void cpfCnpjComTamanhoErrado_eInvalido(String digitado) {
        assertThat(camposInvalidos(comTelefoneECpf("5562999998888", digitado))).containsExactly("cpfCnpj");
    }

    // Opcional em branco vira null (o UNIQUE do banco aceita vários NULL, mas não vários "").
    @Test
    void opcionaisEmBranco_viramNull_eUfFicaMaiuscula() {
        ClienteRequest request = new ClienteRequest(" Ateliê ", "5562999998888", " ", " ", "  ", "go", null);

        assertThat(request.nome()).isEqualTo("Ateliê");
        assertThat(request.email()).isNull();
        assertThat(request.cpfCnpj()).isNull();
        assertThat(request.cidade()).isNull();
        assertThat(request.uf()).isEqualTo("GO");
        assertThat(request.tags()).isEmpty();
        assertThat(camposInvalidos(request)).isEmpty();
    }

    @Test
    void tagEmBranco_eUfComMaisDeDuasLetras_saoInvalidas() {
        ClienteRequest request = new ClienteRequest("Ateliê", "5562999998888", "nao-e-email", null, null, "GOI",
                List.of("Linho", " "));

        assertThat(camposInvalidos(request)).containsExactlyInAnyOrder("email", "uf", "tags[1].<list element>");
    }
}
