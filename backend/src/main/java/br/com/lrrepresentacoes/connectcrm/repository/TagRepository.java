package br.com.lrrepresentacoes.connectcrm.repository;

import br.com.lrrepresentacoes.connectcrm.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    // Regra 6.4: antes de criar, procurar a tag pelo nome sem diferenciar maiúsculas.
    Optional<Tag> findByNomeIgnoreCase(String nome);

    List<Tag> findAllByOrderByNomeAsc();

    // cliente_tag não tem entidade própria, por isso a contagem é feita em SQL nativo.
    @Query(value = "SELECT COUNT(*) FROM cliente_tag WHERE id_tag = :idTag", nativeQuery = true)
    long contarClientes(@Param("idTag") Long idTag);
}
