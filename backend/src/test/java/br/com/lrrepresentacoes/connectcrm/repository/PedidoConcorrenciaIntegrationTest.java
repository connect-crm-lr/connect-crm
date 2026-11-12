package br.com.lrrepresentacoes.connectcrm.repository;

import br.com.lrrepresentacoes.connectcrm.dto.ClienteRequest;
import br.com.lrrepresentacoes.connectcrm.dto.EntradaEstoqueRequest;
import br.com.lrrepresentacoes.connectcrm.dto.ItemPedidoRequest;
import br.com.lrrepresentacoes.connectcrm.dto.PedidoRequest;
import br.com.lrrepresentacoes.connectcrm.exception.RegraNegocioException;
import br.com.lrrepresentacoes.connectcrm.model.Tecido;
import br.com.lrrepresentacoes.connectcrm.service.ClienteService;
import br.com.lrrepresentacoes.connectcrm.service.EstoqueService;
import br.com.lrrepresentacoes.connectcrm.service.PedidoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integração com o PostgreSQL real: pedidos simultâneos na mesma linha de estoque (regra 6.1).
 * Sem @Transactional no teste: cada thread precisa da sua própria transação de verdade,
 * como duas requisições ao mesmo tempo. Por isso os dados criados são apagados no @AfterEach.
 */
@SpringBootTest
class PedidoConcorrenciaIntegrationTest {

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
    private JdbcTemplate jdbcTemplate;

    private Tecido tecido;
    private Long idCliente;

    @BeforeEach
    void criarClienteETecido() {
        logar();
        idCliente = clienteService.criar(new ClienteRequest("IT Concorrência Pedidos", "5562999998888",
                null, null, null, null, List.of())).id();
        tecido = tecidoRepository.save(new Tecido("IT Linho Concorrência", null, null, null,
                new BigDecimal("42.50"), null));
    }

    @AfterEach
    void apagarDadosDoTeste() {
        SecurityContextHolder.clearContext();
        jdbcTemplate.update("DELETE FROM item_pedido WHERE id_pedido IN "
                + "(SELECT id_pedido FROM pedido WHERE id_cliente = ?)", idCliente);
        jdbcTemplate.update("DELETE FROM pedido WHERE id_cliente = ?", idCliente);
        jdbcTemplate.update("DELETE FROM entrada_estoque WHERE id_estoque IN "
                + "(SELECT id_estoque FROM estoque WHERE id_tecido = ?)", tecido.getId());
        jdbcTemplate.update("DELETE FROM estoque WHERE id_tecido = ?", tecido.getId());
        jdbcTemplate.update("DELETE FROM tecido WHERE id_tecido = ?", tecido.getId());
        jdbcTemplate.update("DELETE FROM cliente WHERE id_cliente = ?", idCliente);
    }

    // Simula a requisição autenticada. O contexto de segurança é por thread: cada thread loga.
    private static void logar() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(EMAIL_V2, null, List.of()));
    }

    private Long criarLinhaDeEstoque(String cor, String metros) {
        return estoqueService.registrarEntrada(new EntradaEstoqueRequest(
                tecido.getId(), cor, new BigDecimal(metros), null)).idEstoque();
    }

    private PedidoRequest pedido(ItemPedidoRequest... itens) {
        return new PedidoRequest(idCliente, null, "BOLETO", null, List.of(itens));
    }

    private static ItemPedidoRequest item(Long idEstoque, String metragem) {
        return new ItemPedidoRequest(idEstoque, new BigDecimal(metragem));
    }

    private BigDecimal saldo(Long idEstoque) {
        return jdbcTemplate.queryForObject(
                "SELECT metragem FROM estoque WHERE id_estoque = ?", BigDecimal.class, idEstoque);
    }

    private int contarPedidos() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pedido WHERE id_cliente = ?", Integer.class, idCliente);
    }

    private BigDecimal metragemVendida(Long idEstoque) {
        return jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(metragem), 0) FROM item_pedido WHERE id_estoque = ?",
                BigDecimal.class, idEstoque);
    }

    /**
     * Dispara as tarefas juntas (o latch libera todas as threads no mesmo instante) e devolve,
     * para cada uma, null se deu certo ou a mensagem da regra de negócio que a recusou.
     * Qualquer outra exceção (deadlock, CHECK do banco...) falha o teste.
     */
    private List<String> executarAoMesmoTempo(List<Callable<?>> tarefas) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tarefas.size());
        CountDownLatch largada = new CountDownLatch(1);
        try {
            List<Future<String>> resultados = new ArrayList<>();
            // Padrão Iterator: for-each percorre as tarefas, uma thread para cada.
            for (Callable<?> tarefa : tarefas) {
                resultados.add(executor.submit(() -> {
                    logar();
                    try {
                        largada.await();
                        tarefa.call();
                        return null;
                    } catch (RegraNegocioException recusa) {
                        return recusa.getMessage();
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                }));
            }
            largada.countDown();
            List<String> recusas = new ArrayList<>();
            for (Future<String> resultado : resultados) {
                // get() repassa qualquer exceção inesperada da thread.
                recusas.add(resultado.get(30, TimeUnit.SECONDS));
            }
            return recusas;
        } finally {
            executor.shutdownNow();
        }
    }

    private static <T> List<Callable<?>> repetir(int vezes, Callable<T> tarefa) {
        List<Callable<?>> tarefas = new ArrayList<>();
        for (int i = 0; i < vezes; i++) {
            tarefas.add(tarefa);
        }
        return tarefas;
    }

    @Test
    @DisplayName("CT-RD003a009-03: dois pedidos ao mesmo tempo pelo último metro: só um confirma e o saldo fica em 0")
    void doisPedidosPeloUltimoMetro_soUmConfirma() throws Exception {
        Long ultimoMetro = criarLinhaDeEstoque("Natural", "1.00");

        List<String> recusas = executarAoMesmoTempo(
                repetir(2, () -> pedidoService.confirmar(pedido(item(ultimoMetro, "1.00")))));

        // Um confirmou (null) e o outro foi recusado já enxergando o saldo zerado.
        assertThat(recusas).containsExactlyInAnyOrder(null,
                "Estoque insuficiente: IT Linho Concorrência Natural tem 0 m");
        assertThat(saldo(ultimoMetro)).isEqualByComparingTo("0.00");
        assertThat(contarPedidos()).isEqualTo(1);
        assertThat(metragemVendida(ultimoMetro)).isEqualByComparingTo("1.00");
    }

    // O mesmo cenário com mais disputa: nunca se vende mais do que o saldo.
    @Test
    @DisplayName("CT-RD003a009-03: seis pedidos de 1 m para 3 m de saldo: três confirmam e o saldo nunca fica negativo")
    void seisPedidosParaTresMetros_confirmamSoTres() throws Exception {
        Long linha = criarLinhaDeEstoque("Cru", "3.00");

        List<String> recusas = executarAoMesmoTempo(
                repetir(6, () -> pedidoService.confirmar(pedido(item(linha, "1.00")))));

        assertThat(recusas).filteredOn(recusa -> recusa == null).hasSize(3);
        assertThat(recusas).filteredOn(recusa -> recusa != null)
                .hasSize(3)
                .containsOnly("Estoque insuficiente: IT Linho Concorrência Cru tem 0 m");
        assertThat(saldo(linha)).isEqualByComparingTo("0.00");
        assertThat(contarPedidos()).isEqualTo(3);
        assertThat(metragemVendida(linha)).isEqualByComparingTo("3.00");
    }

    // As linhas são travadas sempre em ordem de id; sem isso, estes dois pedidos poderiam
    // travar uma linha cada e ficar um esperando o outro (deadlock).
    @Test
    @DisplayName("Dois pedidos com as mesmas linhas em ordem inversa confirmam os dois, sem deadlock")
    void pedidosComAsMesmasLinhasEmOrdemInversa_confirmamOsDois() throws Exception {
        Long natural = criarLinhaDeEstoque("Natural", "10.00");
        Long cru = criarLinhaDeEstoque("Cru", "10.00");

        List<Callable<?>> tarefas = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            tarefas.add(() -> pedidoService.confirmar(pedido(item(natural, "1.00"), item(cru, "1.00"))));
            tarefas.add(() -> pedidoService.confirmar(pedido(item(cru, "1.00"), item(natural, "1.00"))));
        }
        List<String> recusas = executarAoMesmoTempo(tarefas);

        assertThat(recusas).containsOnlyNulls();
        assertThat(saldo(natural)).isEqualByComparingTo("4.00");
        assertThat(saldo(cru)).isEqualByComparingTo("4.00");
        assertThat(contarPedidos()).isEqualTo(6);
    }

    @Test
    @DisplayName("CT-RD003a009-02: se o 2º item não tem saldo, nada fica gravado: nem o pedido, nem a baixa do 1º item")
    void segundoItemSemSaldo_nadaFicaGravadoNoBanco() {
        Long natural = criarLinhaDeEstoque("Natural", "15.00");
        Long cru = criarLinhaDeEstoque("Cru", "2.00");

        assertThatThrownBy(() -> pedidoService.confirmar(pedido(item(natural, "10.00"), item(cru, "5.00"))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Estoque insuficiente: IT Linho Concorrência Cru tem 2 m");

        // Lido em outra transação, depois do rollback da confirmação.
        assertThat(contarPedidos()).isZero();
        assertThat(saldo(natural)).isEqualByComparingTo("15.00");
        assertThat(saldo(cru)).isEqualByComparingTo("2.00");
        assertThat(metragemVendida(natural)).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("CT-RD008-01: quatro cancelamentos ao mesmo tempo: só um cancela e o estoque é devolvido uma vez")
    void cancelamentosSimultaneos_devolvemOEstoqueUmaVezSo() throws Exception {
        Long linha = criarLinhaDeEstoque("Natural", "15.00");
        Long idPedido = pedidoService.confirmar(pedido(item(linha, "10.00"))).id();
        assertThat(saldo(linha)).isEqualByComparingTo("5.00");

        List<String> recusas = executarAoMesmoTempo(repetir(4, () -> pedidoService.cancelar(idPedido)));

        assertThat(recusas).filteredOn(recusa -> recusa == null).hasSize(1);
        assertThat(recusas).filteredOn(recusa -> recusa != null)
                .hasSize(3)
                .containsOnly("O pedido já está cancelado");
        assertThat(saldo(linha)).isEqualByComparingTo("15.00");
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM pedido WHERE id_pedido = ?", String.class, idPedido);
        assertThat(status).isEqualTo("CANCELADO");
    }
}
