package br.com.lrrepresentacoes.connectcrm.repository;

import br.com.lrrepresentacoes.connectcrm.model.FollowUp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface FollowUpRepository extends JpaRepository<FollowUp, Long> {

    // Aba Follow-ups da ficha: pela data prevista, a mais próxima primeiro.
    List<FollowUp> findByClienteIdOrderByDataPrevistaAscIdAsc(Long idCliente);

    /**
     * "Pendências de hoje" (regra 6.4 / RD-011): não concluídos, com data prevista até hoje
     * (os atrasados também aparecem) e só de cliente ativo. O follow-up de cliente desativado
     * continua no banco, mas sai da lista (regra 6.5).
     * JOIN FETCH traz o cliente na mesma consulta (evita uma consulta extra por linha).
     */
    @Query("""
            SELECT f FROM FollowUp f JOIN FETCH f.cliente c
            WHERE f.dataConclusao IS NULL
              AND f.dataPrevista <= :hoje
              AND c.ativo = true
            ORDER BY f.dataPrevista, f.id
            """)
    List<FollowUp> listarPendencias(@Param("hoje") LocalDate hoje);
}
