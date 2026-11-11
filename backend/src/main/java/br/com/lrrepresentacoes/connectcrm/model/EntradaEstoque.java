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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Histórico de cada chegada de tecido. A entrada nunca é alterada:
 * o saldo atual fica na linha de estoque.
 */
@Entity
@Table(name = "entrada_estoque")
public class EntradaEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_entrada")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estoque", nullable = false)
    private Estoque estoque;

    @Column(name = "metragem", nullable = false, precision = 10, scale = 2)
    private BigDecimal metragem;

    @Column(name = "data_entrada", nullable = false)
    private LocalDate dataEntrada;

    // Preenchido pelo banco (DEFAULT now() na V1).
    @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
    private LocalDateTime criadoEm;

    protected EntradaEstoque() {
        // exigido pelo JPA
    }

    public EntradaEstoque(Estoque estoque, BigDecimal metragem, LocalDate dataEntrada) {
        this.estoque = estoque;
        this.metragem = metragem;
        this.dataEntrada = dataEntrada;
    }

    public Long getId() {
        return id;
    }

    public Estoque getEstoque() {
        return estoque;
    }

    public BigDecimal getMetragem() {
        return metragem;
    }

    public LocalDate getDataEntrada() {
        return dataEntrada;
    }
}
