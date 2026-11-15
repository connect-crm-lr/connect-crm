package br.com.lrrepresentacoes.connectcrm.model;

/**
 * Como a conversa com o cliente aconteceu. Os valores são os mesmos do CHECK
 * ck_interacao_tipo da V1; o front converte para o texto de exibição ("Ligação").
 */
public enum TipoInteracao {
    WHATSAPP,
    LIGACAO,
    VISITA,
    OUTRO
}
