package br.com.lrrepresentacoes.connectcrm.repository;

import br.com.lrrepresentacoes.connectcrm.model.Interacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InteracaoRepository extends JpaRepository<Interacao, Long> {

    // Aba Interações da ficha: a conversa mais recente primeiro.
    List<Interacao> findByClienteIdOrderByDataInteracaoDescIdDesc(Long idCliente);
}
