package br.com.lrrepresentacoes.connectcrm.controller;

import br.com.lrrepresentacoes.connectcrm.dto.NegociacaoResponse;
import br.com.lrrepresentacoes.connectcrm.exception.GlobalExceptionHandler;
import br.com.lrrepresentacoes.connectcrm.model.EstagioNegociacao;
import br.com.lrrepresentacoes.connectcrm.service.NegociacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /negociacoes (#71), passando pelo Spring MVC e pelo GlobalExceptionHandler. */
@ExtendWith(MockitoExtension.class)
class NegociacaoListagemControllerTest {

    @Mock
    private NegociacaoService negociacaoService;

    private MockMvc mockMvc;

    private static NegociacaoResponse card(Long id, Long idCliente, EstagioNegociacao estagio) {
        return new NegociacaoResponse(id, idCliente, "Ateliê Bella Noiva", "Coleção de verão", estagio,
                LocalDateTime.of(2026, 10, 1, 9, 0), LocalDateTime.of(2026, 10, 5, 9, 0), null);
    }

    @BeforeEach
    void configurar() {
        mockMvc = MockMvcBuilders.standaloneSetup(new NegociacaoController(negociacaoService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /negociacoes sem filtro lista todas as negociações")
    void listar_semFiltro() throws Exception {
        when(negociacaoService.listar(null)).thenReturn(List.of(
                card(2L, 2L, EstagioNegociacao.PRIMEIRO_CONTATO), card(1L, 1L, EstagioNegociacao.FECHADA)));

        mockMvc.perform(get("/negociacoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].estagio").value("PRIMEIRO_CONTATO"))
                .andExpect(jsonPath("$[0].cliente").value("Ateliê Bella Noiva"))
                .andExpect(jsonPath("$[1].estagio").value("FECHADA"));
    }

    @Test
    @DisplayName("GET /negociacoes?clienteId=1 repassa o filtro ao service")
    void listar_comCliente() throws Exception {
        when(negociacaoService.listar(1L)).thenReturn(List.of(card(1L, 1L, EstagioNegociacao.MOSTRUARIO_ENVIADO)));

        mockMvc.perform(get("/negociacoes").param("clienteId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idCliente").value(1));

        verify(negociacaoService).listar(1L);
    }

    @Test
    @DisplayName("GET /negociacoes com clienteId não numérico responde 400")
    void listar_clienteIdInvalido_responde400() throws Exception {
        mockMvc.perform(get("/negociacoes").param("clienteId", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Valor inválido para o parâmetro 'clienteId'"));
    }
}
