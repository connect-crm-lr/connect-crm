package br.com.lrrepresentacoes.connectcrm.exception;

import br.com.lrrepresentacoes.connectcrm.dto.ErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converte as exceções da aplicação no formato ErroResponse, para o front tratar
 * todos os erros do mesmo jeito.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException ex,
                                                  HttpServletRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
            // Mantém a primeira mensagem de cada campo (o front mostra uma por campo).
            campos.putIfAbsent(erro.getField(), erro.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(ErroResponse.deValidacao(request.getRequestURI(), campos));
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException ex,
                                                      HttpServletRequest request) {
        return resposta(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResponse> regraNegocio(RegraNegocioException ex, HttpServletRequest request) {
        return resposta(HttpStatusCode.valueOf(422), ex.getMessage(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoInvalido(HttpMessageNotReadableException ex,
                                                      HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "O corpo da requisição está vazio ou mal formatado", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> parametroInvalido(MethodArgumentTypeMismatchException ex,
                                                          HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "Valor inválido para o parâmetro '" + ex.getName() + "'", request);
    }

    // Restrições do banco (UNIQUE, FK, CHECK) que escaparam das validações do service.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> integridade(DataIntegrityViolationException ex,
                                                    HttpServletRequest request) {
        log.warn("Violação de integridade em {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return resposta(HttpStatus.CONFLICT, "A operação conflita com dados já cadastrados", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> inesperado(Exception ex, HttpServletRequest request) {
        // Exceções do próprio Spring (rota inexistente, método não permitido...) já trazem o status certo.
        if (ex instanceof ErrorResponse erroSpring) {
            HttpStatusCode status = erroSpring.getStatusCode();
            String mensagem = status.value() == 404 ? "Recurso não encontrado" : "Requisição não suportada";
            return resposta(status, mensagem, request);
        }
        log.error("Erro inesperado em {}", request.getRequestURI(), ex);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno. Tente novamente mais tarde", request);
    }

    private ResponseEntity<ErroResponse> resposta(HttpStatusCode status, String mensagem, HttpServletRequest request) {
        HttpStatus conhecido = HttpStatus.resolve(status.value());
        String erro = conhecido != null ? conhecido.getReasonPhrase() : "Erro";
        return ResponseEntity.status(status)
                .body(ErroResponse.de(status.value(), erro, mensagem, request.getRequestURI()));
    }
}
