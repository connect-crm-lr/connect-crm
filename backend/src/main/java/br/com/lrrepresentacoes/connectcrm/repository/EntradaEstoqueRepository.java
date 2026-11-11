package br.com.lrrepresentacoes.connectcrm.repository;

import br.com.lrrepresentacoes.connectcrm.model.EntradaEstoque;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EntradaEstoqueRepository extends JpaRepository<EntradaEstoque, Long> {

    // Histórico das chegadas de uma linha de estoque, da mais recente para a mais antiga.
    List<EntradaEstoque> findByEstoqueIdOrderByDataEntradaDescIdDesc(Long idEstoque);
}
