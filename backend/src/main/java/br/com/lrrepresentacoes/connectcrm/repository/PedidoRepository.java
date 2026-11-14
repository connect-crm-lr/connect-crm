package br.com.lrrepresentacoes.connectcrm.repository;

import br.com.lrrepresentacoes.connectcrm.model.Pedido;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
