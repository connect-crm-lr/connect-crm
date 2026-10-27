package br.com.lrrepresentacoes.connectcrm.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Formato único de erro da API. O front mostra "mensagem" num aviso e usa "campos"
 * para exibir cada erro de validação abaixo do campo correspondente.
 */
public record ErroResponse(
        int status,
        String erro,
        String mensagem,
        String caminho,
        LocalDateTime dataHora,
        Map<String, String> campos
) {

    public static ErroResponse de(int status, String erro, String mensagem, String caminho) {
        return new ErroResponse(status, erro, mensagem, caminho, LocalDateTime.now(), Map.of());
    }

    public static ErroResponse deValidacao(String caminho, Map<String, String> campos) {
        return new ErroResponse(400, "Requisição inválida", "Verifique os campos informados",
                caminho, LocalDateTime.now(), campos);
    }
}
