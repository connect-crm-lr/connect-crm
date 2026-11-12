package br.com.lrrepresentacoes.connectcrm.controller;

import br.com.lrrepresentacoes.connectcrm.dto.InteracaoRequest;
import br.com.lrrepresentacoes.connectcrm.dto.InteracaoResponse;
import br.com.lrrepresentacoes.connectcrm.exception.GlobalExceptionHandler;
import br.com.lrrepresentacoes.connectcrm.exception.RecursoNaoEncontradoException;
import br.com.lrrepresentacoes.connectcrm.model.TipoInteracao;
import br.com.lrrepresentacoes.connectcrm.service.InteracaoService;
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

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa a requisição HTTP passando pelo Spring MVC: controller, validação (@Valid)
 * e GlobalExceptionHandler. O service é simulado com Mockito.
 */
@ExtendWith(MockitoExtension.class)
class InteracaoControllerTest {

    @Mock
    private InteracaoService interacaoService;

    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        mockMvc = MockMvcBuilders.standaloneSetup(new InteracaoController(interacaoService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static String corpo(String tipo, String data) {
        return "{\"tipo\": \"" + tipo + "\", \"dataInteracao\": \"" + data + "\", "
                + "\"descricao\": \"Enviei o mostruário de linho\"}";
    }

    @Test
    @DisplayName("CT-RF011-01: registrar interação válida responde 201 e repassa o cliente da rota")
    void registrar_valida_responde201() throws Exception {
        when(interacaoService.registrar(eq(7L), any(InteracaoRequest.class))).thenReturn(new InteracaoResponse(
                1L, 7L, TipoInteracao.WHATSAPP, LocalDate.of(2026, 11, 3), "Enviei o mostruário de linho"));

        mockMvc.perform(post("/clientes/7/interacoes").contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("WHATSAPP", "2026-11-03")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idCliente").value(7))
                .andExpect(jsonPath("$.tipo").value("WHATSAPP"));

        ArgumentCaptor<InteracaoRequest> captor = ArgumentCaptor.forClass(InteracaoRequest.class);
        verify(interacaoService).registrar(eq(7L), captor.capture());
        assertThat(captor.getValue().dataInteracao()).isEqualTo(LocalDate.of(2026, 11, 3));
    }

    @Test
    @DisplayName("CT-RF011-02: tipo inválido (EMAIL) responde 400 com o erro no campo tipo")
    void registrar_tipoInvalido_responde400NoCampoTipo() throws Exception {
        mockMvc.perform(post("/clientes/7/interacoes").contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("EMAIL", "2026-11-03")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("tipo"))
                .andExpect(jsonPath("$.campos[0].mensagem")
                        .value("Tipo inválido. Use WHATSAPP, LIGACAO, VISITA ou OUTRO"));
        verifyNoInteractions(interacaoService);
    }

    // Garante que a lista do @Pattern do request acompanha o enum: nenhum valor do enum é recusado.
    @ParameterizedTest
    @EnumSource(TipoInteracao.class)
    @DisplayName("CT-RF011-02: os quatro tipos do enum passam pela validação")
    void registrar_tiposDoEnum_passamPelaValidacao(TipoInteracao tipo) throws Exception {
        mockMvc.perform(post("/clientes/7/interacoes").contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(tipo.name(), "2026-11-03")))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Registrar interação sem descrição responde 400 com o erro no campo descricao")
    void registrar_semDescricao_responde400() throws Exception {
        mockMvc.perform(post("/clientes/7/interacoes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\": \"VISITA\", \"dataInteracao\": \"2026-11-03\", \"descricao\": \"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("descricao"));
        verifyNoInteractions(interacaoService);
    }

    // A interação é uma conversa já realizada: a data não pode ser futura.
    @Test
    @DisplayName("Registrar interação com data no futuro responde 400 com o erro no campo dataInteracao")
    void registrar_comDataNoFuturo_responde400() throws Exception {
        String depoisDeAmanha = LocalDate.now().plusDays(2).toString();
        mockMvc.perform(post("/clientes/7/interacoes").contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("LIGACAO", depoisDeAmanha)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("dataInteracao"));
        verifyNoInteractions(interacaoService);
    }

    @Test
    @DisplayName("Listar interações de cliente que não existe responde 404")
    void listar_clienteInexistente_responde404() throws Exception {
        when(interacaoService.listarDoCliente(9L))
                .thenThrow(new RecursoNaoEncontradoException("Cliente não encontrado"));

        mockMvc.perform(get("/clientes/9/interacoes"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Cliente não encontrado"));
    }

    @Test
    @DisplayName("Listar interações responde 200 com as interações do cliente")
    void listar_responde200() throws Exception {
        when(interacaoService.listarDoCliente(7L)).thenReturn(List.of(new InteracaoResponse(
                1L, 7L, TipoInteracao.VISITA, LocalDate.of(2026, 11, 3), "Visita ao ateliê")));

        mockMvc.perform(get("/clientes/7/interacoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("VISITA"))
                .andExpect(jsonPath("$[0].dataInteracao").value("2026-11-03"));
    }
}
