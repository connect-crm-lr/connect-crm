package br.com.lrrepresentacoes.connectcrm.controller;

import br.com.lrrepresentacoes.connectcrm.dto.FollowUpRequest;
import br.com.lrrepresentacoes.connectcrm.dto.FollowUpResponse;
import br.com.lrrepresentacoes.connectcrm.service.FollowUpService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Padrão Singleton: bean do Spring, uma única instância na aplicação.
@RestController
public class FollowUpController {

    private final FollowUpService followUpService;

    public FollowUpController(FollowUpService followUpService) {
        this.followUpService = followUpService;
    }

    // O follow-up pertence a um cliente, por isso é criado e listado dentro de /clientes/{idCliente}.
    @PostMapping("/clientes/{idCliente}/follow-ups")
    @ResponseStatus(HttpStatus.CREATED)
    public FollowUpResponse criar(@PathVariable Long idCliente, @RequestBody @Valid FollowUpRequest request) {
        return followUpService.criar(idCliente, request);
    }

    @GetMapping("/clientes/{idCliente}/follow-ups")
    public List<FollowUpResponse> listar(@PathVariable Long idCliente) {
        return followUpService.listarDoCliente(idCliente);
    }

    // Mesmo formato de /clientes/{id}/desativar: PATCH numa ação, sem corpo.
    @PatchMapping("/follow-ups/{id}/concluir")
    public FollowUpResponse concluir(@PathVariable Long id) {
        return followUpService.concluir(id);
    }

    // "Pendências de hoje": junta follow-ups de vários clientes, por isso fica fora de /clientes.
    @GetMapping("/follow-ups/pendencias")
    public List<FollowUpResponse> listarPendenciasDeHoje() {
        return followUpService.listarPendenciasDeHoje();
    }
}
