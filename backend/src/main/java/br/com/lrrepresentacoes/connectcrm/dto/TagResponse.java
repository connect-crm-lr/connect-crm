package br.com.lrrepresentacoes.connectcrm.dto;

import br.com.lrrepresentacoes.connectcrm.model.Tag;

public record TagResponse(Long id, String nome) {

    public static TagResponse de(Tag tag) {
        return new TagResponse(tag.getId(), tag.getNome());
    }
}
