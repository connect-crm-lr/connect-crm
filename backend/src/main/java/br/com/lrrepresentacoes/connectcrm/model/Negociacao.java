package br.com.lrrepresentacoes.connectcrm.model;

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
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Card do funil de vendas. O nome é "negociacao" em todas as camadas (AGENTS 5.2).
 * As três datas contam a história do card: quando nasceu, quando mudou de etapa pela
 * última vez e quando foi encerrado (FECHADA ou PERDIDA).
 */
@Entity
@Table(name = "negociacao")
public class Negociacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_negociacao")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false, updatable = false)
    private Cliente cliente;

    // Toda tabela de negócio com id_usuario recebe o usuário logado (AGENTS 5.2).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false, updatable = false)
    private Usuario usuario;

    @Column(name = "descricao", nullable = false, length = 200)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "estagio", nullable = false, length = 30)
    private EstagioNegociacao estagio;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_ultima_movimentacao", nullable = false)
    private LocalDateTime dataUltimaMovimentacao;

    // Preenchida só enquanto a negociação está em FECHADA ou PERDIDA.
    @Column(name = "data_fechamento")
    private LocalDateTime dataFechamento;

    protected Negociacao() {
        // exigido pelo JPA
    }

    // Toda negociação nasce na primeira etapa do funil, sem data de fechamento.
    public Negociacao(Cliente cliente, Usuario usuario, String descricao, LocalDateTime agora) {
        this.cliente = cliente;
        this.usuario = usuario;
        this.descricao = descricao;
        this.estagio = EstagioNegociacao.PRIMEIRO_CONTATO;
        this.dataCriacao = agora;
        this.dataUltimaMovimentacao = agora;
        this.dataFechamento = null;
    }

    /**
     * Muda o card de etapa (regra 6.3 / RD-010):
     * - toda mudança atualiza data_ultima_movimentacao;
     * - entrar em FECHADA ou PERDIDA preenche data_fechamento;
     * - sair dessas etapas volta data_fechamento para null.
     */
    public void moverPara(EstagioNegociacao novoEstagio, LocalDateTime agora) {
        this.estagio = novoEstagio;
        this.dataUltimaMovimentacao = agora;
        this.dataFechamento = novoEstagio.isEncerrada() ? agora : null;
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

    public String getDescricao() {
        return descricao;
    }

    public EstagioNegociacao getEstagio() {
        return estagio;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public LocalDateTime getDataUltimaMovimentacao() {
        return dataUltimaMovimentacao;
    }

    public LocalDateTime getDataFechamento() {
        return dataFechamento;
    }
}
