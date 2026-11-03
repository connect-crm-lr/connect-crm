package br.com.lrrepresentacoes.connectcrm.controller;

import br.com.lrrepresentacoes.connectcrm.dto.TagRequest;
import br.com.lrrepresentacoes.connectcrm.dto.TagResponse;
import br.com.lrrepresentacoes.connectcrm.dto.TagUsoResponse;
import br.com.lrrepresentacoes.connectcrm.service.TagService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public List<TagResponse> listar() {
        return tagService.listar();
    }

    // Devolve 200 também quando reaproveita uma tag que já existia (POST idempotente por nome).
    @PostMapping
    public TagResponse criar(@RequestBody @Valid TagRequest request) {
        return tagService.criar(request);
    }

    @GetMapping("/{id}/uso")
    public TagUsoResponse consultarUso(@PathVariable Long id) {
        return tagService.consultarUso(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        tagService.excluir(id);
    }
}
