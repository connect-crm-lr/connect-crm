package br.com.lrrepresentacoes.connectcrm.repository;

import br.com.lrrepresentacoes.connectcrm.dto.EntradaEstoqueRequest;
import br.com.lrrepresentacoes.connectcrm.dto.EntradaEstoqueResponse;
import br.com.lrrepresentacoes.connectcrm.dto.EstoqueResponse;
import br.com.lrrepresentacoes.connectcrm.exception.RegraNegocioException;
import br.com.lrrepresentacoes.connectcrm.model.Tecido;
import br.com.lrrepresentacoes.connectcrm.service.EstoqueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integração com o PostgreSQL real: registrar entrada de estoque grava o histórico,
 * soma ao saldo na mesma linha e respeita as restrições da V1.
 * Cada teste roda numa transação desfeita no fim (@Transactional), então o banco não muda.
 */
@SpringBootTest
@Transactional
class EstoqueIntegrationTest {

    @Autowired
    private EstoqueService estoqueService;

    @Autowired
    private TecidoRepository tecidoRepository;

    @Autowired
    private EstoqueRepository estoqueRepository;

    @Autowired
    private EntradaEstoqueRepository entradaEstoqueRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Tecido linho;
    private Tecido desativado;

    @BeforeEach
    void criarTecidos() {
        linho = tecidoRepository.save(new Tecido("IT Linho Estoque", "100% Linho", 185,
                new BigDecimal("1.40"), new BigDecimal("42.50"), null));
        desativado = new Tecido("IT Tecido Desativado", null, null, null, new BigDecimal("10.00"), null);
        desativado.desativar();
        desativado = tecidoRepository.save(desativado);
        tecidoRepository.flush();
    }

    // CT-RD004-01 + CT-RF010-01: a 1ª entrada cria a linha; a 2ª (mesma cor, outra grafia) soma na mesma linha.
    @Test
    void registrarEntrada_criaALinhaESomaNaMesmaCorComHistorico() {
        EntradaEstoqueResponse primeira = estoqueService.registrarEntrada(new EntradaEstoqueRequest(
                linho.getId(), "Off-white", new BigDecimal("15.00"), LocalDate.of(2026, 11, 2)));
        EntradaEstoqueResponse segunda = estoqueService.registrarEntrada(new EntradaEstoqueRequest(
                linho.getId(), "OFF-WHITE", new BigDecimal("50.00"), LocalDate.of(2026, 11, 3)));
        estoqueRepository.flush();

        assertThat(segunda.idEstoque()).isEqualTo(primeira.idEstoque());
        assertThat(segunda.saldoAtual()).isEqualByComparingTo("65.00");
        assertThat(estoqueRepository.findById(primeira.idEstoque()).orElseThrow().getMetragem())
                .isEqualByComparingTo("65.00");
        assertThat(entradaEstoqueRepository.findByEstoqueIdOrderByDataEntradaDescIdDesc(primeira.idEstoque()))
                .extracting(e -> e.getMetragem().toPlainString())
                .containsExactly("50.00", "15.00");
    }

    // CT-RF009-01/02: a tela Estoque mostra a linha com o indicador de estoque baixo.
    @Test
    void listar_mostraALinhaComEstoqueBaixo() {
        estoqueService.registrarEntrada(new EntradaEstoqueRequest(
                linho.getId(), "Areia", new BigDecimal("19.99"), null));
        estoqueRepository.flush();

        List<EstoqueResponse> baixo = estoqueService.listar("it linho estoque", false);
        assertThat(baixo).extracting(EstoqueResponse::corCodigo, EstoqueResponse::estoqueBaixo)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("Areia", true));

        assertThat(estoqueService.listar(null, true))
                .extracting(EstoqueResponse::tecido)
                .contains("IT Linho Estoque");
    }

    // CT-RF009-03: linhas de tecido desativado não aparecem.
    @Test
    void listar_naoMostraLinhaDeTecidoDesativado() {
        jdbcTemplate.update("INSERT INTO estoque (id_tecido, cor_codigo, metragem) VALUES (?, 'Preto', 100)",
                desativado.getId());

        assertThat(estoqueService.listar(null, false))
                .extracting(EstoqueResponse::tecido)
                .doesNotContain("IT Tecido Desativado");
    }

    // CT-RD012-01: tecido desativado não recebe entrada.
    @Test
    void registrarEntrada_tecidoDesativado_recusa() {
        assertThatThrownBy(() -> estoqueService.registrarEntrada(new EntradaEstoqueRequest(
                desativado.getId(), "Preto", BigDecimal.TEN, null)))
                .isInstanceOf(RegraNegocioException.class);
    }

    // CT-RD013-01: o banco recusa saldo negativo (CHECK metragem >= 0).
    @Test
    void saldoNegativo_oBancoRecusa() {
        EntradaEstoqueResponse entrada = estoqueService.registrarEntrada(new EntradaEstoqueRequest(
                linho.getId(), "Natural", new BigDecimal("5.00"), null));
        estoqueRepository.flush();

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE estoque SET metragem = -1 WHERE id_estoque = ?", entrada.idEstoque()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
