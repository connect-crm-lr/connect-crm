package br.com.lrrepresentacoes.connectcrm.dto;

import java.time.LocalDateTime;
import java.util.List;

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
        List<CampoErro> campos
) {

    // Padrão Factory Method: ponto único de criação do erro, sempre com a data/hora atual.
    public static ErroResponse de(int status, String erro, String mensagem, String caminho) {
        return new ErroResponse(status, erro, mensagem, caminho, LocalDateTime.now(), List.of());
    }

    // Padrão Factory Method: erro de validação (400) com a lista de campos inválidos.
    public static ErroResponse deValidacao(String caminho, List<CampoErro> campos) {
        return new ErroResponse(400, "Requisição inválida", "Verifique os campos informados",
                caminho, LocalDateTime.now(), campos);
    }
}
