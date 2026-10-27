package br.com.lrrepresentacoes.connectcrm.exception;

import br.com.lrrepresentacoes.connectcrm.dto.ErroResponse;
import br.com.lrrepresentacoes.connectcrm.dto.TagRequest;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/tags");

    @Test
    void validacao_devolve400ComOErroDeCadaCampo() {
        BeanPropertyBindingResult resultado = new BeanPropertyBindingResult(new TagRequest(""), "tagRequest");
        resultado.addError(new FieldError("tagRequest", "nome", "Informe o nome da tag"));
        resultado.addError(new FieldError("tagRequest", "nome", "outra mensagem do mesmo campo"));

        ResponseEntity<ErroResponse> resposta =
                handler.validacao(new MethodArgumentNotValidException((MethodParameter) null, resultado), request);

        assertThat(resposta.getStatusCode().value()).isEqualTo(400);
        assertThat(resposta.getBody().campos()).containsExactlyEntriesOf(
                Map.of("nome", "Informe o nome da tag"));
        assertThat(resposta.getBody().caminho()).isEqualTo("/tags");
    }

    @Test
    void recursoNaoEncontrado_devolve404ComAMensagemDoService() {
        ResponseEntity<ErroResponse> resposta =
                handler.naoEncontrado(new RecursoNaoEncontradoException("Tag não encontrada"), request);

        assertThat(resposta.getStatusCode().value()).isEqualTo(404);
        assertThat(resposta.getBody().mensagem()).isEqualTo("Tag não encontrada");
        assertThat(resposta.getBody().campos()).isEmpty();
    }

    @Test
    void regraNegocio_devolve422ComAMensagemDaRegra() {
        ResponseEntity<ErroResponse> resposta = handler.regraNegocio(
                new RegraNegocioException("Estoque insuficiente: Linho Bege 102 tem 18 m"), request);

        assertThat(resposta.getStatusCode().value()).isEqualTo(422);
        assertThat(resposta.getBody().mensagem()).isEqualTo("Estoque insuficiente: Linho Bege 102 tem 18 m");
    }

    @Test
    void integridade_devolve409SemExporODetalheDoBanco() {
        ResponseEntity<ErroResponse> resposta = handler.integridade(
                new DataIntegrityViolationException("duplicate key value violates unique constraint"), request);

        assertThat(resposta.getStatusCode().value()).isEqualTo(409);
        assertThat(resposta.getBody().mensagem()).doesNotContain("duplicate key");
    }

    @Test
    void erroInesperado_devolve500ComMensagemGenerica() {
        ResponseEntity<ErroResponse> resposta =
                handler.inesperado(new IllegalStateException("NullPointer em algum lugar"), request);

        assertThat(resposta.getStatusCode().value()).isEqualTo(500);
        assertThat(resposta.getBody().mensagem()).doesNotContain("NullPointer");
    }
}
