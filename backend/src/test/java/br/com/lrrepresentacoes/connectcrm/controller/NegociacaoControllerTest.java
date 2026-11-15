package br.com.lrrepresentacoes.connectcrm.controller;

import br.com.lrrepresentacoes.connectcrm.dto.MoverEtapaRequest;
import br.com.lrrepresentacoes.connectcrm.dto.NegociacaoRequest;
import br.com.lrrepresentacoes.connectcrm.dto.NegociacaoResponse;
import br.com.lrrepresentacoes.connectcrm.exception.GlobalExceptionHandler;
import br.com.lrrepresentacoes.connectcrm.exception.RecursoNaoEncontradoException;
import br.com.lrrepresentacoes.connectcrm.exception.RegraNegocioException;
import br.com.lrrepresentacoes.connectcrm.model.EstagioNegociacao;
import br.com.lrrepresentacoes.connectcrm.service.NegociacaoService;
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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
class NegociacaoControllerTest {

    @Mock
    private NegociacaoService negociacaoService;

    private MockMvc mockMvc;

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 12, 10, 0);

    private static NegociacaoResponse resposta(EstagioNegociacao estagio, LocalDateTime dataFechamento) {
        return new NegociacaoResponse(3L, 7L, "Ateliê Bella Noiva", "Mostruário de linho",
                estagio, AGORA, AGORA, dataFechamento);
    }

    @BeforeEach
    void configurar() {
        mockMvc = MockMvcBuilders.standaloneSetup(new NegociacaoController(negociacaoService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ---------- POST /negociacoes ----------

    @Test
    @DisplayName("CT-RF007-01: criar negociação responde 201 em PRIMEIRO_CONTATO")
    void criar_valida_responde201EmPrimeiroContato() throws Exception {
        when(negociacaoService.criar(any(NegociacaoRequest.class)))
                .thenReturn(resposta(EstagioNegociacao.PRIMEIRO_CONTATO, null));

        mockMvc.perform(post("/negociacoes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idCliente\": 7, \"descricao\": \"Mostruário de linho\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estagio").value("PRIMEIRO_CONTATO"))
                .andExpect(jsonPath("$.idCliente").value(7))
                .andExpect(jsonPath("$.dataFechamento").doesNotExist());
    }

    @Test
    @DisplayName("Criar negociação sem cliente responde 400 com o erro no campo idCliente")
    void criar_semCliente_responde400() throws Exception {
        mockMvc.perform(post("/negociacoes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\": \"Mostruário de linho\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("idCliente"))
                .andExpect(jsonPath("$.campos[0].mensagem").value("O cliente é obrigatório"));
        verifyNoInteractions(negociacaoService);
    }

    @Test
    @DisplayName("Criar negociação sem descrição responde 400 com o erro no campo descricao")
    void criar_semDescricao_responde400() throws Exception {
        mockMvc.perform(post("/negociacoes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idCliente\": 7, \"descricao\": \"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("descricao"));
        verifyNoInteractions(negociacaoService);
    }

    @Test
    @DisplayName("CT-RD014-01: criar negociação para cliente desativado responde 422 com a mensagem")
    void criar_clienteDesativado_responde422() throws Exception {
        String mensagem = "O cliente Ateliê Bella Noiva está desativado. "
                + "Reative o cliente antes de criar a negociação.";
        when(negociacaoService.criar(any(NegociacaoRequest.class))).thenThrow(new RegraNegocioException(mensagem));

        mockMvc.perform(post("/negociacoes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idCliente\": 7, \"descricao\": \"Mostruário de linho\"}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.mensagem").value(mensagem));
    }

    // ---------- PATCH /negociacoes/{id}/etapa ----------

    @Test
    @DisplayName("CT-RD010-02: mover para FECHADA responde 200 com a data de fechamento")
    void moverEtapa_paraFechada_responde200ComDataFechamento() throws Exception {
        when(negociacaoService.moverEtapa(eq(3L), any(MoverEtapaRequest.class)))
                .thenReturn(resposta(EstagioNegociacao.FECHADA, AGORA));

        mockMvc.perform(patch("/negociacoes/3/etapa").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estagio\": \"FECHADA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estagio").value("FECHADA"))
                .andExpect(jsonPath("$.dataFechamento").exists());

        ArgumentCaptor<MoverEtapaRequest> captor = ArgumentCaptor.forClass(MoverEtapaRequest.class);
        verify(negociacaoService).moverEtapa(eq(3L), captor.capture());
        assertThat(captor.getValue().estagio()).isEqualTo("FECHADA");
    }

    @Test
    @DisplayName("Mover para um estágio que não existe responde 400 com o erro no campo estagio")
    void moverEtapa_estagioInvalido_responde400NoCampoEstagio() throws Exception {
        mockMvc.perform(patch("/negociacoes/3/etapa").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estagio\": \"OPORTUNIDADE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("estagio"));
        verifyNoInteractions(negociacaoService);
    }

    // Garante que a lista do @Pattern do request acompanha o enum: nenhum valor do enum é recusado.
    @ParameterizedTest
    @EnumSource(EstagioNegociacao.class)
    @DisplayName("Os cinco estágios do enum passam pela validação")
    void moverEtapa_estagiosDoEnum_passamPelaValidacao(EstagioNegociacao estagio) throws Exception {
        mockMvc.perform(patch("/negociacoes/3/etapa").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estagio\": \"" + estagio.name() + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Mover para a etapa atual responde 422 com a mensagem")
    void moverEtapa_mesmaEtapa_responde422() throws Exception {
        when(negociacaoService.moverEtapa(eq(3L), any(MoverEtapaRequest.class)))
                .thenThrow(new RegraNegocioException("A negociação já está na etapa FECHADA"));

        mockMvc.perform(patch("/negociacoes/3/etapa").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estagio\": \"FECHADA\"}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.mensagem").value("A negociação já está na etapa FECHADA"));
    }

    @Test
    @DisplayName("Mover negociação que não existe responde 404")
    void moverEtapa_inexistente_responde404() throws Exception {
        when(negociacaoService.moverEtapa(eq(9L), any(MoverEtapaRequest.class)))
                .thenThrow(new RecursoNaoEncontradoException("Negociação não encontrada"));

        mockMvc.perform(patch("/negociacoes/9/etapa").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estagio\": \"PERDIDA\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Negociação não encontrada"));
    }
}
