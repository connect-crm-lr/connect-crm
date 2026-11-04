package br.com.lrrepresentacoes.connectcrm.controller;

import br.com.lrrepresentacoes.connectcrm.dto.EntradaEstoqueRequest;
import br.com.lrrepresentacoes.connectcrm.dto.EntradaEstoqueResponse;
import br.com.lrrepresentacoes.connectcrm.dto.EstoqueResponse;
import br.com.lrrepresentacoes.connectcrm.service.EstoqueService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/estoque")
public class EstoqueController {

    private final EstoqueService estoqueService;

    public EstoqueController(EstoqueService estoqueService) {
        this.estoqueService = estoqueService;
    }

    // GET /estoque?busca=linho  ou  GET /estoque?baixo=true
    @GetMapping
    public List<EstoqueResponse> listar(@RequestParam(required = false) String busca,
                                        @RequestParam(name = "baixo", defaultValue = "false") boolean somenteBaixo) {
        return estoqueService.listar(busca, somenteBaixo);
    }

    @PostMapping("/entradas")
    @ResponseStatus(HttpStatus.CREATED)
    public EntradaEstoqueResponse registrarEntrada(@RequestBody @Valid EntradaEstoqueRequest request) {
        return estoqueService.registrarEntrada(request);
    }

    // Histórico de chegadas de uma linha de estoque (tabela entrada_estoque).
    @GetMapping("/{id}/entradas")
    public List<EntradaEstoqueResponse> listarEntradas(@PathVariable Long id) {
        return estoqueService.listarEntradas(id);
    }
}
