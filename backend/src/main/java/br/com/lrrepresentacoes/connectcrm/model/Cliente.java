package br.com.lrrepresentacoes.connectcrm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Loja, ateliê ou confecção atendida pelo revendedor. Entidade central do CRM.
 * Cliente nunca é apagado: é desativado (regra 6.5), para preservar o histórico.
 */
@Entity
@Table(name = "cliente")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Long id;

    // Toda tabela de negócio com id_usuario recebe o usuário logado (AGENTS 5.2).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false, updatable = false)
    private Usuario usuario;

    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    // Só dígitos, com o DDI 55 (ex.: 5562999998888). A máscara é só visual, no front.
    @Column(name = "telefone", nullable = false, length = 13)
    private String telefone;

    @Column(name = "email", length = 150)
    private String email;

    // Opcional e único (uk_cliente_cpf_cnpj); só dígitos.
    @Column(name = "cpf_cnpj", length = 14)
    private String cpfCnpj;

    @Column(name = "cidade", length = 100)
    private String cidade;

    // CHAR(2) na V1: sem o JdbcTypeCode, o ddl-auto=validate esperaria VARCHAR.
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "uf", length = 2)
    private String uf;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    // Preenchido pelo DEFAULT now() do banco.
    @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
    private LocalDateTime criadoEm;

    // Tags de interesse (N:N pela cliente_tag). A tag não conhece os clientes.
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "cliente_tag",
            joinColumns = @JoinColumn(name = "id_cliente"),
            inverseJoinColumns = @JoinColumn(name = "id_tag"))
    private Set<Tag> tags = new HashSet<>();

    protected Cliente() {
        // exigido pelo JPA
    }

    public Cliente(Usuario usuario, String nome, String telefone, String email,
                   String cpfCnpj, String cidade, String uf) {
        this.usuario = usuario;
        this.ativo = true;
        atualizarDados(nome, telefone, email, cpfCnpj, cidade, uf);
    }

    public void atualizarDados(String nome, String telefone, String email,
                               String cpfCnpj, String cidade, String uf) {
        this.nome = nome;
        this.telefone = telefone;
        this.email = email;
        this.cpfCnpj = cpfCnpj;
        this.cidade = cidade;
        this.uf = uf;
    }

    // Troca o conjunto inteiro: as tags que saírem da lista são desvinculadas.
    public void definirTags(Set<Tag> novasTags) {
        this.tags.clear();
        this.tags.addAll(novasTags);
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

    public Usuario getUsuario() {
        return usuario;
    }

    public String getNome() {
        return nome;
    }

    public String getTelefone() {
        return telefone;
    }

    public String getEmail() {
        return email;
    }

    public String getCpfCnpj() {
        return cpfCnpj;
    }

    public String getCidade() {
        return cidade;
    }

    public String getUf() {
        return uf;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public Set<Tag> getTags() {
        return tags;
    }
}
