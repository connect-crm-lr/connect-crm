package br.com.lrrepresentacoes.connectcrm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tecido do catálogo, com preço por metro. A cor não fica aqui: cada cor é uma
 * linha de estoque (tabela estoque), porque o mesmo tecido pode ter várias cores.
 * Tecido nunca é apagado: é desativado (ativo = false) para preservar o histórico.
 */
@Entity
@Table(name = "tecido")
public class Tecido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tecido")
    private Long id;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "composicao", length = 150)
    private String composicao;

    // Gramatura em g/m².
    @Column(name = "gramatura")
    private Integer gramatura;

    // Largura da peça em metros.
    @Column(name = "largura", precision = 4, scale = 2)
    private BigDecimal largura;

    @Column(name = "preco_metro", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoMetro;

    @Column(name = "foto_url", length = 255)
    private String fotoUrl;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    // Preenchido pelo banco (DEFAULT now() na V1).
    @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
    private LocalDateTime criadoEm;

    protected Tecido() {
        // exigido pelo JPA
    }

    public Tecido(String nome, String composicao, Integer gramatura, BigDecimal largura,
                  BigDecimal precoMetro, String fotoUrl) {
        atualizar(nome, composicao, gramatura, largura, precoMetro, fotoUrl);
    }

    public void atualizar(String nome, String composicao, Integer gramatura, BigDecimal largura,
                          BigDecimal precoMetro, String fotoUrl) {
        this.nome = nome;
        this.composicao = composicao;
        this.gramatura = gramatura;
        this.largura = largura;
        this.precoMetro = precoMetro;
        this.fotoUrl = fotoUrl;
    }

    public void desativar() {
        this.ativo = false;
    }

    public void reativar() {
        this.ativo = true;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getComposicao() {
        return composicao;
    }

    public Integer getGramatura() {
        return gramatura;
    }

    public BigDecimal getLargura() {
        return largura;
    }

    public BigDecimal getPrecoMetro() {
        return precoMetro;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }
}
