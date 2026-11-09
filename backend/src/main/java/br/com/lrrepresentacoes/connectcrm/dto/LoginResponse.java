package br.com.lrrepresentacoes.connectcrm.dto;

/**
 * Resposta do login. O front guarda o token e o envia em cada chamada no cabeçalho
 * "Authorization: Bearer <token>".
 */
public record LoginResponse(String token, String tipo, long expiraEmSegundos, String nome) {

    // Padrão Factory Method: toda resposta de login usa o tipo "Bearer".
    public static LoginResponse bearer(String token, long expiraEmSegundos, String nome) {
        return new LoginResponse(token, "Bearer", expiraEmSegundos, nome);
    }
}
