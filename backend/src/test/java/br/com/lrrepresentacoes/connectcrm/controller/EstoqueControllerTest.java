package br.com.lrrepresentacoes.connectcrm.controller;

import br.com.lrrepresentacoes.connectcrm.exception.GlobalExceptionHandler;
import br.com.lrrepresentacoes.connectcrm.exception.RegraNegocioException;
import br.com.lrrepresentacoes.connectcrm.dto.EntradaEstoqueRequest;
import br.com.lrrepresentacoes.connectcrm.dto.EntradaEstoqueResponse;
import br.com.lrrepresentacoes.connectcrm.service.EstoqueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
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
class EstoqueControllerTest {

    @Mock
    private EstoqueService estoqueService;

    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        mockMvc = MockMvcBuilders.standaloneSetup(new EstoqueController(estoqueService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void registrarEntrada_semMetragem_responde400ComOErroDoCampo() throws Exception {
        mockMvc.perform(post("/estoque/entradas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idTecido\": 1, \"cor\": \"Natural\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("metragem"))
                .andExpect(jsonPath("$.campos[0].mensagem").value("A metragem é obrigatória"));
        verifyNoInteractions(estoqueService);
    }

    @Test
    void registrarEntrada_semCor_responde400ComOErroDoCampo() throws Exception {
        mockMvc.perform(post("/estoque/entradas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idTecido\": 1, \"metragem\": 10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("cor"))
                .andExpect(jsonPath("$.campos[0].mensagem").value("A cor é obrigatória"));
        verifyNoInteractions(estoqueService);
    }

    @Test
    void registrarEntrada_comCorEmBranco_responde400() throws Exception {
        mockMvc.perform(post("/estoque/entradas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idTecido\": 1, \"cor\": \"   \", \"metragem\": 10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("cor"));
        verifyNoInteractions(estoqueService);
    }

    @Test
    void registrarEntrada_comCorMaiorQue50_responde400() throws Exception {
        mockMvc.perform(post("/estoque/entradas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idTecido\": 1, \"cor\": \"" + "a".repeat(51) + "\", \"metragem\": 10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("cor"));
        verifyNoInteractions(estoqueService);
    }

    @Test
    void registrarEntrada_comCodigoMaiorQue20_responde400() throws Exception {
        mockMvc.perform(post("/estoque/entradas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idTecido\": 1, \"cor\": \"Marinho\", \"codigo\": \"" + "1".repeat(21)
                                + "\", \"metragem\": 10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("codigo"));
        verifyNoInteractions(estoqueService);
    }

    // O código é opcional: só cor e metragem bastam, e o JSON de resposta traz codigo, cor e descricaoCor.
    @Test
    void registrarEntrada_semCodigo_aceitaEResponde201ComOContratoNovo() throws Exception {
        when(estoqueService.registrarEntrada(any(EntradaEstoqueRequest.class))).thenReturn(
                new EntradaEstoqueResponse(1L, 2L, "Linho Natural", null, "Marinho", "Marinho",
                        new BigDecimal("10.00"), LocalDate.of(2026, 11, 3), new BigDecimal("10.00")));

        mockMvc.perform(post("/estoque/entradas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idTecido\": 1, \"cor\": \"Marinho\", \"metragem\": 10}"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.codigo").doesNotExist())
                .andExpect(jsonPath("$.cor").value("Marinho"))
                .andExpect(jsonPath("$.descricaoCor").value("Marinho"))
                .andExpect(jsonPath("$.corCodigo").doesNotExist());
    }

    @Test
    void registrarEntrada_comMetragemZero_responde400() throws Exception {
        mockMvc.perform(post("/estoque/entradas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idTecido\": 1, \"cor\": \"Natural\", \"metragem\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("metragem"));
        verifyNoInteractions(estoqueService);
    }

    @Test
    void registrarEntrada_comDataNoFuturo_responde400() throws Exception {
        String amanha = LocalDate.now().plusDays(2).toString();
        mockMvc.perform(post("/estoque/entradas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idTecido\": 1, \"cor\": \"Natural\", \"metragem\": 10, "
                                + "\"dataEntrada\": \"" + amanha + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("dataEntrada"));
        verifyNoInteractions(estoqueService);
    }

    // CT-RD012-01 na API: a mensagem da regra chega ao front com 422.
    @Test
    void registrarEntrada_tecidoDesativado_responde422ComAMensagem() throws Exception {
        String mensagem = "O tecido Linho Natural está desativado. Reative o tecido antes de registrar a entrada.";
        when(estoqueService.registrarEntrada(any(EntradaEstoqueRequest.class)))
                .thenThrow(new RegraNegocioException(mensagem));

        mockMvc.perform(post("/estoque/entradas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idTecido\": 1, \"cor\": \"Natural\", \"metragem\": 10}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.mensagem").value(mensagem));
    }

    @Test
    void listar_comBaixoTrue_repassaParaOService() throws Exception {
        when(estoqueService.listar(null, true)).thenReturn(List.of());

        mockMvc.perform(get("/estoque").param("baixo", "true"))
                .andExpect(status().isOk());

        verify(estoqueService).listar(null, true);
    }
}
