package br.com.lrrepresentacoes.connectcrm.n2;

import br.com.lrrepresentacoes.connectcrm.dto.ItemPedidoRequest;
import br.com.lrrepresentacoes.connectcrm.dto.PedidoRequest;
import br.com.lrrepresentacoes.connectcrm.dto.PedidoResponse;
import br.com.lrrepresentacoes.connectcrm.exception.RegraNegocioException;
import br.com.lrrepresentacoes.connectcrm.model.Cliente;
import br.com.lrrepresentacoes.connectcrm.model.Estoque;
import br.com.lrrepresentacoes.connectcrm.model.ItemPedido;
import br.com.lrrepresentacoes.connectcrm.model.Pedido;
import br.com.lrrepresentacoes.connectcrm.model.StatusPedido;
import br.com.lrrepresentacoes.connectcrm.model.Tecido;
import br.com.lrrepresentacoes.connectcrm.model.Usuario;
import br.com.lrrepresentacoes.connectcrm.repository.ClienteRepository;
import br.com.lrrepresentacoes.connectcrm.repository.EstoqueRepository;
import br.com.lrrepresentacoes.connectcrm.repository.PedidoRepository;
import br.com.lrrepresentacoes.connectcrm.service.PedidoService;
import br.com.lrrepresentacoes.connectcrm.service.UsuarioLogadoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Avaliação N2 (Teste de Software) — casos unitários U-01, U-02 e U-03.
 *
 * Regra testada: confirmar pedido x estoque (RF-008, RD-003, RD-004 e RD-005).
 * - o total do pedido é calculado pelo sistema (soma de metragem × preço do metro);
 * - a metragem vendida é descontada da linha de estoque (tecido + cor);
 * - o pedido é recusado quando a metragem passa do saldo, sem gravar nada.
 *
 * Unitário: só o PedidoService é real. Os repositórios e o usuário logado são simulados
 * com Mockito, então o teste não usa banco, rede nem Spring.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("N2 — Pedido x estoque (unitários)")
class PedidoServiceN2Test {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private EstoqueRepository estoqueRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private UsuarioLogadoService usuarioLogadoService;

    @InjectMocks
    private PedidoService pedidoService;

    private static final Usuario REVENDEDOR = new Usuario("Revendedor", "revendedor@exemplo.com", "hash");

    // ---------- dados de exemplo (tecidos Tricoline do revendedor) ----------

    private static Tecido tecido(String nome, String precoMetro) {
        return new Tecido(nome, "100% Algodão", 110, new BigDecimal("1.50"), new BigDecimal(precoMetro), null);
    }

    private static Estoque linha(Long id, Tecido tecido, String cor, String metros) {
        Estoque estoque = new Estoque(tecido, cor);
        estoque.somar(new BigDecimal(metros));
        ReflectionTestUtils.setField(estoque, "id", id);
        return estoque;
    }

    private static ItemPedidoRequest item(Long idEstoque, String metragem) {
        return new ItemPedidoRequest(idEstoque, new BigDecimal(metragem));
    }

    private static PedidoRequest pedido(ItemPedidoRequest... itens) {
        return new PedidoRequest(1L, null, "BOLETO", false, List.of(itens));
    }

    private void clienteAtivo() {
        Cliente cliente = new Cliente(REVENDEDOR, "Boutique Tecidos da Serra", "5562993456789",
                null, null, "Goiânia", "GO");
        ReflectionTestUtils.setField(cliente, "id", 1L);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
    }

    private void estoqueTravado(Estoque... linhas) {
        when(estoqueRepository.buscarPorIdsParaAtualizar(anyCollection())).thenReturn(List.of(linhas));
    }

    private void gravacaoLiberada() {
        when(usuarioLogadoService.obter()).thenReturn(REVENDEDOR);
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    // ---------- U-01: funcionamento esperado ----------

    @Test
    @DisplayName("U-01: pedido com estoque suficiente calcula o total (350,45) e desconta a metragem de cada linha")
    void u01_pedidoComEstoqueSuficiente_calculaTotalEDescontaEstoque() {
        clienteAtivo();
        gravacaoLiberada();
        Estoque lisaBege = linha(10L, tecido("Tricoline Lisa", "21.90"), "07 Bege", "40.00");
        Estoque listradaAzul = linha(11L, tecido("Tricoline Listrada", "23.90"), "05 Azul e branco", "20.00");
        estoqueTravado(lisaBege, listradaAzul);

        // 10 m × R$ 21,90 = 219,00  +  5,5 m × R$ 23,90 = 131,45  →  total R$ 350,45
        PedidoResponse resposta = pedidoService.confirmar(pedido(item(10L, "10.00"), item(11L, "5.50")));

        assertThat(resposta.total()).isEqualByComparingTo("350.45");
        assertThat(resposta.status()).isEqualTo(StatusPedido.EM_ABERTO);
        assertThat(lisaBege.getMetragem()).isEqualByComparingTo("30.00");
        assertThat(listradaAzul.getMetragem()).isEqualByComparingTo("14.50");

        // O item guarda o preço do metro do dia da venda (RD-006).
        ArgumentCaptor<Pedido> gravado = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(gravado.capture());
        List<ItemPedido> itens = gravado.getValue().getItens();
        assertThat(itens).hasSize(2);
        assertThat(itens.get(0).getPrecoMetro()).isEqualByComparingTo("21.90");
        assertThat(itens.get(1).getSubtotal()).isEqualByComparingTo("131.45");
    }

    // ---------- U-02: erro ----------

    @Test
    @DisplayName("U-02: metragem maior que o saldo é recusada com a mensagem do saldo e nada é gravado")
    void u02_metragemMaiorQueOSaldo_recusaSemGravar() {
        clienteAtivo();
        Estoque flameCru = linha(20L, tecido("Tricoline Flame Premium", "32.90"), "02 Cru", "15.00");
        estoqueTravado(flameCru);

        assertThatThrownBy(() -> pedidoService.confirmar(pedido(item(20L, "20.00"))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Estoque insuficiente: Tricoline Flame Premium 02 Cru tem 15 m");

        assertThat(flameCru.getMetragem()).isEqualByComparingTo("15.00");
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    // ---------- U-03: limite ----------

    @Test
    @DisplayName("U-03: vender exatamente o saldo é aceito e a linha fica com 0 m (limite)")
    void u03_venderExatamenteOSaldo_aceitaEZeraALinha() {
        clienteAtivo();
        gravacaoLiberada();
        Estoque mistaBranco = linha(30L, tecido("Tricoline Mista", "16.90"), "01 Branco", "15.00");
        estoqueTravado(mistaBranco);

        PedidoResponse resposta = pedidoService.confirmar(pedido(item(30L, "15.00")));

        // 15 m × R$ 16,90 = R$ 253,50; o saldo chega a zero, mas não fica negativo.
        assertThat(resposta.total()).isEqualByComparingTo("253.50");
        assertThat(mistaBranco.getMetragem()).isEqualByComparingTo("0");
        assertThat(mistaBranco.getMetragem().signum()).isZero();
        verify(pedidoRepository).save(any(Pedido.class));
    }
}
