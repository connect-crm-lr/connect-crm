package br.com.lrrepresentacoes.connectcrm.service;

import br.com.lrrepresentacoes.connectcrm.dto.DashboardResponse;
import br.com.lrrepresentacoes.connectcrm.model.Cliente;
import br.com.lrrepresentacoes.connectcrm.model.EstagioNegociacao;
import br.com.lrrepresentacoes.connectcrm.model.Estoque;
import br.com.lrrepresentacoes.connectcrm.model.FollowUp;
import br.com.lrrepresentacoes.connectcrm.model.Tecido;
import br.com.lrrepresentacoes.connectcrm.model.Usuario;
import br.com.lrrepresentacoes.connectcrm.repository.EstoqueRepository;
import br.com.lrrepresentacoes.connectcrm.repository.FollowUpRepository;
import br.com.lrrepresentacoes.connectcrm.repository.NegociacaoRepository;
import br.com.lrrepresentacoes.connectcrm.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private NegociacaoRepository negociacaoRepository;

    @Mock
    private FollowUpRepository followUpRepository;

    @Mock
    private EstoqueRepository estoqueRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private static final Usuario REVENDEDOR = new Usuario("Revendedor", "revendedor@exemplo.com", "hash");
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 14);

    private void semDados() {
        when(pedidoRepository.somarVendasEntre(any(), any())).thenReturn(BigDecimal.ZERO);
        when(pedidoRepository.contarVendasEntre(any(), any())).thenReturn(0L);
        when(negociacaoRepository.contarPorEstagio()).thenReturn(List.of());
        when(followUpRepository.listarPendencias(any())).thenReturn(List.of());
        when(estoqueRepository.listarAbaixoDe(any())).thenReturn(List.of());
    }

    // CT-RF021-01: vendas do mês = do dia 1 às 00:00 até o dia 1 do mês seguinte (fim exclusivo).
    @Test
    void resumo_consultaAsVendasDoMesAtualComFimExclusivo() {
        semDados();
        when(pedidoRepository.somarVendasEntre(any(), any())).thenReturn(new BigDecimal("1530.5"));
        when(pedidoRepository.contarVendasEntre(any(), any())).thenReturn(3L);

        DashboardResponse resposta = dashboardService.resumo(HOJE);

        LocalDateTime inicio = LocalDateTime.of(2026, 11, 1, 0, 0);
        LocalDateTime fim = LocalDateTime.of(2026, 12, 1, 0, 0);
        verify(pedidoRepository).somarVendasEntre(inicio, fim);
        verify(pedidoRepository).contarVendasEntre(inicio, fim);
        assertThat(resposta.vendasMes()).isEqualByComparingTo("1530.50");
        assertThat(resposta.vendasMes().scale()).isEqualTo(2);
        assertThat(resposta.pedidosMes()).isEqualTo(3);
    }

    // Virada de ano: dezembro vai até 01/01 do ano seguinte.
    @Test
    void resumo_emDezembro_terminaNoPrimeiroDiaDoAnoSeguinte() {
        semDados();

        dashboardService.resumo(LocalDate.of(2026, 12, 31));

        verify(pedidoRepository).somarVendasEntre(LocalDateTime.of(2026, 12, 1, 0, 0),
                LocalDateTime.of(2027, 1, 1, 0, 0));
    }

    @Test
    void resumo_semVendas_mostraZeroComDuasCasas() {
        semDados();

        DashboardResponse resposta = dashboardService.resumo(HOJE);

        assertThat(resposta.vendasMes()).isEqualByComparingTo("0.00");
        assertThat(resposta.vendasMes().scale()).isEqualTo(2);
    }

    // CT-RF021-02: as 5 etapas aparecem na ordem do funil, as vazias com zero.
    @Test
    void resumo_negociacoesPorEtapa_trazAsCincoEtapasNaOrdemDoFunil() {
        semDados();
        when(negociacaoRepository.contarPorEstagio()).thenReturn(List.of(
                new Object[]{EstagioNegociacao.FECHADA, 4L},
                new Object[]{EstagioNegociacao.PRIMEIRO_CONTATO, 2L}));

        DashboardResponse resposta = dashboardService.resumo(HOJE);

        assertThat(resposta.negociacoesPorEtapa()).containsExactly(
                entry(EstagioNegociacao.PRIMEIRO_CONTATO, 2L),
                entry(EstagioNegociacao.MOSTRUARIO_ENVIADO, 0L),
                entry(EstagioNegociacao.AGUARDANDO_RESPOSTA, 0L),
                entry(EstagioNegociacao.FECHADA, 4L),
                entry(EstagioNegociacao.PERDIDA, 0L));
    }

    // CT-RD011: pendências de hoje usam a consulta que já filtra concluídos, futuros e cliente inativo.
    @Test
    void resumo_pendencias_consultaComADataDeHojeEConverte() {
        semDados();
        Cliente cliente = new Cliente(REVENDEDOR, "Ateliê Bella Noiva", "5562999998888", null, null,
                "Goiânia", "GO");
        when(followUpRepository.listarPendencias(HOJE)).thenReturn(List.of(
                new FollowUp(cliente, REVENDEDOR, HOJE.minusDays(1), "Retornar sobre o mostruário")));

        DashboardResponse resposta = dashboardService.resumo(HOJE);

        verify(followUpRepository).listarPendencias(HOJE);
        assertThat(resposta.pendenciasHoje()).singleElement().satisfies(p -> {
            assertThat(p.cliente()).isEqualTo("Ateliê Bella Noiva");
            assertThat(p.dataPrevista()).isEqualTo(HOJE.minusDays(1));
        });
    }

    // CT-RF021-03: estoque baixo = metragem abaixo de 20 m (regra 6.6).
    @Test
    void resumo_estoqueBaixo_usaOLimiteDe20MetrosEMarcaALinha() {
        semDados();
        Tecido linho = new Tecido("Linho Natural", "100% Linho", 185, new BigDecimal("1.40"),
                new BigDecimal("42.50"), null);
        Estoque linha = new Estoque(linho, "102", "Bege");
        linha.somar(new BigDecimal("18.00"));
        when(estoqueRepository.listarAbaixoDe(new BigDecimal("20"))).thenReturn(List.of(linha));

        DashboardResponse resposta = dashboardService.resumo(HOJE);

        verify(estoqueRepository).listarAbaixoDe(new BigDecimal("20"));
        assertThat(resposta.estoqueBaixo()).singleElement().satisfies(e -> {
            assertThat(e.tecido()).isEqualTo("Linho Natural");
            assertThat(e.descricaoCor()).isEqualTo("102 Bege");
            assertThat(e.estoqueBaixo()).isTrue();
        });
    }

    private static java.util.Map.Entry<EstagioNegociacao, Long> entry(EstagioNegociacao estagio, long total) {
        return java.util.Map.entry(estagio, total);
    }
}
