package br.com.lrrepresentacoes.connectcrm.repository;

import br.com.lrrepresentacoes.connectcrm.model.Tecido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TecidoRepository extends JpaRepository<Tecido, Long> {

    // Listagem padrão: só os ativos (regra 6.5).
    List<Tecido> findByAtivoTrueOrderByNomeAsc();

    // Opção "mostrar inativos": todos, para poder reativar.
    List<Tecido> findAllByOrderByNomeAsc();

    /**
     * Busca pelo nome ou pela composição, sem diferenciar maiúsculas (RF-015).
     * O padrão chega pronto do service: em minúsculas e entre '%' (ex.: "%linho%").
     */
    @Query("""
            SELECT t FROM Tecido t
            WHERE (LOWER(t.nome) LIKE :padrao OR LOWER(t.composicao) LIKE :padrao)
              AND (t.ativo = true OR :incluirInativos = true)
            ORDER BY t.nome
            """)
    List<Tecido> buscar(@Param("padrao") String padrao, @Param("incluirInativos") boolean incluirInativos);
}
