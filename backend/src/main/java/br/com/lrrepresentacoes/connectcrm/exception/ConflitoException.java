package br.com.lrrepresentacoes.connectcrm.exception;

/**
 * O dado enviado conflita com um registro já cadastrado (ex.: CPF/CNPJ de outro cliente).
 * Verificado no service para dar uma mensagem clara; o GlobalExceptionHandler responde 409.
 */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
