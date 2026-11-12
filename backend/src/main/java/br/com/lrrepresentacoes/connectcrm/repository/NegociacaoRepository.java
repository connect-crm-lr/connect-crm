package br.com.lrrepresentacoes.connectcrm.repository;

import br.com.lrrepresentacoes.connectcrm.model.Negociacao;
import org.springframework.data.jpa.repository.JpaRepository;

// Criar e mover etapa usam só o save() e o findById() herdados do JpaRepository.
public interface NegociacaoRepository extends JpaRepository<Negociacao, Long> {
}
