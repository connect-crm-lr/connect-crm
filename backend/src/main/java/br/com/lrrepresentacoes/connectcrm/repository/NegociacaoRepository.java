package br.com.lrrepresentacoes.connectcrm.repository;

import br.com.lrrepresentacoes.connectcrm.model.Negociacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

// Criar e mover etapa usam só o save() e o findById() herdados do JpaRepository.
public interface NegociacaoRepository extends JpaRepository<Negociacao, Long> {

    /**
     * Dashboard, "Negociações por etapa": cada linha é [EstagioNegociacao, quantidade].
     * Etapa sem negociação não aparece aqui; o service completa com zero.
     */
    @Query("SELECT n.estagio, COUNT(n) FROM Negociacao n GROUP BY n.estagio")
    List<Object[]> contarPorEstagio();
}
