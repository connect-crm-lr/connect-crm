package br.com.lrrepresentacoes.connectcrm.service;

import br.com.lrrepresentacoes.connectcrm.dto.TagRequest;
import br.com.lrrepresentacoes.connectcrm.dto.TagResponse;
import br.com.lrrepresentacoes.connectcrm.dto.TagUsoResponse;
import br.com.lrrepresentacoes.connectcrm.exception.RecursoNaoEncontradoException;
import br.com.lrrepresentacoes.connectcrm.model.Tag;
import br.com.lrrepresentacoes.connectcrm.repository.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    /**
     * Tags são criadas na hora, no cadastro do cliente. Se já existir uma tag com o
     * mesmo nome (sem diferenciar maiúsculas), ela é reaproveitada em vez de duplicada.
     */
    @Transactional
    public TagResponse criar(TagRequest request) {
        String nome = request.nome().trim();
        Tag tag = tagRepository.findByNomeIgnoreCase(nome)
                .orElseGet(() -> tagRepository.save(new Tag(nome)));
        return TagResponse.de(tag);
    }

    @Transactional(readOnly = true)
    public List<TagResponse> listar() {
        return tagRepository.findAllByOrderByNomeAsc().stream()
                .map(TagResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public TagUsoResponse consultarUso(Long id) {
        Tag tag = buscar(id);
        long quantidade = tagRepository.contarClientes(id);
        return new TagUsoResponse(tag.getId(), tag.getNome(), quantidade);
    }

    /**
     * Os vínculos em cliente_tag saem junto (ON DELETE CASCADE na V1).
     * A confirmação com a quantidade de clientes é feita no front, via consultarUso.
     */
    @Transactional
    public void excluir(Long id) {
        Tag tag = buscar(id);
        tagRepository.delete(tag);
    }

    private Tag buscar(Long id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tag não encontrada"));
    }
}
