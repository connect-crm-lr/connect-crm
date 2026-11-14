package br.com.lrrepresentacoes.connectcrm.controller;

import br.com.lrrepresentacoes.connectcrm.dto.ItemPedidoResponse;
import br.com.lrrepresentacoes.connectcrm.dto.PedidoResponse;
import br.com.lrrepresentacoes.connectcrm.exception.GlobalExceptionHandler;
import br.com.lrrepresentacoes.connectcrm.exception.RecursoNaoEncontradoException;
import br.com.lrrepresentacoes.connectcrm.model.FormaPagamento;
import br.com.lrrepresentacoes.connectcrm.model.StatusPedido;
import br.com.lrrepresentacoes.connectcrm.service.PedidoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /pedidos e GET /pedidos/{id} (#71), passando pelo Spring MVC e pelo GlobalExceptionHandler. */
@ExtendWith(MockitoExtension.class)
class PedidoListagemControllerTest {

    @Mock
    private PedidoService pedidoService;

    private MockMvc mockMvc;

    private static PedidoResponse resposta(Long id, StatusPedido status) {
        return new PedidoResponse(id, 1L, "Ateliê Bella Noiva", null, LocalDateTime.of(2026, 11, 3, 10, 0),
                FormaPagamento.BOLETO, status, new BigDecimal("425.00"), List.of(
                new ItemPedidoResponse(1L, 10L, "Linho Natural", "Natural", new BigDecimal("10.00"),
                        new BigDecimal("42.50"), new BigDecimal("425.00"))));
    }

    @BeforeEach
    void configurar() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PedidoController(pedidoService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /pedidos sem filtros lista todos, inclusive o cancelado, com o status")
    void listar_semFiltros() throws Exception {
        when(pedidoService.listar(null, null)).thenReturn(List.of(
                resposta(8L, StatusPedido.CANCELADO), resposta(7L, StatusPedido.PAGO)));

        mockMvc.perform(get("/pedidos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(8))
                .andExpect(jsonPath("$[0].status").value("CANCELADO"))
                .andExpect(jsonPath("$[1].status").value("PAGO"));
    }

    @Test
    @DisplayName("GET /pedidos?clienteId=1&status=EM_ABERTO repassa os dois filtros")
    void listar_comFiltros() throws Exception {
        when(pedidoService.listar(1L, StatusPedido.EM_ABERTO))
                .thenReturn(List.of(resposta(7L, StatusPedido.EM_ABERTO)));

        mockMvc.perform(get("/pedidos").param("clienteId", "1").param("status", "EM_ABERTO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("EM_ABERTO"));

        verify(pedidoService).listar(1L, StatusPedido.EM_ABERTO);
    }

    @Test
    @DisplayName("GET /pedidos com status inexistente responde 400 sem chamar o service")
    void listar_statusInvalido_responde400() throws Exception {
        mockMvc.perform(get("/pedidos").param("status", "ATRASADO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Valor inválido para o parâmetro 'status'"));

        verifyNoInteractions(pedidoService);
    }

    @Test
    @DisplayName("GET /pedidos com clienteId não numérico responde 400")
    void listar_clienteIdInvalido_responde400() throws Exception {
        mockMvc.perform(get("/pedidos").param("clienteId", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Valor inválido para o parâmetro 'clienteId'"));
    }

    @Test
    @DisplayName("GET /pedidos/{id} devolve o pedido com os itens e o subtotal")
    void buscarPorId_devolveOsItens() throws Exception {
        when(pedidoService.buscarPorId(7L)).thenReturn(resposta(7L, StatusPedido.EM_ABERTO));

        mockMvc.perform(get("/pedidos/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.total").value(425.00))
                .andExpect(jsonPath("$.itens[0].tecido").value("Linho Natural"))
                .andExpect(jsonPath("$.itens[0].corCodigo").value("Natural"))
                .andExpect(jsonPath("$.itens[0].metragem").value(10.00))
                .andExpect(jsonPath("$.itens[0].precoMetro").value(42.50))
                .andExpect(jsonPath("$.itens[0].subtotal").value(425.00));
    }

    @Test
    @DisplayName("GET /pedidos/{id} inexistente responde 404 com a mensagem")
    void buscarPorId_inexistente_responde404() throws Exception {
        when(pedidoService.buscarPorId(99L)).thenThrow(new RecursoNaoEncontradoException("Pedido não encontrado"));

        mockMvc.perform(get("/pedidos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Pedido não encontrado"));
    }
}
