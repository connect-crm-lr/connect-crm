package br.com.lrrepresentacoes.connectcrm.exception;

/**
 * E-mail ou senha errados. A mensagem é a mesma nos dois casos, para não revelar
 * quais e-mails existem no sistema. O GlobalExceptionHandler responde 401.
 */
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException() {
        super("E-mail ou senha inválidos");
    }
}
