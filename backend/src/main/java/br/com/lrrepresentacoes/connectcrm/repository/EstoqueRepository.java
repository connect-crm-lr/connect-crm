package br.com.lrrepresentacoes.connectcrm.repository;

import br.com.lrrepresentacoes.connectcrm.model.Estoque;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
     * Busca a linha do tecido + cor travando-a até o fim da transação (SELECT ... FOR UPDATE).
     * Assim duas entradas ao mesmo tempo na mesma linha não perdem a soma uma da outra.
     * A cor é comparada sem diferenciar maiúsculas para "Off-white" e "off-white" caírem na mesma linha.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT e FROM Estoque e
            WHERE e.tecido.id = :idTecido AND LOWER(e.corCodigo) = LOWER(:corCodigo)
            """)
    Optional<Estoque> buscarParaAtualizar(@Param("idTecido") Long idTecido,
                                          @Param("corCodigo") String corCodigo);
}
