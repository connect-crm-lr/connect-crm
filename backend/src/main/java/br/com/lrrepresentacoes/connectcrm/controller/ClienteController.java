package br.com.lrrepresentacoes.connectcrm.controller;

import br.com.lrrepresentacoes.connectcrm.dto.ClienteRequest;
import br.com.lrrepresentacoes.connectcrm.dto.ClienteResponse;
import br.com.lrrepresentacoes.connectcrm.service.ClienteService;
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

// Padrão Singleton: bean do Spring, uma única instância na aplicação.
@RestController
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    // GET /clientes?busca=bella&tag=3&inativos=true
    @GetMapping
    public List<ClienteResponse> listar(@RequestParam(required = false) String busca,
                                        @RequestParam(name = "tag", required = false) Long idTag,
                                        @RequestParam(name = "inativos", defaultValue = "false") boolean incluirInativos) {
        return clienteService.listar(busca, idTag, incluirInativos);
    }

    @GetMapping("/{id}")
    public ClienteResponse buscarPorId(@PathVariable Long id) {
        return clienteService.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteResponse criar(@RequestBody @Valid ClienteRequest request) {
        return clienteService.criar(request);
    }

    @PutMapping("/{id}")
    public ClienteResponse atualizar(@PathVariable Long id, @RequestBody @Valid ClienteRequest request) {
        return clienteService.atualizar(id, request);
    }

    // Não existe DELETE: o cliente é desativado (regra 6.5).
    @PatchMapping("/{id}/desativar")
    public ClienteResponse desativar(@PathVariable Long id) {
        return clienteService.desativar(id);
    }

    @PatchMapping("/{id}/reativar")
    public ClienteResponse reativar(@PathVariable Long id) {
        return clienteService.reativar(id);
    }
}
