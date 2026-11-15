package br.com.lrrepresentacoes.connectcrm.repository;

import br.com.lrrepresentacoes.connectcrm.model.Negociacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NegociacaoRepository extends JpaRepository<Negociacao, Long> {

    /**
     * Listagem do kanban (#71): todas as etapas, com filtro opcional por cliente (null = todos).
     * Mais recentemente movidas primeiro. O cliente vem na mesma consulta, porque o card mostra o nome.
     */
    @Query("""
            SELECT n FROM Negociacao n
            JOIN FETCH n.cliente c
            WHERE (:idCliente IS NULL OR c.id = :idCliente)
            ORDER BY n.dataUltimaMovimentacao DESC, n.id DESC
            """)
    List<Negociacao> listar(@Param("idCliente") Long idCliente);

    /**
     * Dashboard, "Negociações por etapa": cada linha é [EstagioNegociacao, quantidade].
     * Etapa sem negociação não aparece aqui; o service completa com zero.
     */
    @Query("SELECT n.estagio, COUNT(n) FROM Negociacao n GROUP BY n.estagio")
    List<Object[]> contarPorEstagio();
}
