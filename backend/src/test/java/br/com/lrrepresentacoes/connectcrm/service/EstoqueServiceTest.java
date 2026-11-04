package br.com.lrrepresentacoes.connectcrm.service;

import br.com.lrrepresentacoes.connectcrm.dto.EntradaEstoqueRequest;
import br.com.lrrepresentacoes.connectcrm.dto.EntradaEstoqueResponse;
import br.com.lrrepresentacoes.connectcrm.dto.EstoqueResponse;
import br.com.lrrepresentacoes.connectcrm.exception.RecursoNaoEncontradoException;
import br.com.lrrepresentacoes.connectcrm.exception.RegraNegocioException;
import br.com.lrrepresentacoes.connectcrm.model.EntradaEstoque;
import br.com.lrrepresentacoes.connectcrm.model.Estoque;
import br.com.lrrepresentacoes.connectcrm.model.Tecido;
import br.com.lrrepresentacoes.connectcrm.repository.EntradaEstoqueRepository;
import br.com.lrrepresentacoes.connectcrm.repository.EstoqueRepository;
import br.com.lrrepresentacoes.connectcrm.repository.TecidoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstoqueServiceTest {

    @Mock
    private EstoqueRepository estoqueRepository;

    @Mock
    private EntradaEstoqueRepository entradaEstoqueRepository;

    @Mock
    private TecidoRepository tecidoRepository;

    @InjectMocks
    private EstoqueService estoqueService;

    private static Tecido linho() {
        return new Tecido("Linho Natural", "100% Linho", 185, new BigDecimal("1.40"),
                new BigDecimal("42.50"), null);
    }

    private static Estoque linhaCom(Tecido tecido, String cor, String metros) {
        Estoque estoque = new Estoque(tecido, cor);
        estoque.somar(new BigDecimal(metros));
        return estoque;
    }

    // CT-RF010-01: entrada numa linha que já existe soma ao saldo e grava o histórico.
    @Test
    void registrarEntrada_linhaExistente_somaAoSaldoEGravaAEntrada() {
        Tecido tecido = linho();
        Estoque linha = linhaCom(tecido, "Natural", "15.00");
        when(tecidoRepository.findById(1L)).thenReturn(Optional.of(tecido));
        when(estoqueRepository.buscarParaAtualizar(any(), eq("Natural"))).thenReturn(Optional.of(linha));
        when(entradaEstoqueRepository.save(any(EntradaEstoque.class))).thenAnswer(inv -> inv.getArgument(0));

        EntradaEstoqueResponse resposta = estoqueService.registrarEntrada(
                new EntradaEstoqueRequest(1L, " Natural ", new BigDecimal("50.00"), LocalDate.of(2026, 11, 3)));

        assertThat(linha.getMetragem()).isEqualByComparingTo("65.00");
        assertThat(resposta.saldoAtual()).isEqualByComparingTo("65.00");
        assertThat(resposta.metragem()).isEqualByComparingTo("50.00");
        assertThat(resposta.dataEntrada()).isEqualTo(LocalDate.of(2026, 11, 3));
        verify(estoqueRepository, never()).save(any(Estoque.class));
    }

    // CT-RD004-01: cor nova cria a linha com 0 e soma a entrada.
    @Test
    void registrarEntrada_corNova_criaALinhaComZeroESoma() {
        Tecido tecido = linho();
        when(tecidoRepository.findById(1L)).thenReturn(Optional.of(tecido));
        when(estoqueRepository.buscarParaAtualizar(any(), eq("Cru"))).thenReturn(Optional.empty());
        when(estoqueRepository.save(any(Estoque.class))).thenAnswer(inv -> inv.getArgument(0));
        when(entradaEstoqueRepository.save(any(EntradaEstoque.class))).thenAnswer(inv -> inv.getArgument(0));

        EntradaEstoqueResponse resposta = estoqueService.registrarEntrada(
                new EntradaEstoqueRequest(1L, "Cru", new BigDecimal("30.50"), null));

        ArgumentCaptor<Estoque> captor = ArgumentCaptor.forClass(Estoque.class);
        verify(estoqueRepository).save(captor.capture());
        assertThat(captor.getValue().getCorCodigo()).isEqualTo("Cru");
        assertThat(resposta.saldoAtual()).isEqualByComparingTo("30.50");
    }

    // CT-RF010-02 (regra no back): sem data, usa hoje no fuso de São Paulo.
    @Test
    void registrarEntrada_semData_usaADataDeHojeNoFusoDeSaoPaulo() {
        Tecido tecido = linho();
        when(tecidoRepository.findById(1L)).thenReturn(Optional.of(tecido));
        when(estoqueRepository.buscarParaAtualizar(any(), eq("Natural")))
                .thenReturn(Optional.of(linhaCom(tecido, "Natural", "0")));
        when(entradaEstoqueRepository.save(any(EntradaEstoque.class))).thenAnswer(inv -> inv.getArgument(0));

        EntradaEstoqueResponse resposta = estoqueService.registrarEntrada(
                new EntradaEstoqueRequest(1L, "Natural", BigDecimal.TEN, null));

        assertThat(resposta.dataEntrada()).isEqualTo(LocalDate.now(ZoneId.of("America/Sao_Paulo")));
    }

    // CT-RD012-01: tecido desativado é recusado e nada é gravado.
    @Test
    void registrarEntrada_tecidoDesativado_recusaSemGravarNada() {
        Tecido tecido = linho();
        tecido.desativar();
        when(tecidoRepository.findById(1L)).thenReturn(Optional.of(tecido));

        assertThatThrownBy(() -> estoqueService.registrarEntrada(
                new EntradaEstoqueRequest(1L, "Natural", BigDecimal.TEN, null)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("O tecido Linho Natural está desativado. Reative o tecido antes de registrar a entrada.");
        verify(entradaEstoqueRepository, never()).save(any(EntradaEstoque.class));
        verify(estoqueRepository, never()).save(any(Estoque.class));
    }

    @Test
    void registrarEntrada_tecidoInexistente_lancaRecursoNaoEncontrado() {
        when(tecidoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> estoqueService.registrarEntrada(
                new EntradaEstoqueRequest(99L, "Natural", BigDecimal.TEN, null)))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Tecido não encontrado");
    }

    // CT-RF009-02: valor-limite do estoque baixo (< 20 m).
    @Test
    void listar_marcaEstoqueBaixoSoAbaixoDe20Metros() {
        Tecido tecido = linho();
        when(estoqueRepository.listarDeTecidosAtivos()).thenReturn(List.of(
                linhaCom(tecido, "Areia", "19.99"),
                linhaCom(tecido, "Natural", "20.00")));

        List<EstoqueResponse> resposta = estoqueService.listar(null, false);

        assertThat(resposta).extracting(EstoqueResponse::corCodigo, EstoqueResponse::estoqueBaixo)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Areia", true),
                        org.assertj.core.groups.Tuple.tuple("Natural", false));
    }

    @Test
    void listar_somenteBaixo_consultaComOLimiteDe20Metros() {
        when(estoqueRepository.listarAbaixoDe(new BigDecimal("20"))).thenReturn(List.of());

        estoqueService.listar(null, true);

        verify(estoqueRepository).listarAbaixoDe(new BigDecimal("20"));
    }

    @Test
    void listar_comBusca_montaOPadraoEmMinusculas() {
        when(estoqueRepository.buscarDeTecidosAtivos("%off-white%")).thenReturn(List.of());

        estoqueService.listar(" OFF-WHITE ", false);

        verify(estoqueRepository).buscarDeTecidosAtivos("%off-white%");
    }

    @Test
    void listarEntradas_linhaInexistente_lancaRecursoNaoEncontrado() {
        when(estoqueRepository.existsById(5L)).thenReturn(false);

        assertThatThrownBy(() -> estoqueService.listarEntradas(5L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
