package br.com.lrrepresentacoes.connectcrm.controller;

import br.com.lrrepresentacoes.connectcrm.dto.InteracaoRequest;
import br.com.lrrepresentacoes.connectcrm.dto.InteracaoResponse;
import br.com.lrrepresentacoes.connectcrm.service.InteracaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Padrão Singleton: bean do Spring, uma única instância na aplicação.
// A interação pertence a um cliente, por isso a rota fica dentro de /clientes/{idCliente}.
@RestController
@RequestMapping("/clientes/{idCliente}/interacoes")
public class InteracaoController {

    private final InteracaoService interacaoService;

    public InteracaoController(InteracaoService interacaoService) {
        this.interacaoService = interacaoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InteracaoResponse registrar(@PathVariable Long idCliente,
                                       @RequestBody @Valid InteracaoRequest request) {
        return interacaoService.registrar(idCliente, request);
    }

    @GetMapping
    public List<InteracaoResponse> listar(@PathVariable Long idCliente) {
        return interacaoService.listarDoCliente(idCliente);
    }
}
