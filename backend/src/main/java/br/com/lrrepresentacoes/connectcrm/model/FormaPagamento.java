package br.com.lrrepresentacoes.connectcrm.model;

/**
 * Como o pedido é pago. Os valores são os mesmos do CHECK ck_pedido_forma_pagamento
 * da V1; o front converte para o texto de exibição ("À vista").
 */
public enum FormaPagamento {
    A_VISTA,
    CARTAO,
    BOLETO,
    CHEQUE
}
