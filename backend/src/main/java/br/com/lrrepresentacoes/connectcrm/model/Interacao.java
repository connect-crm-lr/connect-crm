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

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Conversa já realizada com o cliente (regra 6.4): tipo, data em que ocorreu e descrição.
 * É um registro de histórico: depois de gravada, a interação não é alterada.
 */
@Entity
@Table(name = "interacao")
public class Interacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_interacao")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false, updatable = false)
    private Cliente cliente;

    // Toda tabela de negócio com id_usuario recebe o usuário logado (AGENTS 5.2).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false, updatable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoInteracao tipo;

    // Dia em que a conversa aconteceu, que pode ser anterior ao dia do registro.
    @Column(name = "data_interacao", nullable = false)
    private LocalDate dataInteracao;

    // TEXT na V1: sem limite de tamanho.
    @Column(name = "descricao", nullable = false, columnDefinition = "TEXT")
    private String descricao;

    // Preenchido pelo DEFAULT now() do banco.
    @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
    private LocalDateTime criadoEm;

    protected Interacao() {
        // exigido pelo JPA
    }

    public Interacao(Cliente cliente, Usuario usuario, TipoInteracao tipo,
                     LocalDate dataInteracao, String descricao) {
        this.cliente = cliente;
        this.usuario = usuario;
        this.tipo = tipo;
        this.dataInteracao = dataInteracao;
        this.descricao = descricao;
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

    public TipoInteracao getTipo() {
        return tipo;
    }

    public LocalDate getDataInteracao() {
        return dataInteracao;
    }

    public String getDescricao() {
        return descricao;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }
}
