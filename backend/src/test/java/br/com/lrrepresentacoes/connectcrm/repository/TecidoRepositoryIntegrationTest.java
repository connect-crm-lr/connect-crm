package br.com.lrrepresentacoes.connectcrm.repository;

import br.com.lrrepresentacoes.connectcrm.dto.TecidoResponse;
import br.com.lrrepresentacoes.connectcrm.model.Tecido;
import br.com.lrrepresentacoes.connectcrm.service.TecidoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integração com o PostgreSQL real (migrations V1 e V2 aplicadas pelo Flyway).
 * Confere a consulta de busca e as restrições da tabela tecido.
 * Cada teste roda numa transação desfeita no fim (@Transactional), então o banco não muda.
 * Os nomes usam o prefixo "IT " para não colidir com dados já existentes no banco local.
 */
@SpringBootTest
@Transactional
class TecidoRepositoryIntegrationTest {

    @Autowired
    private TecidoRepository tecidoRepository;

    @Autowired
    private TecidoService tecidoService;

    @BeforeEach
    void criarTecidos() {
        tecidoRepository.save(new Tecido("IT Linho Rústico", "100% Linho", 185,
                new BigDecimal("1.40"), new BigDecimal("42.50"), null));
        tecidoRepository.save(new Tecido("IT Crepe", "100% Poliéster com toque de linho", 80,
                new BigDecimal("1.50"), new BigDecimal("28.90"), null));
        Tecido inativo = new Tecido("IT Linho Antigo", "Linho misto", null, null, new BigDecimal("30.00"), null);
        inativo.desativar();
        tecidoRepository.save(inativo);
        tecidoRepository.flush();
    }

    // CT-RF015-01: busca no nome e na composição, sem diferenciar maiúsculas, só ativos.
    @Test
    void buscar_encontraPeloNomeOuPelaComposicaoSemOsInativos() {
        List<TecidoResponse> resultado = tecidoService.listar("LINHO", false);

        assertThat(resultado).extracting(TecidoResponse::nome)
                .contains("IT Linho Rústico", "IT Crepe")
                .doesNotContain("IT Linho Antigo");
    }

    @Test
    void buscar_comInativos_incluiODesativado() {
        List<TecidoResponse> resultado = tecidoService.listar("it linho", true);

        assertThat(resultado).extracting(TecidoResponse::nome)
                .contains("IT Linho Rústico", "IT Linho Antigo");
    }

    // CT-RF023-01: tecido desativado sai da listagem padrão.
    @Test
    void listar_padrao_naoMostraDesativados() {
        List<TecidoResponse> resultado = tecidoService.listar(null, false);

        assertThat(resultado).extracting(TecidoResponse::nome)
                .contains("IT Linho Rústico")
                .doesNotContain("IT Linho Antigo");
    }

    // O banco também protege o preço (CHECK preco_metro > 0 na V1).
    @Test
    void salvar_comPrecoZero_oBancoRecusa() {
        Tecido invalido = new Tecido("IT Preço Zero", null, null, null, BigDecimal.ZERO, null);

        assertThatThrownBy(() -> tecidoRepository.saveAndFlush(invalido))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
