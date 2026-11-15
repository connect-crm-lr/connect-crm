package br.com.lrrepresentacoes.connectcrm.service;

import br.com.lrrepresentacoes.connectcrm.dto.PedidoResponse;
import br.com.lrrepresentacoes.connectcrm.dto.TotalPedidosResponse;
import br.com.lrrepresentacoes.connectcrm.exception.RecursoNaoEncontradoException;
import br.com.lrrepresentacoes.connectcrm.exception.RegraNegocioException;
import br.com.lrrepresentacoes.connectcrm.model.Cliente;
import br.com.lrrepresentacoes.connectcrm.model.Estoque;
import br.com.lrrepresentacoes.connectcrm.model.FormaPagamento;
import br.com.lrrepresentacoes.connectcrm.model.Pedido;
import br.com.lrrepresentacoes.connectcrm.model.StatusPedido;
import br.com.lrrepresentacoes.connectcrm.model.Tecido;
import br.com.lrrepresentacoes.connectcrm.model.Usuario;
import br.com.lrrepresentacoes.connectcrm.repository.PedidoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Consultas de pedidos (#71): listar com filtros e buscar um pedido com os itens. */
@ExtendWith(MockitoExtension.class)
class PedidoListagemServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @InjectMocks
    private PedidoService pedidoService;

    private static final Usuario REVENDEDOR = new Usuario("Revendedor", "revendedor@exemplo.com", "hash");

    private static Pedido pedido(Long id, StatusPedido status) {
        Cliente cliente = new Cliente(REVENDEDOR, "Ateliê Bella Noiva", "5562999998888", null, null, "Goiânia", "GO");
        ReflectionTestUtils.setField(cliente, "id", 1L);
        Tecido linho = new Tecido("Linho Natural", null, null, null, new BigDecimal("42.50"), null);
        Estoque linha = new Estoque(linho, "Natural");
        linha.somar(new BigDecimal("50.00"));
        ReflectionTestUtils.setField(linha, "id", 10L);

        Pedido pedido = new Pedido(cliente, REVENDEDOR, null, FormaPagamento.BOLETO, false,
                LocalDateTime.of(2026, 11, 3, 10, 0));
        pedido.adicionarItem(linha, new BigDecimal("10.00"), linho.getPrecoMetro());
        ReflectionTestUtils.setField(pedido, "id", id);
        ReflectionTestUtils.setField(pedido, "status", status);
        return pedido;
    }

    @Test
    @DisplayName("listar repassa os filtros de cliente e status ao repository")
    void listar_repassaOsFiltros() {
        when(pedidoRepository.listar(1L, StatusPedido.EM_ABERTO, null, null))
                .thenReturn(List.of(pedido(7L, StatusPedido.EM_ABERTO)));

        List<PedidoResponse> resultado = pedidoService.listar(1L, StatusPedido.EM_ABERTO, null, null);

        assertThat(resultado).hasSize(1);
        verify(pedidoRepository).listar(1L, StatusPedido.EM_ABERTO, null, null);
    }

    // ---------- período (#49) ----------

    @Test
    @DisplayName("período: início vira 00:00 do dia e o fim é exclusivo no dia seguinte (fim inclusivo)")
    void listar_periodo_fimInclusivo() {
        LocalDate inicio = LocalDate.of(2026, 11, 1);
        LocalDate fim = LocalDate.of(2026, 11, 30);
        when(pedidoRepository.listar(null, null, LocalDateTime.of(2026, 11, 1, 0, 0),
                LocalDateTime.of(2026, 12, 1, 0, 0))).thenReturn(List.of(pedido(7L, StatusPedido.PAGO)));

        List<PedidoResponse> resultado = pedidoService.listar(null, null, inicio, fim);

        assertThat(resultado).hasSize(1);
    }

    @Test
    @DisplayName("período só com início ou só com fim: o outro limite fica aberto (null)")
    void listar_periodoAberto() {
        when(pedidoRepository.listar(null, null, LocalDateTime.of(2026, 11, 1, 0, 0), null))
                .thenReturn(List.of());
        when(pedidoRepository.listar(null, null, null, LocalDateTime.of(2026, 12, 1, 0, 0)))
                .thenReturn(List.of());

        assertThat(pedidoService.listar(null, null, LocalDate.of(2026, 11, 1), null)).isEmpty();
        assertThat(pedidoService.listar(null, null, null, LocalDate.of(2026, 11, 30))).isEmpty();
    }

    @Test
    @DisplayName("início depois do fim é recusado (422) sem consultar o banco")
    void listar_inicioDepoisDoFim_recusa() {
        assertThatThrownBy(() -> pedidoService.listar(null, null, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 11, 1)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("A data inicial não pode ser depois da data final");
        verifyNoInteractions(pedidoRepository);
    }

    @Test
    @DisplayName("total do período soma só os não cancelados e informa a quantidade")
    void total_somaSemCancelados() {
        LocalDateTime de = LocalDateTime.of(2026, 11, 1, 0, 0);
        LocalDateTime ate = LocalDateTime.of(2026, 12, 1, 0, 0);
        when(pedidoRepository.somarTotalSemCancelados(null, de, ate)).thenReturn(new BigDecimal("1250.50"));
        when(pedidoRepository.contarSemCancelados(null, de, ate)).thenReturn(3L);

        TotalPedidosResponse total = pedidoService.totalDoPeriodo(null, LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30));

        assertThat(total.total()).isEqualByComparingTo("1250.50");
        assertThat(total.quantidadePedidos()).isEqualTo(3);
    }

    @Test
    @DisplayName("total sem pedidos no período é 0,00 e quantidade 0 (soma null do banco)")
    void total_semPedidos_ehZero() {
        when(pedidoRepository.somarTotalSemCancelados(null, null, null)).thenReturn(null);
        when(pedidoRepository.contarSemCancelados(null, null, null)).thenReturn(0L);

        TotalPedidosResponse total = pedidoService.totalDoPeriodo(null, null, null);

        assertThat(total.total()).isEqualByComparingTo("0");
        assertThat(total.quantidadePedidos()).isZero();
    }

    @Test
    @DisplayName("total com status=CANCELADO é 0 e nem consulta o banco")
    void total_statusCancelado_ehZeroSemConsultar() {
        TotalPedidosResponse total = pedidoService.totalDoPeriodo(StatusPedido.CANCELADO, null, null);

        assertThat(total.total()).isEqualByComparingTo("0");
        assertThat(total.quantidadePedidos()).isZero();
        verifyNoInteractions(pedidoRepository);
    }

    @Test
    @DisplayName("total filtrado por status PAGO repassa o status à consulta")
    void total_statusPago_repassaOStatus() {
        when(pedidoRepository.somarTotalSemCancelados(StatusPedido.PAGO, null, null))
                .thenReturn(new BigDecimal("425.00"));
        when(pedidoRepository.contarSemCancelados(StatusPedido.PAGO, null, null)).thenReturn(1L);

        TotalPedidosResponse total = pedidoService.totalDoPeriodo(StatusPedido.PAGO, null, null);

        assertThat(total.total()).isEqualByComparingTo("425.00");
        assertThat(total.quantidadePedidos()).isEqualTo(1);
    }

    @Test
    @DisplayName("total com início depois do fim é recusado (422)")
    void total_inicioDepoisDoFim_recusa() {
        assertThatThrownBy(() -> pedidoService.totalDoPeriodo(null, LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 11, 1)))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    @DisplayName("listar sem filtros passa null nos dois (todos os pedidos)")
    void listar_semFiltros() {
        when(pedidoRepository.listar(null, null, null, null)).thenReturn(List.of());

        assertThat(pedidoService.listar(null, null, null, null)).isEmpty();
    }

    @Test
    @DisplayName("listar mantém a ordem do repository (mais novos primeiro)")
    void listar_mantemAOrdemDoRepository() {
        when(pedidoRepository.listar(null, null, null, null)).thenReturn(List.of(
                pedido(9L, StatusPedido.PAGO), pedido(8L, StatusPedido.EM_ABERTO), pedido(7L, StatusPedido.PAGO)));

        List<PedidoResponse> resultado = pedidoService.listar(null, null, null, null);

        assertThat(resultado).extracting(PedidoResponse::id).containsExactly(9L, 8L, 7L);
    }

    @Test
    @DisplayName("pedido CANCELADO aparece na lista com o status")
    void listar_pedidoCanceladoApareceComOStatus() {
        when(pedidoRepository.listar(null, null, null, null)).thenReturn(List.of(
                pedido(8L, StatusPedido.CANCELADO), pedido(7L, StatusPedido.PAGO)));

        List<PedidoResponse> resultado = pedidoService.listar(null, null, null, null);

        assertThat(resultado).extracting(PedidoResponse::status)
                .containsExactly(StatusPedido.CANCELADO, StatusPedido.PAGO);
    }

    @Test
    @DisplayName("buscarPorId devolve o pedido com tecido, cor, metragem, preço do metro e subtotal")
    void buscarPorId_devolveOPedidoComOsItens() {
        when(pedidoRepository.findById(7L)).thenReturn(Optional.of(pedido(7L, StatusPedido.EM_ABERTO)));

        PedidoResponse resposta = pedidoService.buscarPorId(7L);

        assertThat(resposta.id()).isEqualTo(7L);
        assertThat(resposta.cliente()).isEqualTo("Ateliê Bella Noiva");
        assertThat(resposta.total()).isEqualByComparingTo("425.00");
        assertThat(resposta.itens()).hasSize(1);
        assertThat(resposta.itens().get(0).tecido()).isEqualTo("Linho Natural");
        assertThat(resposta.itens().get(0).corCodigo()).isEqualTo("Natural");
        assertThat(resposta.itens().get(0).metragem()).isEqualByComparingTo("10.00");
        assertThat(resposta.itens().get(0).precoMetro()).isEqualByComparingTo("42.50");
        assertThat(resposta.itens().get(0).subtotal()).isEqualByComparingTo("425.00");
    }

    @Test
    @DisplayName("buscarPorId de pedido inexistente lança 'Pedido não encontrado'")
    void buscarPorId_inexistente() {
        when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoService.buscarPorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Pedido não encontrado");
    }
}
