package br.com.lrrepresentacoes.connectcrm.controller;

import br.com.lrrepresentacoes.connectcrm.dto.MoverEtapaRequest;
import br.com.lrrepresentacoes.connectcrm.dto.NegociacaoRequest;
import br.com.lrrepresentacoes.connectcrm.dto.NegociacaoResponse;
import br.com.lrrepresentacoes.connectcrm.service.NegociacaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Padrão Singleton: bean do Spring, uma única instância na aplicação.
@RestController
@RequestMapping("/negociacoes")
public class NegociacaoController {

    private final NegociacaoService negociacaoService;

    public NegociacaoController(NegociacaoService negociacaoService) {
        this.negociacaoService = negociacaoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NegociacaoResponse criar(@RequestBody @Valid NegociacaoRequest request) {
        return negociacaoService.criar(request);
    }

    // PATCH /negociacoes/3/etapa  com  {"estagio": "MOSTRUARIO_ENVIADO"}
    @PatchMapping("/{id}/etapa")
    public NegociacaoResponse moverEtapa(@PathVariable Long id, @RequestBody @Valid MoverEtapaRequest request) {
        return negociacaoService.moverEtapa(id, request);
    }
}
