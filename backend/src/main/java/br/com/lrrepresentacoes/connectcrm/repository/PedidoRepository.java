package br.com.lrrepresentacoes.connectcrm.repository;

import br.com.lrrepresentacoes.connectcrm.model.Pedido;
import br.com.lrrepresentacoes.connectcrm.model.StatusPedido;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    /**
     * Busca o pedido travando a linha até o fim da transação (SELECT ... FOR UPDATE).
     * Marcar como pago e cancelar leem o status e depois o trocam: com a trava, dois
     * cancelamentos ao mesmo tempo não devolvem a metragem ao estoque duas vezes.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Pedido p WHERE p.id = :id")
    Optional<Pedido> buscarParaAtualizar(@Param("id") Long id);

    /**
     * Listagem de pedidos (#71 e #49), do mais novo para o mais antigo. Filtros opcionais, todos
     * null = sem filtro: cliente, status e período. O período é sobre data_pedido, com início
     * inclusivo e fim EXCLUSIVO: o service passa o dia seguinte à data final à meia-noite, para o
     * último dia entrar inteiro. Pedido CANCELADO aparece na lista com o status (regra 6.2: só fica
     * fora de vendas do mês, histórico e tarja). Cliente e itens (com estoque e tecido) vêm na mesma
     * consulta, para montar o PedidoResponse sem uma consulta extra por pedido.
     * O CAST nas datas é necessário no PostgreSQL: um parâmetro de data null usado só em "IS NULL"
     * não tem tipo definido ("could not determine data type of parameter"); com o CAST ele tem.
     */
    @Query("""
            SELECT DISTINCT p FROM Pedido p
            JOIN FETCH p.cliente c
            LEFT JOIN FETCH p.itens i
            LEFT JOIN FETCH i.estoque e
            LEFT JOIN FETCH e.tecido
            WHERE (:idCliente IS NULL OR c.id = :idCliente)
              AND (:status IS NULL OR p.status = :status)
              AND (CAST(:inicio AS LocalDateTime) IS NULL OR p.dataPedido >= :inicio)
              AND (CAST(:fimExclusivo AS LocalDateTime) IS NULL OR p.dataPedido < :fimExclusivo)
            ORDER BY p.dataPedido DESC, p.id DESC
            """)
    List<Pedido> listar(@Param("idCliente") Long idCliente, @Param("status") StatusPedido status,
                        @Param("inicio") LocalDateTime inicio,
                        @Param("fimExclusivo") LocalDateTime fimExclusivo);

    /**
     * Total do período para o histórico de vendas (#49, RF-022): soma só pedidos que NÃO são
     * CANCELADO (regra 6.2). Sem pedidos, a soma é null (o service troca por zero).
     */
    @Query("""
            SELECT SUM(p.total) FROM Pedido p
            WHERE p.status <> br.com.lrrepresentacoes.connectcrm.model.StatusPedido.CANCELADO
              AND (:status IS NULL OR p.status = :status)
              AND (CAST(:inicio AS LocalDateTime) IS NULL OR p.dataPedido >= :inicio)
              AND (CAST(:fimExclusivo AS LocalDateTime) IS NULL OR p.dataPedido < :fimExclusivo)
            """)
    BigDecimal somarTotalSemCancelados(@Param("status") StatusPedido status,
                                       @Param("inicio") LocalDateTime inicio,
                                       @Param("fimExclusivo") LocalDateTime fimExclusivo);

    // Mesmo filtro da soma: quantos pedidos entraram no total.
    @Query("""
            SELECT COUNT(p) FROM Pedido p
            WHERE p.status <> br.com.lrrepresentacoes.connectcrm.model.StatusPedido.CANCELADO
              AND (:status IS NULL OR p.status = :status)
              AND (CAST(:inicio AS LocalDateTime) IS NULL OR p.dataPedido >= :inicio)
              AND (CAST(:fimExclusivo AS LocalDateTime) IS NULL OR p.dataPedido < :fimExclusivo)
            """)
    long contarSemCancelados(@Param("status") StatusPedido status,
                             @Param("inicio") LocalDateTime inicio,
                             @Param("fimExclusivo") LocalDateTime fimExclusivo);

    /*
     * A negociação tem issue própria (funil) e ainda não tem entidade nesta branch; por isso
     * a consulta abaixo é feita em SQL nativo, direto na tabela da V1, como já acontece
     * nas contagens do ClienteRepository.
     */

    // Cliente da negociação que originou o pedido (regra 6.1, "Confirmar pedido", passo 2).
    @Query(value = "SELECT id_cliente FROM negociacao WHERE id_negociacao = :idNegociacao", nativeQuery = true)
    Optional<Long> buscarIdClienteDaNegociacao(@Param("idNegociacao") Long idNegociacao);

    /**
     * Dashboard, "Vendas do mês" (regra 6.2): soma dos totais no período [inicio, fim),
     * sem os pedidos CANCELADO. COALESCE devolve 0 quando não há venda no período.
     */
    @Query("""
            SELECT COALESCE(SUM(p.total), 0) FROM Pedido p
            WHERE p.status <> br.com.lrrepresentacoes.connectcrm.model.StatusPedido.CANCELADO
              AND p.dataPedido >= :inicio AND p.dataPedido < :fim
            """)
    BigDecimal somarVendasEntre(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    // Quantidade de pedidos não cancelados no mesmo período da soma acima.
    @Query("""
            SELECT COUNT(p) FROM Pedido p
            WHERE p.status <> br.com.lrrepresentacoes.connectcrm.model.StatusPedido.CANCELADO
              AND p.dataPedido >= :inicio AND p.dataPedido < :fim
            """)
    long contarVendasEntre(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);
}
