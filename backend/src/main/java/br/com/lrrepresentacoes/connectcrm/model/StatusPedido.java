package br.com.lrrepresentacoes.connectcrm.model;

/**
 * Situação do pedido. Os valores são os mesmos do CHECK ck_pedido_status da V1.
 * EM_ABERTO = pedido não pago (regra 6.2). CANCELADO é definitivo: o pedido continua no banco.
 */
public enum StatusPedido {
    EM_ABERTO,
    PAGO,
    CANCELADO
}
