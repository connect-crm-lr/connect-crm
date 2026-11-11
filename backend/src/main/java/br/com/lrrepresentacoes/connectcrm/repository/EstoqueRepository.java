package br.com.lrrepresentacoes.connectcrm.repository;

import br.com.lrrepresentacoes.connectcrm.model.Estoque;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface EstoqueRepository extends JpaRepository<Estoque, Long> {

    // Tela Estoque: não mostra linhas de tecido desativado (regra 6.5).
    // JOIN FETCH traz o tecido na mesma consulta (evita uma consulta extra por linha).
    @Query("""
            SELECT e FROM Estoque e JOIN FETCH e.tecido t
            WHERE t.ativo = true
            ORDER BY t.nome, e.corCodigo
            """)
    List<Estoque> listarDeTecidosAtivos();

    // Busca pelo nome do tecido ou pela cor/código; o padrão chega em minúsculas e entre '%'.
    @Query("""
            SELECT e FROM Estoque e JOIN FETCH e.tecido t
            WHERE t.ativo = true
              AND (LOWER(t.nome) LIKE :padrao OR LOWER(e.corCodigo) LIKE :padrao)
            ORDER BY t.nome, e.corCodigo
            """)
    List<Estoque> buscarDeTecidosAtivos(@Param("padrao") String padrao);

    // Estoque baixo = metragem abaixo do limite (regra 6.6: < 20 m), calculado na consulta.
    @Query("""
            SELECT e FROM Estoque e JOIN FETCH e.tecido t
            WHERE t.ativo = true AND e.metragem < :limite
            ORDER BY e.metragem, t.nome
            """)
    List<Estoque> listarAbaixoDe(@Param("limite") BigDecimal limite);

    /**
     * Cria a linha tecido + cor com metragem 0 se ela ainda não existir (regra 6.1, passo 2).
     * O ON CONFLICT resolve a corrida de duas primeiras entradas da mesma cor ao mesmo tempo:
     * a segunda espera a primeira terminar e não insere nada, em vez de falhar na
     * uk_estoque_tecido_cor. Capturar a exceção não serviria: no PostgreSQL, depois de um erro
     * a transação inteira fica abortada. metragem e atualizado_em usam os DEFAULT da V1.
     */
    @Modifying
    @Query(value = """
            INSERT INTO estoque (id_tecido, cor_codigo)
            VALUES (:idTecido, :corCodigo)
            ON CONFLICT ON CONSTRAINT uk_estoque_tecido_cor DO NOTHING
            """, nativeQuery = true)
    int criarSeNaoExistir(@Param("idTecido") Long idTecido, @Param("corCodigo") String corCodigo);

    /**
     * Busca a linha do tecido + cor travando-a até o fim da transação (SELECT ... FOR UPDATE).
     * Assim duas entradas ao mesmo tempo na mesma linha não perdem a soma uma da outra.
     * A cor é comparada exatamente como foi gravada, igual à uk_estoque_tecido_cor da V1.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT e FROM Estoque e
            WHERE e.tecido.id = :idTecido AND e.corCodigo = :corCodigo
            """)
    Optional<Estoque> buscarParaAtualizar(@Param("idTecido") Long idTecido,
                                          @Param("corCodigo") String corCodigo);
}
