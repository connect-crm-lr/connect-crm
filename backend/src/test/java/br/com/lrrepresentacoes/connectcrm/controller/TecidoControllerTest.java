package br.com.lrrepresentacoes.connectcrm.controller;

import br.com.lrrepresentacoes.connectcrm.dto.TecidoRequest;
import br.com.lrrepresentacoes.connectcrm.dto.TecidoResponse;
import br.com.lrrepresentacoes.connectcrm.exception.GlobalExceptionHandler;
import br.com.lrrepresentacoes.connectcrm.exception.RegraNegocioException;
import br.com.lrrepresentacoes.connectcrm.service.TecidoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa a requisição HTTP passando pelo Spring MVC: controller, validação (@Valid)
 * e GlobalExceptionHandler. O service é simulado com Mockito.
 */
@ExtendWith(MockitoExtension.class)
class TecidoControllerTest {

    @Mock
    private TecidoService tecidoService;

    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TecidoController(tecidoService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void criar_comNomeEPreco_responde201() throws Exception {
        when(tecidoService.criar(any(TecidoRequest.class))).thenReturn(new TecidoResponse(
                1L, "Viscose", null, null, null, new BigDecimal("29.90"), null, true));

        mockMvc.perform(post("/tecidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"Viscose\", \"precoMetro\": 29.90}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    // CT-RF013-02: sem preço, responde 400 com o erro no campo.
    @Test
    void criar_semPreco_responde400ComOErroDoCampo() throws Exception {
        mockMvc.perform(post("/tecidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"Viscose\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("precoMetro"))
                .andExpect(jsonPath("$.campos[0].mensagem").value("O preço por metro é obrigatório"));
        verifyNoInteractions(tecidoService);
    }

    // CT-RF013-03: preço negativo é recusado antes de chegar ao banco.
    @Test
    void criar_comPrecoNegativo_responde400() throws Exception {
        mockMvc.perform(post("/tecidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"Viscose\", \"precoMetro\": -1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("precoMetro"));
        verifyNoInteractions(tecidoService);
    }

    @Test
    void listar_repassaBuscaEInativosParaOService() throws Exception {
        when(tecidoService.listar("linho", true)).thenReturn(List.of());

        mockMvc.perform(get("/tecidos").param("busca", "linho").param("inativos", "true"))
                .andExpect(status().isOk());

        verify(tecidoService).listar("linho", true);
    }

    @Test
    void desativar_tecidoJaDesativado_responde422ComAMensagem() throws Exception {
        when(tecidoService.desativar(5L)).thenThrow(new RegraNegocioException("O tecido já está desativado"));

        mockMvc.perform(patch("/tecidos/5/desativar"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.mensagem").value("O tecido já está desativado"));
    }
}
