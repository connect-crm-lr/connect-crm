package br.com.lrrepresentacoes.connectcrm.repository;

import br.com.lrrepresentacoes.connectcrm.dto.EntradaEstoqueRequest;
import br.com.lrrepresentacoes.connectcrm.model.Tecido;
import br.com.lrrepresentacoes.connectcrm.service.EstoqueService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integração com o PostgreSQL real: entradas de estoque simultâneas (regra 6.1).
 * Sem @Transactional no teste: cada thread precisa da sua própria transação de verdade,
 * como duas requisições ao mesmo tempo. Por isso os dados criados são apagados no @AfterEach.
 */
@SpringBootTest
class EstoqueConcorrenciaIntegrationTest {

    private static final int ENTRADAS_SIMULTANEAS = 4;

    @Autowired
    private EstoqueService estoqueService;

    @Autowired
    private TecidoRepository tecidoRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Tecido tecido;

    @BeforeEach
    void criarTecido() {
        tecido = tecidoRepository.save(new Tecido("IT Concorrência", null, null, null,
                new BigDecimal("20.00"), null));
    }

    @AfterEach
    void apagarDadosDoTeste() {
        jdbcTemplate.update("DELETE FROM entrada_estoque WHERE id_estoque IN "
                + "(SELECT id_estoque FROM estoque WHERE id_tecido = ?)", tecido.getId());
        jdbcTemplate.update("DELETE FROM estoque WHERE id_tecido = ?", tecido.getId());
        jdbcTemplate.update("DELETE FROM tecido WHERE id_tecido = ?", tecido.getId());
    }

    // Primeiras entradas de uma cor nova ao mesmo tempo: nenhuma falha na
    // uk_estoque_tecido_cor, existe uma única linha e nenhuma metragem se perde.
    @Test
    void entradasSimultaneas_corNova_criamUmaLinhaESomamTudo() throws Exception {
        registrarAoMesmoTempo("Azul Royal", new BigDecimal("10.00"));

        assertThat(contarLinhas("Azul Royal")).isEqualTo(1);
        assertThat(saldo("Azul Royal")).isEqualByComparingTo("40.00");
        assertThat(contarEntradas("Azul Royal")).isEqualTo(ENTRADAS_SIMULTANEAS);
    }

    // Entradas simultâneas numa linha que já existe: o SELECT ... FOR UPDATE faz uma
    // esperar a outra, então a soma de uma não sobrescreve a da outra.
    @Test
    void entradasSimultaneas_linhaExistente_naoPerdemASoma() throws Exception {
        estoqueService.registrarEntrada(new EntradaEstoqueRequest(
                tecido.getId(), "Verde", new BigDecimal("5.00"), null));

        registrarAoMesmoTempo("Verde", new BigDecimal("2.50"));

        assertThat(contarLinhas("Verde")).isEqualTo(1);
        assertThat(saldo("Verde")).isEqualByComparingTo("15.00");
        assertThat(contarEntradas("Verde")).isEqualTo(ENTRADAS_SIMULTANEAS + 1);
    }

    // Dispara as entradas juntas (o latch libera todas as threads no mesmo instante)
    // e falha o teste se qualquer uma delas lançar exceção.
    private void registrarAoMesmoTempo(String cor, BigDecimal metragem) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(ENTRADAS_SIMULTANEAS);
        CountDownLatch largada = new CountDownLatch(1);
        try {
            List<Future<?>> resultados = new ArrayList<>();
            for (int i = 0; i < ENTRADAS_SIMULTANEAS; i++) {
                resultados.add(executor.submit(() -> {
                    largada.await();
                    estoqueService.registrarEntrada(new EntradaEstoqueRequest(
                            tecido.getId(), cor, metragem, null));
                    return null;
                }));
            }
            largada.countDown();
            // Padrão Iterator: for-each percorre os resultados; get() repassa a exceção da thread.
            for (Future<?> resultado : resultados) {
                resultado.get(30, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private int contarLinhas(String cor) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM estoque WHERE id_tecido = ? AND cor_codigo = ?",
                Integer.class, tecido.getId(), cor);
    }

    private BigDecimal saldo(String cor) {
        return jdbcTemplate.queryForObject(
                "SELECT metragem FROM estoque WHERE id_tecido = ? AND cor_codigo = ?",
                BigDecimal.class, tecido.getId(), cor);
    }

    private int contarEntradas(String cor) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM entrada_estoque en JOIN estoque e ON e.id_estoque = en.id_estoque "
                        + "WHERE e.id_tecido = ? AND e.cor_codigo = ?",
                Integer.class, tecido.getId(), cor);
    }
}
