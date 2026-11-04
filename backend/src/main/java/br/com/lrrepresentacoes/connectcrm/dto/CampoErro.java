package br.com.lrrepresentacoes.connectcrm.dto;

// Um erro de validação: qual campo e a mensagem para mostrar abaixo dele no front.
public record CampoErro(String campo, String mensagem) {
}
