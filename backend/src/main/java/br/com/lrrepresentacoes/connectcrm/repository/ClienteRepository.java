package br.com.lrrepresentacoes.connectcrm.repository;

import br.com.lrrepresentacoes.connectcrm.model.Cliente;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    /**
     * Listagem da tela Clientes (RF-003/RF-004), em ordem de nome:
     * - padrao: busca no nome sem diferenciar maiúsculas; chega do service em minúsculas,
     *   entre '%' e com '\', '%' e '_' escapados (sem busca, chega "%", que acha todos);
     * - idTag: só clientes com essa tag (null = todas);
     * - incluirInativos: sem ele, só os ativos (regra 6.5).
     * O EntityGraph traz as tags na mesma consulta (evita uma consulta extra por cliente).
     */
    @EntityGraph(attributePaths = "tags")
    @Query("""
            SELECT c FROM Cliente c
            WHERE LOWER(c.nome) LIKE :padrao ESCAPE '\\'
              AND (c.ativo = true OR :incluirInativos = true)
              AND (:idTag IS NULL OR EXISTS (SELECT t FROM c.tags t WHERE t.id = :idTag))
            ORDER BY c.nome
            """)
    List<Cliente> listar(@Param("padrao") String padrao,
                         @Param("idTag") Long idTag,
                         @Param("incluirInativos") boolean incluirInativos);

    // CPF/CNPJ é único (uk_cliente_cpf_cnpj): usado para responder 409 com o nome de quem já tem o documento.
    Optional<Cliente> findByCpfCnpj(String cpfCnpj);

    /*
     * Pedido e negociação têm issues próprias e ainda não têm entidade; por isso as duas
     * contagens abaixo são feitas em SQL nativo, direto nas tabelas da V1.
     */

    // "Pedido em aberto" = pedido não pago (regra 6.2).
    @Query(value = "SELECT COUNT(*) FROM pedido WHERE id_cliente = :idCliente AND status = 'EM_ABERTO'",
            nativeQuery = true)
    long contarPedidosEmAberto(@Param("idCliente") Long idCliente);

    // Negociação em andamento = estágio diferente de FECHADA e PERDIDA (regra 6.5).
    @Query(value = """
            SELECT COUNT(*) FROM negociacao
            WHERE id_cliente = :idCliente AND estagio NOT IN ('FECHADA', 'PERDIDA')
            """, nativeQuery = true)
    long contarNegociacoesEmAndamento(@Param("idCliente") Long idCliente);
}
