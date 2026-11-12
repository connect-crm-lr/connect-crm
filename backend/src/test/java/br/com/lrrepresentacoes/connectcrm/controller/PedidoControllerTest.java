package br.com.lrrepresentacoes.connectcrm.controller;

import br.com.lrrepresentacoes.connectcrm.dto.ItemPedidoResponse;
import br.com.lrrepresentacoes.connectcrm.dto.PedidoRequest;
import br.com.lrrepresentacoes.connectcrm.dto.PedidoResponse;
import br.com.lrrepresentacoes.connectcrm.exception.GlobalExceptionHandler;
import br.com.lrrepresentacoes.connectcrm.exception.RecursoNaoEncontradoException;
import br.com.lrrepresentacoes.connectcrm.exception.RegraNegocioException;
import br.com.lrrepresentacoes.connectcrm.model.FormaPagamento;
import br.com.lrrepresentacoes.connectcrm.model.StatusPedido;
import br.com.lrrepresentacoes.connectcrm.service.PedidoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa a requisição HTTP passando pelo Spring MVC: controller, validação (@Valid)
 * e GlobalExceptionHandler. O service é simulado com Mockito.
 */
@ExtendWith(MockitoExtension.class)
class PedidoControllerTest {

    @Mock
    private PedidoService pedidoService;

    private MockMvc mockMvc;

    private static final String CORPO_VALIDO = """
            {"idCliente": 1, "formaPagamento": "BOLETO",
             "itens": [{"idEstoque": 10, "metragem": 10.00}, {"idEstoque": 11, "metragem": 2.5}]}
            """;

    private static PedidoResponse resposta(StatusPedido status) {
        return new PedidoResponse(7L, 1L, "Ateliê Bella Noiva", null, LocalDateTime.of(2026, 11, 3, 10, 0),
                FormaPagamento.BOLETO, status, new BigDecimal("497.25"), List.of(
                new ItemPedidoResponse(1L, 10L, "Linho Natural", "Natural", new BigDecimal("10.00"),
                        new BigDecimal("42.50"), new BigDecimal("425.00")),
                new ItemPedidoResponse(2L, 11L, "Viscose", "Azul", new BigDecimal("2.50"),
                        new BigDecimal("28.90"), new BigDecimal("72.25"))));
    }

    @BeforeEach
    void configurar() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PedidoController(pedidoService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ---------- POST /pedidos ----------

    @Test
    @DisplayName("CT-RF008-01: confirmar pedido válido responde 201 com status, total e itens")
    void confirmar_valido_responde201() throws Exception {
        when(pedidoService.confirmar(any(PedidoRequest.class))).thenReturn(resposta(StatusPedido.EM_ABERTO));

        mockMvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("EM_ABERTO"))
                .andExpect(jsonPath("$.total").value(497.25))
                .andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.itens[0].precoMetro").value(42.50));

        // Sem "pagoNoAto" no corpo, o pedido não é pago no ato; sem "idNegociacao", é venda direta.
        ArgumentCaptor<PedidoRequest> captor = ArgumentCaptor.forClass(PedidoRequest.class);
        verify(pedidoService).confirmar(captor.capture());
        assertThat(captor.getValue().pagoNoAto()).isFalse();
        assertThat(captor.getValue().idNegociacao()).isNull();
        assertThat(captor.getValue().itens()).hasSize(2);
    }

    @Test
    @DisplayName("CT-RF008-03: \"pagoNoAto\": true chega ao service")
    void confirmar_pagoNoAto_repassaParaOService() throws Exception {
        when(pedidoService.confirmar(any(PedidoRequest.class))).thenReturn(resposta(StatusPedido.PAGO));

        mockMvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idCliente\": 1, \"formaPagamento\": \"A_VISTA\", \"pagoNoAto\": true, "
                                + "\"itens\": [{\"idEstoque\": 10, \"metragem\": 1}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PAGO"));

        ArgumentCaptor<PedidoRequest> captor = ArgumentCaptor.forClass(PedidoRequest.class);
        verify(pedidoService).confirmar(captor.capture());
        assertThat(captor.getValue().pagoNoAto()).isTrue();
    }

    @Test
    @DisplayName("Pedido sem itens responde 400 com o erro no campo itens")
    void confirmar_semItens_responde400() throws Exception {
        mockMvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idCliente\": 1, \"formaPagamento\": \"BOLETO\", \"itens\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("itens"))
                .andExpect(jsonPath("$.campos[0].mensagem").value("O pedido precisa de pelo menos um item"));
        verifyNoInteractions(pedidoService);
    }

    @Test
    @DisplayName("Pedido sem cliente responde 400 com o erro no campo idCliente")
    void confirmar_semCliente_responde400() throws Exception {
        mockMvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"formaPagamento\": \"BOLETO\", \"itens\": [{\"idEstoque\": 10, \"metragem\": 1}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("idCliente"));
        verifyNoInteractions(pedidoService);
    }

    @Test
    @DisplayName("Item com metragem zero responde 400 com o erro no campo do item")
    void confirmar_itemComMetragemZero_responde400NoCampoDoItem() throws Exception {
        mockMvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idCliente\": 1, \"formaPagamento\": \"BOLETO\", "
                                + "\"itens\": [{\"idEstoque\": 10, \"metragem\": 0}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("itens[0].metragem"))
                .andExpect(jsonPath("$.campos[0].mensagem").value("A metragem deve ser maior que zero"));
        verifyNoInteractions(pedidoService);
    }

    @Test
    @DisplayName("Forma de pagamento que não existe responde 400 com o erro no campo formaPagamento")
    void confirmar_formaDePagamentoInvalida_responde400() throws Exception {
        mockMvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idCliente\": 1, \"formaPagamento\": \"PIX\", "
                                + "\"itens\": [{\"idEstoque\": 10, \"metragem\": 1}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("formaPagamento"))
                .andExpect(jsonPath("$.campos[0].mensagem")
                        .value("Forma de pagamento inválida. Use A_VISTA, CARTAO, BOLETO ou CHEQUE"));
        verifyNoInteractions(pedidoService);
    }

    // Garante que a lista do @Pattern do request acompanha o enum: nenhum valor do enum é recusado.
    @ParameterizedTest
    @EnumSource(FormaPagamento.class)
    @DisplayName("As quatro formas de pagamento do enum passam pela validação")
    void confirmar_formasDePagamentoDoEnum_passamPelaValidacao(FormaPagamento forma) throws Exception {
        mockMvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idCliente\": 1, \"formaPagamento\": \"" + forma.name() + "\", "
                                + "\"itens\": [{\"idEstoque\": 10, \"metragem\": 1}]}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("CT-RD003a009-01: estoque insuficiente responde 422 com a mensagem")
    void confirmar_estoqueInsuficiente_responde422ComAMensagem() throws Exception {
        when(pedidoService.confirmar(any(PedidoRequest.class)))
                .thenThrow(new RegraNegocioException("Estoque insuficiente: Linho Bege 102 tem 18 m"));

        mockMvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.mensagem").value("Estoque insuficiente: Linho Bege 102 tem 18 m"));
    }

    // ---------- PATCH /pedidos/{id}/pagar ----------

    @Test
    @DisplayName("CT-RF017-01: marcar como pago responde 200 com status PAGO")
    void marcarComoPago_responde200() throws Exception {
        when(pedidoService.marcarComoPago(7L)).thenReturn(resposta(StatusPedido.PAGO));

        mockMvc.perform(patch("/pedidos/7/pagar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAGO"));
    }

    @Test
    @DisplayName("Marcar como pago um pedido que não existe responde 404")
    void marcarComoPago_inexistente_responde404() throws Exception {
        when(pedidoService.marcarComoPago(99L)).thenThrow(new RecursoNaoEncontradoException("Pedido não encontrado"));

        mockMvc.perform(patch("/pedidos/99/pagar"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Pedido não encontrado"));
    }

    // ---------- PATCH /pedidos/{id}/cancelar ----------

    @Test
    @DisplayName("CT-RF018-01: cancelar responde 200 com status CANCELADO e o pedido completo")
    void cancelar_responde200() throws Exception {
        when(pedidoService.cancelar(7L)).thenReturn(resposta(StatusPedido.CANCELADO));

        mockMvc.perform(patch("/pedidos/7/cancelar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"))
                .andExpect(jsonPath("$.itens.length()").value(2));
    }

    @Test
    @DisplayName("CT-RD008-01: cancelar pela segunda vez responde 422 com a mensagem")
    void cancelar_jaCancelado_responde422() throws Exception {
        when(pedidoService.cancelar(7L)).thenThrow(new RegraNegocioException("O pedido já está cancelado"));

        mockMvc.perform(patch("/pedidos/7/cancelar"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.mensagem").value("O pedido já está cancelado"));
    }
}
