package br.com.lrrepresentacoes.connectcrm.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Venda confirmada, com seus itens. O pedido nunca é apagado: o cancelamento só troca
 * o status para CANCELADO (regra 6.1), preservando o histórico.
 */
@Entity
@Table(name = "pedido")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false, updatable = false)
    private Cliente cliente;

    // Toda tabela de negócio com id_usuario recebe o usuário logado (AGENTS 5.2).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false, updatable = false)
    private Usuario usuario;

    // Opcional: vazio = venda direta (regra 6.2). Fica só o id porque a entidade Negociacao
    // pertence à issue do funil; a FK fk_pedido_negociacao da V1 garante que ela existe.
    @Column(name = "id_negociacao", updatable = false)
    private Long idNegociacao;

    @Column(name = "data_pedido", nullable = false, updatable = false)
    private LocalDateTime dataPedido;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", nullable = false, updatable = false, length = 20)
    private FormaPagamento formaPagamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusPedido status;

    // Soma dos subtotais dos itens, calculada pelo sistema (regra 6.2).
    @Column(name = "total", nullable = false, precision = 10, scale = 2)
    private BigDecimal total = BigDecimal.ZERO.setScale(2);

    // Os itens são gravados junto com o pedido (cascade), na mesma transação.
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<ItemPedido> itens = new ArrayList<>();

    protected Pedido() {
        // exigido pelo JPA
    }

    // Status inicial: EM_ABERTO, ou PAGO se o pedido foi pago no ato (regra 6.1, passo 7).
    public Pedido(Cliente cliente, Usuario usuario, Long idNegociacao, FormaPagamento formaPagamento,
                  boolean pagoNoAto, LocalDateTime agora) {
        this.cliente = cliente;
        this.usuario = usuario;
        this.idNegociacao = idNegociacao;
        this.formaPagamento = formaPagamento;
        this.status = pagoNoAto ? StatusPedido.PAGO : StatusPedido.EM_ABERTO;
        this.dataPedido = agora;
    }

    /**
     * Acrescenta o item e soma o subtotal dele ao total. O preço por metro é o do tecido
     * no momento da venda e fica gravado no item (preço histórico, regra 6.2).
     */
    public void adicionarItem(Estoque estoque, BigDecimal metragem, BigDecimal precoMetro) {
        ItemPedido item = new ItemPedido(this, estoque, metragem, precoMetro);
        this.itens.add(item);
        this.total = this.total.add(item.getSubtotal());
    }

    public void marcarComoPago() {
        this.status = StatusPedido.PAGO;
    }

    public void cancelar() {
        this.status = StatusPedido.CANCELADO;
    }

    public Long getId() {
        return id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Long getIdNegociacao() {
        return idNegociacao;
    }

    public LocalDateTime getDataPedido() {
        return dataPedido;
    }

    public FormaPagamento getFormaPagamento() {
        return formaPagamento;
    }

    public StatusPedido getStatus() {
        return status;
    }

    public BigDecimal getTotal() {
        return total;
    }

    // Só leitura: item novo entra pelo adicionarItem, que mantém o total em dia.
    public List<ItemPedido> getItens() {
        return Collections.unmodifiableList(itens);
    }
}
