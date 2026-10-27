package br.com.lrrepresentacoes.connectcrm.exception;

/**
 * Violação de regra de negócio validada no service, com mensagem pronta para o usuário.
 * Ex.: "Estoque insuficiente: Linho Bege 102 tem 18 m". O GlobalExceptionHandler responde 422.
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
