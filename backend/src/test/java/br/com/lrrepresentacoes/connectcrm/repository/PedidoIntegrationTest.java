package br.com.lrrepresentacoes.connectcrm.repository;

import br.com.lrrepresentacoes.connectcrm.dto.ClienteRequest;
import br.com.lrrepresentacoes.connectcrm.dto.ClienteResponse;
import br.com.lrrepresentacoes.connectcrm.dto.EntradaEstoqueRequest;
import br.com.lrrepresentacoes.connectcrm.dto.ItemPedidoRequest;
import br.com.lrrepresentacoes.connectcrm.dto.PedidoRequest;
import br.com.lrrepresentacoes.connectcrm.dto.PedidoResponse;
import br.com.lrrepresentacoes.connectcrm.exception.RegraNegocioException;
import br.com.lrrepresentacoes.connectcrm.model.StatusPedido;
import br.com.lrrepresentacoes.connectcrm.model.Tecido;
import br.com.lrrepresentacoes.connectcrm.service.ClienteService;
import br.com.lrrepresentacoes.connectcrm.service.EstoqueService;
import br.com.lrrepresentacoes.connectcrm.service.PedidoService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integração com o PostgreSQL real (migrations V1 e V2 aplicadas pelo Flyway).
 * Confere o que o pedido grava de verdade nas tabelas pedido, item_pedido e estoque.
 * Cada teste roda numa transação desfeita no fim (@Transactional), então o banco não muda.
 * Os testes que precisam de transações de verdade (concorrência e "nada gravado") ficam no
 * PedidoConcorrenciaIntegrationTest.
 */
@SpringBootTest
@Transactional
class PedidoIntegrationTest {

    // Usuário inicial criado pela V2.
    private static final String EMAIL_V2 = "revendedor@lrrepresentacoes.com.br";

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private EstoqueService estoqueService;

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private TecidoRepository tecidoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ClienteResponse bella;
    private Tecido linho;
    private Long idLinhoNatural;
    private Long idViscoseAzul;

    @BeforeEach
    void logarECriarDados() {
        // Simula a requisição autenticada: o JWT leva o e-mail do usuário como nome.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(EMAIL_V2, null, List.of()));

        bella = clienteService.criar(new ClienteRequest("IT Ateliê Bella Noiva", "5562999998888",
                null, null, "Goiânia", "GO", List.of()));
        linho = tecidoRepository.save(new Tecido("IT Linho Natural", null, null, null,
                new BigDecimal("42.50"), null));
        Tecido viscose = tecidoRepository.save(new Tecido("IT Viscose", null, null, null,
                new BigDecimal("28.90"), null));
        idLinhoNatural = estoqueService.registrarEntrada(new EntradaEstoqueRequest(
                linho.getId(), "Natural", new BigDecimal("15.00"), null)).idEstoque();
        idViscoseAzul = estoqueService.registrarEntrada(new EntradaEstoqueRequest(
                viscose.getId(), "Azul", new BigDecimal("30.00"), null)).idEstoque();
    }

    @AfterEach
    void sair() {
        SecurityContextHolder.clearContext();
    }

    private static ItemPedidoRequest item(Long idEstoque, String metragem) {
        return new ItemPedidoRequest(idEstoque, new BigDecimal(metragem));
    }

    private PedidoRequest pedidoDaBella(ItemPedidoRequest... itens) {
        return new PedidoRequest(bella.id(), null, "BOLETO", null, List.of(itens));
    }

    // Grava o que está pendente no Hibernate antes de ler direto da tabela.
    private BigDecimal saldo(Long idEstoque) {
        entityManager.flush();
        return jdbcTemplate.queryForObject(
                "SELECT metragem FROM estoque WHERE id_estoque = ?", BigDecimal.class, idEstoque);
    }

    private Map<String, Object> linhaDoPedido(Long idPedido) {
        entityManager.flush();
        return jdbcTemplate.queryForMap("""
                SELECT id_cliente, id_usuario, id_negociacao, forma_pagamento, status, total, data_pedido
                FROM pedido WHERE id_pedido = ?
                """, idPedido);
    }

    private Long idUsuarioV2() {
        return jdbcTemplate.queryForObject("SELECT id_usuario FROM usuario WHERE email = ?", Long.class, EMAIL_V2);
    }

    private Long criarNegociacao(Long idCliente) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO negociacao (id_cliente, id_usuario, descricao, estagio)
                VALUES (?, ?, 'IT pedido de linho', 'FECHADA') RETURNING id_negociacao
                """, Long.class, idCliente, idUsuarioV2());
    }

    // ---------- confirmar ----------

    @Test
    @DisplayName("CT-RF008-01: confirmar 10 m de uma linha com 15 m grava o pedido EM_ABERTO e deixa 5 m no estoque")
    void confirmar_gravaOPedidoEDescontaOEstoque() {
        PedidoResponse resposta = pedidoService.confirmar(pedidoDaBella(item(idLinhoNatural, "10.00")));

        Map<String, Object> pedido = linhaDoPedido(resposta.id());
        assertThat(pedido.get("id_cliente")).isEqualTo(bella.id());
        assertThat(pedido.get("id_usuario")).isEqualTo(idUsuarioV2());
        assertThat(pedido.get("id_negociacao")).isNull();
        assertThat(pedido.get("forma_pagamento")).isEqualTo("BOLETO");
        assertThat(pedido.get("status")).isEqualTo("EM_ABERTO");
        assertThat((BigDecimal) pedido.get("total")).isEqualByComparingTo("425.00");
        assertThat(pedido.get("data_pedido")).isNotNull();
        assertThat(saldo(idLinhoNatural)).isEqualByComparingTo("5.00");
    }

    @Test
    @DisplayName("CT-RF008-02: o total gravado é 10 m × 42,50 + 2,5 m × 28,90 = 497,25")
    void confirmar_gravaOTotalEOsItensComOPrecoDoTecido() {
        PedidoResponse resposta = pedidoService.confirmar(
                pedidoDaBella(item(idLinhoNatural, "10.00"), item(idViscoseAzul, "2.50")));
        entityManager.flush();

        assertThat(resposta.total()).isEqualByComparingTo("497.25");
        List<Map<String, Object>> itens = jdbcTemplate.queryForList(
                "SELECT id_estoque, metragem, preco_metro FROM item_pedido WHERE id_pedido = ? ORDER BY id_item_pedido",
                resposta.id());
        assertThat(itens).hasSize(2);
        assertThat(itens.get(0).get("id_estoque")).isEqualTo(idLinhoNatural);
        assertThat((BigDecimal) itens.get(0).get("preco_metro")).isEqualByComparingTo("42.50");
        assertThat((BigDecimal) itens.get(1).get("metragem")).isEqualByComparingTo("2.50");
        assertThat((BigDecimal) itens.get(1).get("preco_metro")).isEqualByComparingTo("28.90");
        assertThat(saldo(idViscoseAzul)).isEqualByComparingTo("27.50");
    }

    @Test
    @DisplayName("CT-RF008-03: pedido pago no ato é gravado com status PAGO")
    void confirmar_pagoNoAto_gravaComoPago() {
        PedidoResponse resposta = pedidoService.confirmar(new PedidoRequest(
                bella.id(), null, "A_VISTA", true, List.of(item(idLinhoNatural, "1.00"))));

        assertThat(linhaDoPedido(resposta.id()).get("status")).isEqualTo("PAGO");
    }

    @Test
    @DisplayName("CT-RD003a009-04: mudar o preço do tecido depois da venda não muda o preço gravado no item")
    void confirmar_itemMantemOPrecoDaVendaNoBanco() {
        PedidoResponse resposta = pedidoService.confirmar(pedidoDaBella(item(idLinhoNatural, "10.00")));

        linho.atualizar("IT Linho Natural", null, null, null, new BigDecimal("45.00"), null);
        tecidoRepository.saveAndFlush(linho);

        BigDecimal precoDoItem = jdbcTemplate.queryForObject(
                "SELECT preco_metro FROM item_pedido WHERE id_pedido = ?", BigDecimal.class, resposta.id());
        BigDecimal precoDoTecido = jdbcTemplate.queryForObject(
                "SELECT preco_metro FROM tecido WHERE id_tecido = ?", BigDecimal.class, linho.getId());
        assertThat(precoDoItem).isEqualByComparingTo("42.50");
        assertThat(precoDoTecido).isEqualByComparingTo("45.00");
        assertThat((BigDecimal) linhaDoPedido(resposta.id()).get("total")).isEqualByComparingTo("425.00");
    }

    @Test
    @DisplayName("CT-RD003a009-05: o mesmo tecido + cor duas vezes (5 m + 3 m) é gravado como um item de 8 m")
    void confirmar_mesmoEstoqueDuasVezes_gravaUmItemSo() {
        PedidoResponse resposta = pedidoService.confirmar(
                pedidoDaBella(item(idLinhoNatural, "5.00"), item(idLinhoNatural, "3.00")));
        entityManager.flush();

        List<BigDecimal> metragens = jdbcTemplate.queryForList(
                "SELECT metragem FROM item_pedido WHERE id_pedido = ?", BigDecimal.class, resposta.id());
        assertThat(metragens).hasSize(1);
        assertThat(metragens.getFirst()).isEqualByComparingTo("8.00");
        assertThat(saldo(idLinhoNatural)).isEqualByComparingTo("7.00");
    }

    @Test
    @DisplayName("CT-RD003a009-06: o pedido da negociação do próprio cliente grava o id_negociacao; o de outro cliente é recusado")
    void confirmar_pedidoDeNegociacao_exigeOMesmoCliente() {
        ClienteResponse moreno = clienteService.criar(new ClienteRequest("IT Casa Têxtil Moreno", "551134567890",
                null, null, null, null, List.of()));
        Long negociacaoDaBella = criarNegociacao(bella.id());

        PedidoResponse resposta = pedidoService.confirmar(new PedidoRequest(
                bella.id(), negociacaoDaBella, "BOLETO", null, List.of(item(idLinhoNatural, "1.00"))));
        assertThat(linhaDoPedido(resposta.id()).get("id_negociacao")).isEqualTo(negociacaoDaBella);

        // Negociação da Bella, pedido para o Moreno.
        assertThatThrownBy(() -> pedidoService.confirmar(new PedidoRequest(
                moreno.id(), negociacaoDaBella, "BOLETO", null, List.of(item(idLinhoNatural, "1.00")))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("é de outro cliente");
        assertThat(saldo(idLinhoNatural)).isEqualByComparingTo("14.00");
    }

    // Regra 6.2: uma negociação pode originar vários pedidos.
    @Test
    @DisplayName("Uma negociação pode originar mais de um pedido")
    void confirmar_doisPedidosDaMesmaNegociacao_saoAceitos() {
        Long negociacao = criarNegociacao(bella.id());

        pedidoService.confirmar(new PedidoRequest(
                bella.id(), negociacao, "BOLETO", null, List.of(item(idLinhoNatural, "1.00"))));
        pedidoService.confirmar(new PedidoRequest(
                bella.id(), negociacao, "CHEQUE", null, List.of(item(idViscoseAzul, "1.00"))));
        entityManager.flush();

        Integer pedidos = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pedido WHERE id_negociacao = ?", Integer.class, negociacao);
        assertThat(pedidos).isEqualTo(2);
    }

    @Test
    @DisplayName("CT-RD003a009-07: tecido desativado e cliente desativado são recusados e o estoque não muda")
    void confirmar_tecidoOuClienteDesativado_recusa() {
        linho.desativar();
        tecidoRepository.saveAndFlush(linho);
        assertThatThrownBy(() -> pedidoService.confirmar(pedidoDaBella(item(idLinhoNatural, "1.00"))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("O tecido IT Linho Natural está desativado. Reative o tecido antes de vender.");

        clienteService.desativar(bella.id());
        assertThatThrownBy(() -> pedidoService.confirmar(pedidoDaBella(item(idViscoseAzul, "1.00"))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("IT Ateliê Bella Noiva está desativado");

        assertThat(saldo(idLinhoNatural)).isEqualByComparingTo("15.00");
        assertThat(saldo(idViscoseAzul)).isEqualByComparingTo("30.00");
    }

    // ---------- marcar como pago ----------

    // "Pedido em aberto" = pedido não pago: é a contagem usada pela tarja e pela desativação do cliente.
    @Test
    @DisplayName("CT-RF017-01: marcar como pago grava PAGO e o cliente deixa de ter pedido em aberto")
    void marcarComoPago_gravaPagoEOClienteDeixaDeTerPedidoEmAberto() {
        PedidoResponse resposta = pedidoService.confirmar(pedidoDaBella(item(idLinhoNatural, "10.00")));
        entityManager.flush();
        assertThat(clienteRepository.contarPedidosEmAberto(bella.id())).isEqualTo(1);
        assertThatThrownBy(() -> clienteService.desativar(bella.id()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("1 pedido em aberto");

        PedidoResponse pago = pedidoService.marcarComoPago(resposta.id());

        assertThat(pago.status()).isEqualTo(StatusPedido.PAGO);
        assertThat(linhaDoPedido(resposta.id()).get("status")).isEqualTo("PAGO");
        assertThat(clienteRepository.contarPedidosEmAberto(bella.id())).isZero();
        // Pagar não mexe no estoque.
        assertThat(saldo(idLinhoNatural)).isEqualByComparingTo("5.00");
    }

    // ---------- cancelar ----------

    @Test
    @DisplayName("CT-RF018-01: cancelar o pedido de 10 m devolve os 10 m ao estoque e o pedido continua no banco")
    void cancelar_devolveOEstoqueEMantemOPedidoNoBanco() {
        PedidoResponse resposta = pedidoService.confirmar(
                pedidoDaBella(item(idLinhoNatural, "10.00"), item(idViscoseAzul, "2.50")));
        assertThat(saldo(idLinhoNatural)).isEqualByComparingTo("5.00");

        PedidoResponse cancelado = pedidoService.cancelar(resposta.id());

        assertThat(cancelado.status()).isEqualTo(StatusPedido.CANCELADO);
        assertThat(saldo(idLinhoNatural)).isEqualByComparingTo("15.00");
        assertThat(saldo(idViscoseAzul)).isEqualByComparingTo("30.00");
        // O pedido cancelado nunca é apagado: a linha, os itens e o total continuam lá.
        Map<String, Object> pedido = linhaDoPedido(resposta.id());
        assertThat(pedido.get("status")).isEqualTo("CANCELADO");
        assertThat((BigDecimal) pedido.get("total")).isEqualByComparingTo("497.25");
        Integer itens = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM item_pedido WHERE id_pedido = ?", Integer.class, resposta.id());
        assertThat(itens).isEqualTo(2);
        // Pedido cancelado não conta como pedido em aberto.
        assertThat(clienteRepository.contarPedidosEmAberto(bella.id())).isZero();
    }

    @Test
    @DisplayName("CT-RD008-01: cancelar o mesmo pedido duas vezes é recusado e o estoque não é devolvido de novo")
    void cancelar_duasVezes_recusaASegundaSemMexerNoEstoque() {
        PedidoResponse resposta = pedidoService.confirmar(pedidoDaBella(item(idLinhoNatural, "10.00")));
        pedidoService.cancelar(resposta.id());

        assertThatThrownBy(() -> pedidoService.cancelar(resposta.id()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("O pedido já está cancelado");

        assertThat(saldo(idLinhoNatural)).isEqualByComparingTo("15.00");
    }

    @Test
    @DisplayName("Pedido cancelado não pode ser marcado como pago")
    void marcarComoPago_pedidoCancelado_recusa() {
        PedidoResponse resposta = pedidoService.confirmar(pedidoDaBella(item(idLinhoNatural, "10.00")));
        pedidoService.cancelar(resposta.id());

        assertThatThrownBy(() -> pedidoService.marcarComoPago(resposta.id()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("O pedido está cancelado e não pode ser marcado como pago");
        assertThat(linhaDoPedido(resposta.id()).get("status")).isEqualTo("CANCELADO");
    }
}
