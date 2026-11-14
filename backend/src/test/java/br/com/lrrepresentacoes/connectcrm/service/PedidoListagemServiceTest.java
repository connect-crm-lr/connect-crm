package br.com.lrrepresentacoes.connectcrm.service;

import br.com.lrrepresentacoes.connectcrm.dto.PedidoResponse;
import br.com.lrrepresentacoes.connectcrm.exception.RecursoNaoEncontradoException;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
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
        when(pedidoRepository.listar(1L, StatusPedido.EM_ABERTO))
                .thenReturn(List.of(pedido(7L, StatusPedido.EM_ABERTO)));

        List<PedidoResponse> resultado = pedidoService.listar(1L, StatusPedido.EM_ABERTO);

        assertThat(resultado).hasSize(1);
        verify(pedidoRepository).listar(1L, StatusPedido.EM_ABERTO);
    }

    @Test
    @DisplayName("listar sem filtros passa null nos dois (todos os pedidos)")
    void listar_semFiltros() {
        when(pedidoRepository.listar(null, null)).thenReturn(List.of());

        assertThat(pedidoService.listar(null, null)).isEmpty();
    }

    @Test
    @DisplayName("listar mantém a ordem do repository (mais novos primeiro)")
    void listar_mantemAOrdemDoRepository() {
        when(pedidoRepository.listar(null, null)).thenReturn(List.of(
                pedido(9L, StatusPedido.PAGO), pedido(8L, StatusPedido.EM_ABERTO), pedido(7L, StatusPedido.PAGO)));

        List<PedidoResponse> resultado = pedidoService.listar(null, null);

        assertThat(resultado).extracting(PedidoResponse::id).containsExactly(9L, 8L, 7L);
    }

    @Test
    @DisplayName("pedido CANCELADO aparece na lista com o status")
    void listar_pedidoCanceladoApareceComOStatus() {
        when(pedidoRepository.listar(null, null)).thenReturn(List.of(
                pedido(8L, StatusPedido.CANCELADO), pedido(7L, StatusPedido.PAGO)));

        List<PedidoResponse> resultado = pedidoService.listar(null, null);

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
