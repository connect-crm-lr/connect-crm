package br.com.lrrepresentacoes.connectcrm.exception;

// Registro procurado não existe. O GlobalExceptionHandler responde 404.
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
