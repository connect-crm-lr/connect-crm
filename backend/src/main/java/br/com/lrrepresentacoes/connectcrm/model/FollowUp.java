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

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Próxima ação a fazer com o cliente, com data (regra 6.4).
 * data_conclusao vazia = pendente; preenchida = concluído.
 */
@Entity
@Table(name = "follow_up")
public class FollowUp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_follow_up")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false, updatable = false)
    private Cliente cliente;

    // Toda tabela de negócio com id_usuario recebe o usuário logado (AGENTS 5.2).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false, updatable = false)
    private Usuario usuario;

    @Column(name = "data_prevista", nullable = false)
    private LocalDate dataPrevista;

    // TEXT na V1: sem limite de tamanho.
    @Column(name = "descricao", nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "data_conclusao")
    private LocalDateTime dataConclusao;

    // Preenchido pelo DEFAULT now() do banco.
    @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
    private LocalDateTime criadoEm;

    protected FollowUp() {
        // exigido pelo JPA
    }

    // Todo follow-up nasce pendente: sem data de conclusão.
    public FollowUp(Cliente cliente, Usuario usuario, LocalDate dataPrevista, String descricao) {
        this.cliente = cliente;
        this.usuario = usuario;
        this.dataPrevista = dataPrevista;
        this.descricao = descricao;
        this.dataConclusao = null;
    }

    // Concluir = preencher a data de conclusão com a data/hora atual (regra 6.4).
    public void concluir(LocalDateTime agora) {
        this.dataConclusao = agora;
    }

    public boolean isConcluido() {
        return dataConclusao != null;
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

    public LocalDate getDataPrevista() {
        return dataPrevista;
    }

    public String getDescricao() {
        return descricao;
    }

    public LocalDateTime getDataConclusao() {
        return dataConclusao;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }
}
