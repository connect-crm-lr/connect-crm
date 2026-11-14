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
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Saldo em metros de um tecido em uma cor. Existe uma linha por tecido + cor
 * (UNIQUE na V1). O banco impede saldo negativo (CHECK metragem >= 0).
 */
@Entity
@Table(name = "estoque")
public class Estoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estoque")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tecido", nullable = false)
    private Tecido tecido;

    @Column(name = "cor_codigo", nullable = false, length = 50)
    private String corCodigo;

    @Column(name = "metragem", nullable = false, precision = 10, scale = 2)
    private BigDecimal metragem = BigDecimal.ZERO;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    // Mesmo fuso do banco e do Hibernate (AGENTS 4), independente do fuso da JVM.
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    protected Estoque() {
        // exigido pelo JPA
    }

    // Linha nova começa com metragem 0 (regra 6.1, "Registrar entrada de estoque", passo 2).
    public Estoque(Tecido tecido, String corCodigo) {
        this.tecido = tecido;
        this.corCodigo = corCodigo;
        this.metragem = BigDecimal.ZERO;
        this.atualizadoEm = LocalDateTime.now(FUSO);
    }

    public void somar(BigDecimal metros) {
        this.metragem = this.metragem.add(metros);
        this.atualizadoEm = LocalDateTime.now(FUSO);
    }

    // Baixa da venda. O saldo é conferido antes pelo PedidoService, com a linha travada;
    // o CHECK metragem >= 0 do banco é a última barreira contra saldo negativo.
    public void baixar(BigDecimal metros) {
        this.metragem = this.metragem.subtract(metros);
        this.atualizadoEm = LocalDateTime.now(FUSO);
    }

    public Long getId() {
        return id;
    }

    public Tecido getTecido() {
        return tecido;
    }

    public String getCorCodigo() {
        return corCodigo;
    }

    public BigDecimal getMetragem() {
        return metragem;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }
}
