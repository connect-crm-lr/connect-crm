package br.com.lrrepresentacoes.connectcrm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Item do pedido: aponta para uma linha de estoque (tecido + cor) e guarda a metragem
 * vendida e o preço por metro do momento da venda. O preço não segue o preço atual do
 * tecido: pedidos antigos mantêm o preço da época (regra 6.2).
 */
@Entity
@Table(name = "item_pedido")
public class ItemPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_item_pedido")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pedido", nullable = false, updatable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estoque", nullable = false, updatable = false)
    private Estoque estoque;

    @Column(name = "metragem", nullable = false, updatable = false, precision = 10, scale = 2)
    private BigDecimal metragem;

    // Retrato do preço no ato da venda.
    @Column(name = "preco_metro", nullable = false, updatable = false, precision = 10, scale = 2)
    private BigDecimal precoMetro;

    protected ItemPedido() {
        // exigido pelo JPA
    }

    // Criado só pelo Pedido.adicionarItem, que mantém o total do pedido em dia.
    ItemPedido(Pedido pedido, Estoque estoque, BigDecimal metragem, BigDecimal precoMetro) {
        this.pedido = pedido;
        this.estoque = estoque;
        this.metragem = metragem;
        this.precoMetro = precoMetro;
    }

    // metragem × preço por metro, em reais com 2 casas (meio centavo arredonda para cima).
    public BigDecimal getSubtotal() {
        return metragem.multiply(precoMetro).setScale(2, RoundingMode.HALF_UP);
    }

    public Long getId() {
        return id;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public Estoque getEstoque() {
        return estoque;
    }

    public BigDecimal getMetragem() {
        return metragem;
    }

    public BigDecimal getPrecoMetro() {
        return precoMetro;
    }
}
