package br.com.lrrepresentacoes.connectcrm.controller;

import br.com.lrrepresentacoes.connectcrm.dto.FollowUpRequest;
import br.com.lrrepresentacoes.connectcrm.dto.FollowUpResponse;
import br.com.lrrepresentacoes.connectcrm.exception.GlobalExceptionHandler;
import br.com.lrrepresentacoes.connectcrm.exception.RecursoNaoEncontradoException;
import br.com.lrrepresentacoes.connectcrm.exception.RegraNegocioException;
import br.com.lrrepresentacoes.connectcrm.service.FollowUpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
class FollowUpControllerTest {

    @Mock
    private FollowUpService followUpService;

    private MockMvc mockMvc;

    private static final FollowUpResponse PENDENTE = new FollowUpResponse(
            5L, 7L, "Ateliê Bella Noiva", LocalDate.of(2026, 11, 20), "Retornar sobre o mostruário", null);

    @BeforeEach
    void configurar() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FollowUpController(followUpService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("CT-RF012-01: criar follow-up válido responde 201 com data_conclusao vazia")
    void criar_valido_responde201Pendente() throws Exception {
        when(followUpService.criar(eq(7L), any(FollowUpRequest.class))).thenReturn(PENDENTE);

        mockMvc.perform(post("/clientes/7/follow-ups").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dataPrevista\": \"2026-11-20\", \"descricao\": \"Retornar sobre o mostruário\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idCliente").value(7))
                .andExpect(jsonPath("$.dataPrevista").value("2026-11-20"))
                .andExpect(jsonPath("$.dataConclusao").doesNotExist());
    }

    @Test
    @DisplayName("CT-RF012-02: criar follow-up sem data responde 400 com o erro no campo dataPrevista")
    void criar_semData_responde400NoCampoDataPrevista() throws Exception {
        mockMvc.perform(post("/clientes/7/follow-ups").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\": \"Retornar sobre o mostruário\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("dataPrevista"))
                .andExpect(jsonPath("$.campos[0].mensagem").value("A data prevista é obrigatória"));
        verifyNoInteractions(followUpService);
    }

    @Test
    @DisplayName("Criar follow-up sem descrição responde 400 com o erro no campo descricao")
    void criar_semDescricao_responde400() throws Exception {
        mockMvc.perform(post("/clientes/7/follow-ups").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dataPrevista\": \"2026-11-20\", \"descricao\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("descricao"));
        verifyNoInteractions(followUpService);
    }

    @Test
    @DisplayName("CT-RF012-03: concluir follow-up responde 200 com a data de conclusão")
    void concluir_responde200ComADataDeConclusao() throws Exception {
        when(followUpService.concluir(5L)).thenReturn(new FollowUpResponse(5L, 7L, "Ateliê Bella Noiva",
                LocalDate.of(2026, 11, 20), "Retornar sobre o mostruário", LocalDateTime.of(2026, 11, 20, 10, 15)));

        mockMvc.perform(patch("/follow-ups/5/concluir"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataConclusao").exists());
    }

    @Test
    @DisplayName("Concluir follow-up já concluído responde 422 com a mensagem")
    void concluir_jaConcluido_responde422() throws Exception {
        when(followUpService.concluir(5L)).thenThrow(new RegraNegocioException("O follow-up já foi concluído"));

        mockMvc.perform(patch("/follow-ups/5/concluir"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.mensagem").value("O follow-up já foi concluído"));
    }

    @Test
    @DisplayName("Concluir follow-up que não existe responde 404")
    void concluir_inexistente_responde404() throws Exception {
        when(followUpService.concluir(9L)).thenThrow(new RecursoNaoEncontradoException("Follow-up não encontrado"));

        mockMvc.perform(patch("/follow-ups/9/concluir"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Follow-up não encontrado"));
    }

    // "pendencias" não pode ser lido como o {id} de /follow-ups/{id}/concluir.
    @Test
    @DisplayName("CT-RD011-01: GET /follow-ups/pendencias responde 200 com as pendências de hoje")
    void listarPendencias_responde200() throws Exception {
        when(followUpService.listarPendenciasDeHoje()).thenReturn(List.of(PENDENTE));

        mockMvc.perform(get("/follow-ups/pendencias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cliente").value("Ateliê Bella Noiva"))
                .andExpect(jsonPath("$[0].id").value(5));
    }

    @Test
    @DisplayName("Listar follow-ups do cliente responde 200")
    void listar_responde200() throws Exception {
        when(followUpService.listarDoCliente(7L)).thenReturn(List.of(PENDENTE));

        mockMvc.perform(get("/clientes/7/follow-ups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dataPrevista").value("2026-11-20"));
    }
}
