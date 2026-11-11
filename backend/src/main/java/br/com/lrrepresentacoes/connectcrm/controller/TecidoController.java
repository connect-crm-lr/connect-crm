package br.com.lrrepresentacoes.connectcrm.controller;

import br.com.lrrepresentacoes.connectcrm.dto.TecidoRequest;
import br.com.lrrepresentacoes.connectcrm.dto.TecidoResponse;
import br.com.lrrepresentacoes.connectcrm.service.TecidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tecidos")
public class TecidoController {

    private final TecidoService tecidoService;

    public TecidoController(TecidoService tecidoService) {
        this.tecidoService = tecidoService;
    }

    // GET /tecidos?busca=linho&inativos=true
    @GetMapping
    public List<TecidoResponse> listar(@RequestParam(required = false) String busca,
                                       @RequestParam(name = "inativos", defaultValue = "false") boolean incluirInativos) {
        return tecidoService.listar(busca, incluirInativos);
    }

    @GetMapping("/{id}")
    public TecidoResponse buscarPorId(@PathVariable Long id) {
        return tecidoService.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TecidoResponse criar(@RequestBody @Valid TecidoRequest request) {
        return tecidoService.criar(request);
    }

    @PutMapping("/{id}")
    public TecidoResponse atualizar(@PathVariable Long id, @RequestBody @Valid TecidoRequest request) {
        return tecidoService.atualizar(id, request);
    }

    // Não existe DELETE: o "Excluir" da tela desativa o tecido (regra 6.5).
    @PatchMapping("/{id}/desativar")
    public TecidoResponse desativar(@PathVariable Long id) {
        return tecidoService.desativar(id);
    }

    @PatchMapping("/{id}/reativar")
    public TecidoResponse reativar(@PathVariable Long id) {
        return tecidoService.reativar(id);
    }
}
