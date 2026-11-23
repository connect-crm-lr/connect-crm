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
 * (UNIQUE tecido + cor, V3). O código da cor (ex.: "05") é opcional e só informativo. O banco impede saldo negativo (CHECK metragem >= 0).
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

    @Column(name = "codigo", length = 20)
    private String codigo;

    @Column(name = "cor", nullable = false, length = 50)
    private String cor;

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
    public Estoque(Tecido tecido, String codigo, String cor) {
        this.tecido = tecido;
        this.codigo = codigo;
        this.cor = cor;
        this.metragem = BigDecimal.ZERO;
        this.atualizadoEm = LocalDateTime.now(FUSO);
    }

    // Linha que existia sem código recebe o código informado numa entrada (nunca troca um código já gravado).
    public void preencherCodigo(String novoCodigo) {
        if (this.codigo == null) {
            this.codigo = novoCodigo;
        }
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

    public String getCodigo() {
        return codigo;
    }

    public String getCor() {
        return cor;
    }

    /** Texto para exibir: "05 Marinho" ou só "Marinho" quando não há código. */
    public String getDescricaoCor() {
        return codigo == null ? cor : codigo + " " + cor;
    }

    public BigDecimal getMetragem() {
        return metragem;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }
}
