package br.com.lrrepresentacoes.connectcrm.controller;

import br.com.lrrepresentacoes.connectcrm.dto.DashboardResponse;
import br.com.lrrepresentacoes.connectcrm.dto.EstoqueResponse;
import br.com.lrrepresentacoes.connectcrm.dto.FollowUpResponse;
import br.com.lrrepresentacoes.connectcrm.exception.GlobalExceptionHandler;
import br.com.lrrepresentacoes.connectcrm.model.EstagioNegociacao;
import br.com.lrrepresentacoes.connectcrm.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa a requisição HTTP passando pelo Spring MVC: rota e formato do JSON.
 * O service é simulado com Mockito.
 */
@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private DashboardService dashboardService;

    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        mockMvc = MockMvcBuilders.standaloneSetup(new DashboardController(dashboardService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void resumo_respondeOsQuatroBlocosDoPainel() throws Exception {
        Map<EstagioNegociacao, Long> porEtapa = new EnumMap<>(EstagioNegociacao.class);
        for (EstagioNegociacao estagio : EstagioNegociacao.values()) {
            porEtapa.put(estagio, 0L);
        }
        porEtapa.put(EstagioNegociacao.AGUARDANDO_RESPOSTA, 3L);
        LocalDate hoje = LocalDate.of(2026, 11, 14);
        when(dashboardService.resumo()).thenReturn(new DashboardResponse(
                new BigDecimal("1530.50"), 4L, porEtapa,
                List.of(new FollowUpResponse(7L, 2L, "Ateliê Bella Noiva", hoje, "Retornar", null)),
                List.of(new EstoqueResponse(5L, 1L, "Linho Natural", "Bege 102", new BigDecimal("18.00"), true))));

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendasMes").value(1530.50))
                .andExpect(jsonPath("$.pedidosMes").value(4))
                .andExpect(jsonPath("$.negociacoesPorEtapa.PRIMEIRO_CONTATO").value(0))
                .andExpect(jsonPath("$.negociacoesPorEtapa.AGUARDANDO_RESPOSTA").value(3))
                .andExpect(jsonPath("$.negociacoesPorEtapa.PERDIDA").value(0))
                .andExpect(jsonPath("$.pendenciasHoje[0].cliente").value("Ateliê Bella Noiva"))
                .andExpect(jsonPath("$.estoqueBaixo[0].corCodigo").value("Bege 102"))
                .andExpect(jsonPath("$.estoqueBaixo[0].estoqueBaixo").value(true));
    }
}
