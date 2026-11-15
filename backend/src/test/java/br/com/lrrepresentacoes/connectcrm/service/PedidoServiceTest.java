package br.com.lrrepresentacoes.connectcrm.service;

import br.com.lrrepresentacoes.connectcrm.dto.ItemPedidoRequest;
import br.com.lrrepresentacoes.connectcrm.dto.ItemPedidoResponse;
import br.com.lrrepresentacoes.connectcrm.dto.PedidoRequest;
import br.com.lrrepresentacoes.connectcrm.dto.PedidoResponse;
import br.com.lrrepresentacoes.connectcrm.exception.RecursoNaoEncontradoException;
import br.com.lrrepresentacoes.connectcrm.exception.RegraNegocioException;
import br.com.lrrepresentacoes.connectcrm.model.Cliente;
import br.com.lrrepresentacoes.connectcrm.model.Estoque;
import br.com.lrrepresentacoes.connectcrm.model.FormaPagamento;
import br.com.lrrepresentacoes.connectcrm.model.Pedido;
import br.com.lrrepresentacoes.connectcrm.model.StatusPedido;
import br.com.lrrepresentacoes.connectcrm.model.Tecido;
import br.com.lrrepresentacoes.connectcrm.model.Usuario;
import br.com.lrrepresentacoes.connectcrm.repository.ClienteRepository;
import br.com.lrrepresentacoes.connectcrm.repository.EstoqueRepository;
import br.com.lrrepresentacoes.connectcrm.repository.PedidoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

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

    private static Cliente cliente(Long id, String nome) {
        Cliente cliente = new Cliente(REVENDEDOR, nome, "5562999998888", null, null, "Goiânia", "GO");
        ReflectionTestUtils.setField(cliente, "id", id);
        return cliente;
    }

    private static Tecido tecido(String nome, String precoMetro) {
        return new Tecido(nome, null, null, null, new BigDecimal(precoMetro), null);
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

    private static PedidoRequest request(Long idNegociacao, Boolean pagoNoAto, ItemPedidoRequest... itens) {
        return new PedidoRequest(1L, idNegociacao, "BOLETO", pagoNoAto, List.of(itens));
    }

    // Cliente ativo de id 1, usuário logado e o save devolvendo o próprio pedido.
    private Cliente clienteAtivoLogado() {
        Cliente atelie = cliente(1L, "Ateliê Bella Noiva");
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(atelie));
        return atelie;
    }

    private void gravacaoLiberada() {
        when(usuarioLogadoService.obter()).thenReturn(REVENDEDOR);
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private void estoqueTravado(Estoque... linhas) {
        when(estoqueRepository.buscarPorIdsParaAtualizar(anyCollection())).thenReturn(List.of(linhas));
    }

    // Pedido já gravado, de 10 m numa linha que ficou com 5 m, para os testes de pagar e cancelar.
    private Pedido pedidoGravado(Estoque linha, boolean pagoNoAto) {
        Pedido pedido = new Pedido(cliente(1L, "Ateliê Bella Noiva"), REVENDEDOR, null,
                FormaPagamento.BOLETO, pagoNoAto, LocalDateTime.of(2026, 11, 3, 10, 0));
        pedido.adicionarItem(linha, new BigDecimal("10.00"), new BigDecimal("42.50"));
        when(pedidoRepository.buscarParaAtualizar(7L)).thenReturn(Optional.of(pedido));
        return pedido;
    }

    // ---------- confirmar ----------

    @Test
    @DisplayName("CT-RF008-01: confirmar com estoque suficiente grava o pedido EM_ABERTO e desconta o estoque")
    void confirmar_comEstoqueSuficiente_gravaEmAbertoEDescontaOEstoque() {
        Cliente atelie = clienteAtivoLogado();
        gravacaoLiberada();
        Estoque natural = linha(10L, tecido("Linho Natural", "42.50"), "Natural", "15.00");
        estoqueTravado(natural);

        PedidoResponse resposta = pedidoService.confirmar(request(null, null, item(10L, "10.00")));

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(captor.capture());
        Pedido gravado = captor.getValue();
        assertThat(gravado.getCliente()).isSameAs(atelie);
        assertThat(gravado.getUsuario()).isSameAs(REVENDEDOR);
        assertThat(gravado.getIdNegociacao()).isNull();
        assertThat(gravado.getFormaPagamento()).isEqualTo(FormaPagamento.BOLETO);
        assertThat(gravado.getStatus()).isEqualTo(StatusPedido.EM_ABERTO);
        assertThat(gravado.getDataPedido()).isNotNull();
        assertThat(natural.getMetragem()).isEqualByComparingTo("5.00");
        assertThat(resposta.status()).isEqualTo(StatusPedido.EM_ABERTO);
        assertThat(resposta.total()).isEqualByComparingTo("425.00");
    }

    @Test
    @DisplayName("CT-RF008-02: o total é a soma de metragem × preço por metro dos itens (497,25)")
    void confirmar_calculaOTotalPelosItens() {
        clienteAtivoLogado();
        gravacaoLiberada();
        estoqueTravado(linha(10L, tecido("Linho Natural", "42.50"), "Natural", "15.00"),
                linha(11L, tecido("Viscose", "28.90"), "Azul", "30.00"));

        PedidoResponse resposta = pedidoService.confirmar(
                request(null, null, item(10L, "10.00"), item(11L, "2.50")));

        // 10 × 42,50 = 425,00 e 2,5 × 28,90 = 72,25.
        assertThat(resposta.total()).isEqualByComparingTo("497.25");
        assertThat(resposta.itens()).extracting(ItemPedidoResponse::subtotal)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("425.00"), new BigDecimal("72.25"));
    }

    @Test
    @DisplayName("CT-RF008-03: pedido pago no ato nasce com status PAGO")
    void confirmar_pagoNoAto_nasceComoPago() {
        clienteAtivoLogado();
        gravacaoLiberada();
        estoqueTravado(linha(10L, tecido("Linho Natural", "42.50"), "Natural", "15.00"));

        PedidoResponse resposta = pedidoService.confirmar(request(null, true, item(10L, "1.00")));

        assertThat(resposta.status()).isEqualTo(StatusPedido.PAGO);
    }

    @Test
    @DisplayName("CT-RD003a009-01: estoque insuficiente recusa com \"Estoque insuficiente: <tecido> <cor> tem 18 m\" e nada é gravado")
    void confirmar_comEstoqueInsuficiente_recusaComAMensagemESemGravar() {
        clienteAtivoLogado();
        Estoque bege = linha(10L, tecido("Linho", "42.50"), "Bege 102", "18.00");
        estoqueTravado(bege);

        assertThatThrownBy(() -> pedidoService.confirmar(request(null, null, item(10L, "20.00"))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Estoque insuficiente: Linho Bege 102 tem 18 m");

        verify(pedidoRepository, never()).save(any());
        assertThat(bege.getMetragem()).isEqualByComparingTo("18.00");
    }

    @Test
    @DisplayName("CT-RD003a009-02: se o 2º item não tem saldo, nem o pedido nem a baixa do 1º item acontecem")
    void confirmar_segundoItemSemSaldo_naoBaixaOPrimeiroNemGrava() {
        clienteAtivoLogado();
        Estoque natural = linha(10L, tecido("Linho Natural", "42.50"), "Natural", "15.00");
        Estoque azul = linha(11L, tecido("Viscose", "28.90"), "Azul", "2.50");
        estoqueTravado(natural, azul);

        assertThatThrownBy(() -> pedidoService.confirmar(
                request(null, null, item(10L, "10.00"), item(11L, "5.00"))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Estoque insuficiente: Viscose Azul tem 2,5 m");

        verify(pedidoRepository, never()).save(any());
        assertThat(natural.getMetragem()).isEqualByComparingTo("15.00");
        assertThat(azul.getMetragem()).isEqualByComparingTo("2.50");
    }

    @Test
    @DisplayName("Vender exatamente o saldo é permitido e zera a linha de estoque")
    void confirmar_comMetragemIgualAoSaldo_zeraOEstoque() {
        clienteAtivoLogado();
        gravacaoLiberada();
        Estoque natural = linha(10L, tecido("Linho Natural", "42.50"), "Natural", "1.00");
        estoqueTravado(natural);

        pedidoService.confirmar(request(null, null, item(10L, "1.00")));

        assertThat(natural.getMetragem()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("CT-RD003a009-04: o item guarda o preço da venda mesmo se o preço do tecido mudar depois")
    void confirmar_itemGuardaOPrecoDaVenda() {
        clienteAtivoLogado();
        gravacaoLiberada();
        Tecido linho = tecido("Linho Natural", "42.50");
        estoqueTravado(linha(10L, linho, "Natural", "15.00"));

        pedidoService.confirmar(request(null, null, item(10L, "10.00")));
        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(captor.capture());
        Pedido gravado = captor.getValue();

        // O preço do tecido muda depois da venda (42,50 → 45,00).
        linho.atualizar("Linho Natural", null, null, null, new BigDecimal("45.00"), null);

        assertThat(gravado.getItens().getFirst().getPrecoMetro()).isEqualByComparingTo("42.50");
        assertThat(gravado.getItens().getFirst().getSubtotal()).isEqualByComparingTo("425.00");
        assertThat(gravado.getTotal()).isEqualByComparingTo("425.00");
    }

    @Test
    @DisplayName("CT-RD003a009-05: o mesmo tecido + cor duas vezes (5 m + 3 m) vira uma linha só com 8 m")
    void confirmar_mesmoEstoqueDuasVezes_viraUmItemSo() {
        clienteAtivoLogado();
        gravacaoLiberada();
        Estoque natural = linha(10L, tecido("Linho Natural", "42.50"), "Natural", "15.00");
        estoqueTravado(natural);

        PedidoResponse resposta = pedidoService.confirmar(
                request(null, null, item(10L, "5.00"), item(10L, "3.00")));

        assertThat(resposta.itens()).hasSize(1);
        assertThat(resposta.itens().getFirst().metragem()).isEqualByComparingTo("8.00");
        assertThat(resposta.total()).isEqualByComparingTo("340.00");
        assertThat(natural.getMetragem()).isEqualByComparingTo("7.00");
    }

    // A soma das duas linhas é que precisa caber no saldo: 5 m + 3 m não cabem em 6 m.
    @Test
    @DisplayName("CT-RD003a009-05: o saldo é conferido com a metragem somada do tecido + cor repetido")
    void confirmar_mesmoEstoqueDuasVezes_confereOSaldoComASoma() {
        clienteAtivoLogado();
        estoqueTravado(linha(10L, tecido("Linho Natural", "42.50"), "Natural", "6.00"));

        assertThatThrownBy(() -> pedidoService.confirmar(
                request(null, null, item(10L, "5.00"), item(10L, "3.00"))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Estoque insuficiente: Linho Natural Natural tem 6 m");
    }

    @Test
    @DisplayName("CT-RD003a009-06: pedido vindo de negociação de outro cliente é recusado")
    void confirmar_negociacaoDeOutroCliente_recusaSemTocarNoEstoque() {
        clienteAtivoLogado();
        when(pedidoRepository.buscarIdClienteDaNegociacao(5L)).thenReturn(Optional.of(2L));

        assertThatThrownBy(() -> pedidoService.confirmar(request(5L, null, item(10L, "1.00"))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("A negociação informada é de outro cliente. "
                        + "O pedido de uma negociação precisa ser para o cliente dela.");

        verify(estoqueRepository, never()).buscarPorIdsParaAtualizar(anyCollection());
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CT-RD003a009-06: pedido vindo da negociação do próprio cliente é aceito e guarda a negociação")
    void confirmar_negociacaoDoMesmoCliente_gravaONumeroDaNegociacao() {
        clienteAtivoLogado();
        gravacaoLiberada();
        when(pedidoRepository.buscarIdClienteDaNegociacao(5L)).thenReturn(Optional.of(1L));
        estoqueTravado(linha(10L, tecido("Linho Natural", "42.50"), "Natural", "15.00"));

        PedidoResponse resposta = pedidoService.confirmar(request(5L, null, item(10L, "1.00")));

        assertThat(resposta.idNegociacao()).isEqualTo(5L);
    }

    @Test
    @DisplayName("Pedido com negociação que não existe responde não encontrado")
    void confirmar_negociacaoInexistente_lancaRecursoNaoEncontrado() {
        clienteAtivoLogado();
        when(pedidoRepository.buscarIdClienteDaNegociacao(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoService.confirmar(request(99L, null, item(10L, "1.00"))))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Negociação não encontrada");
    }

    @Test
    @DisplayName("CT-RD003a009-07: vender para cliente desativado é recusado")
    void confirmar_clienteDesativado_recusaSemTocarNoEstoque() {
        Cliente desativado = cliente(1L, "Ateliê Bella Noiva");
        desativado.desativar();
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(desativado));

        assertThatThrownBy(() -> pedidoService.confirmar(request(null, null, item(10L, "1.00"))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("O cliente Ateliê Bella Noiva está desativado. "
                        + "Reative o cliente antes de criar o pedido.");

        verify(estoqueRepository, never()).buscarPorIdsParaAtualizar(anyCollection());
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CT-RD003a009-07: vender tecido desativado é recusado e o estoque não muda")
    void confirmar_tecidoDesativado_recusaSemGravar() {
        clienteAtivoLogado();
        Tecido linho = tecido("Linho Natural", "42.50");
        linho.desativar();
        Estoque natural = linha(10L, linho, "Natural", "15.00");
        estoqueTravado(natural);

        assertThatThrownBy(() -> pedidoService.confirmar(request(null, null, item(10L, "1.00"))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("O tecido Linho Natural está desativado. Reative o tecido antes de vender.");

        verify(pedidoRepository, never()).save(any());
        assertThat(natural.getMetragem()).isEqualByComparingTo("15.00");
    }

    @Test
    @DisplayName("Pedido para cliente que não existe responde não encontrado")
    void confirmar_clienteInexistente_lancaRecursoNaoEncontrado() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoService.confirmar(request(null, null, item(10L, "1.00"))))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Cliente não encontrado");
    }

    @Test
    @DisplayName("Pedido com linha de estoque que não existe responde não encontrado e nada é gravado")
    void confirmar_linhaDeEstoqueInexistente_lancaRecursoNaoEncontrado() {
        clienteAtivoLogado();
        estoqueTravado(linha(10L, tecido("Linho Natural", "42.50"), "Natural", "15.00"));

        assertThatThrownBy(() -> pedidoService.confirmar(
                request(null, null, item(10L, "1.00"), item(99L, "1.00"))))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Linha de estoque não encontrada");
        verify(pedidoRepository, never()).save(any());
    }

    // ---------- marcar como pago ----------

    @Test
    @DisplayName("CT-RF017-01: marcar pedido EM_ABERTO como pago muda o status para PAGO")
    void marcarComoPago_emAberto_viraPago() {
        Pedido pedido = pedidoGravado(linha(10L, tecido("Linho Natural", "42.50"), "Natural", "5.00"), false);

        PedidoResponse resposta = pedidoService.marcarComoPago(7L);

        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.PAGO);
        assertThat(resposta.status()).isEqualTo(StatusPedido.PAGO);
    }

    @Test
    @DisplayName("Marcar como pago um pedido já pago é recusado")
    void marcarComoPago_jaPago_recusa() {
        pedidoGravado(linha(10L, tecido("Linho Natural", "42.50"), "Natural", "5.00"), true);

        assertThatThrownBy(() -> pedidoService.marcarComoPago(7L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("O pedido já está pago");
    }

    // O cancelamento é definitivo (regra 6.1).
    @Test
    @DisplayName("Marcar como pago um pedido cancelado é recusado e ele continua CANCELADO")
    void marcarComoPago_cancelado_recusa() {
        Pedido pedido = pedidoGravado(linha(10L, tecido("Linho Natural", "42.50"), "Natural", "5.00"), false);
        pedido.cancelar();

        assertThatThrownBy(() -> pedidoService.marcarComoPago(7L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("O pedido está cancelado e não pode ser marcado como pago");
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.CANCELADO);
    }

    @Test
    @DisplayName("Marcar como pago um pedido que não existe responde não encontrado")
    void marcarComoPago_inexistente_lancaRecursoNaoEncontrado() {
        when(pedidoRepository.buscarParaAtualizar(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoService.marcarComoPago(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Pedido não encontrado");
    }

    // ---------- cancelar ----------

    @Test
    @DisplayName("CT-RF018-01: cancelar pedido EM_ABERTO devolve a metragem ao estoque e o status vira CANCELADO")
    void cancelar_emAberto_devolveOEstoqueECancela() {
        Estoque natural = linha(10L, tecido("Linho Natural", "42.50"), "Natural", "5.00");
        Pedido pedido = pedidoGravado(natural, false);
        estoqueTravado(natural);

        PedidoResponse resposta = pedidoService.cancelar(7L);

        assertThat(natural.getMetragem()).isEqualByComparingTo("15.00");
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.CANCELADO);
        assertThat(resposta.status()).isEqualTo(StatusPedido.CANCELADO);
        // O pedido não é apagado: continua com os itens e o total.
        verify(pedidoRepository, never()).delete(any());
        assertThat(resposta.itens()).hasSize(1);
        assertThat(resposta.total()).isEqualByComparingTo("425.00");
    }

    // Regra 6.1: aceita qualquer pedido com status diferente de CANCELADO, inclusive o PAGO.
    @Test
    @DisplayName("CT-RF018-01: cancelar pedido PAGO também devolve a metragem ao estoque")
    void cancelar_pago_devolveOEstoqueECancela() {
        Estoque natural = linha(10L, tecido("Linho Natural", "42.50"), "Natural", "5.00");
        Pedido pedido = pedidoGravado(natural, true);
        estoqueTravado(natural);

        pedidoService.cancelar(7L);

        assertThat(natural.getMetragem()).isEqualByComparingTo("15.00");
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.CANCELADO);
    }

    @Test
    @DisplayName("CT-RD008-01: cancelar o mesmo pedido duas vezes é recusado e o estoque não muda")
    void cancelar_jaCancelado_recusaSemDevolverDeNovo() {
        Estoque natural = linha(10L, tecido("Linho Natural", "42.50"), "Natural", "5.00");
        pedidoGravado(natural, false);
        estoqueTravado(natural);
        pedidoService.cancelar(7L);
        assertThat(natural.getMetragem()).isEqualByComparingTo("15.00");

        assertThatThrownBy(() -> pedidoService.cancelar(7L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("O pedido já está cancelado");

        assertThat(natural.getMetragem()).isEqualByComparingTo("15.00");
    }

    @Test
    @DisplayName("Cancelar pedido que não existe responde não encontrado")
    void cancelar_inexistente_lancaRecursoNaoEncontrado() {
        when(pedidoRepository.buscarParaAtualizar(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoService.cancelar(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Pedido não encontrado");
    }

    // ---------- mensagem ----------

    @Test
    @DisplayName("A metragem da mensagem sai sem zeros à direita e com vírgula decimal")
    void emMetros_formataSemZerosEComVirgula() {
        assertThat(PedidoService.emMetros(new BigDecimal("18.00"))).isEqualTo("18");
        assertThat(PedidoService.emMetros(new BigDecimal("2.50"))).isEqualTo("2,5");
        assertThat(PedidoService.emMetros(new BigDecimal("0.00"))).isEqualTo("0");
        assertThat(PedidoService.emMetros(new BigDecimal("100.00"))).isEqualTo("100");
    }
}
