package br.com.lrrepresentacoes.connectcrm.controller;

import br.com.lrrepresentacoes.connectcrm.dto.PedidoRequest;
import br.com.lrrepresentacoes.connectcrm.dto.PedidoResponse;
import br.com.lrrepresentacoes.connectcrm.service.PedidoService;
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
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    // Confirmar pedido: grava o pedido e desconta o estoque na mesma transação.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponse confirmar(@RequestBody @Valid PedidoRequest request) {
        return pedidoService.confirmar(request);
    }

    // Mesmo formato de /clientes/{id}/desativar: PATCH numa ação, sem corpo.
    @PatchMapping("/{id}/pagar")
    public PedidoResponse marcarComoPago(@PathVariable Long id) {
        return pedidoService.marcarComoPago(id);
    }

    // Não existe DELETE: o pedido cancelado continua no banco (regra 6.1).
    @PatchMapping("/{id}/cancelar")
    public PedidoResponse cancelar(@PathVariable Long id) {
        return pedidoService.cancelar(id);
    }
}
