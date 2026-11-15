package br.com.lrrepresentacoes.connectcrm.repository;

import br.com.lrrepresentacoes.connectcrm.dto.ClienteRequest;
import br.com.lrrepresentacoes.connectcrm.dto.ClienteResponse;
import br.com.lrrepresentacoes.connectcrm.dto.MoverEtapaRequest;
import br.com.lrrepresentacoes.connectcrm.dto.NegociacaoRequest;
import br.com.lrrepresentacoes.connectcrm.dto.NegociacaoResponse;
import br.com.lrrepresentacoes.connectcrm.exception.RegraNegocioException;
import br.com.lrrepresentacoes.connectcrm.model.EstagioNegociacao;
import br.com.lrrepresentacoes.connectcrm.service.ClienteService;
import br.com.lrrepresentacoes.connectcrm.service.NegociacaoService;
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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integração com o PostgreSQL real (migrations V1 e V2 aplicadas pelo Flyway).
 * Confere o mapeamento da tabela negociacao (o estágio gravado como texto do enum e as
 * três datas) e a ligação com a regra de desativação do cliente, que conta as negociações
 * em andamento direto na tabela.
 * Cada teste roda numa transação desfeita no fim (@Transactional), então o banco não muda.
 */
@SpringBootTest
@Transactional
class NegociacaoIntegrationTest {

    // Usuário inicial criado pela V2.
    private static final String EMAIL_V2 = "revendedor@lrrepresentacoes.com.br";

    @Autowired
    private NegociacaoService negociacaoService;

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ClienteResponse bella;

    @BeforeEach
    void logarECriarCliente() {
        // Simula a requisição autenticada: o JWT leva o e-mail do usuário como nome.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(EMAIL_V2, null, List.of()));

        bella = clienteService.criar(new ClienteRequest("IT Ateliê Bella Noiva", "5562999998888",
                null, null, "Goiânia", "GO", List.of()));
    }

    @AfterEach
    void sair() {
        SecurityContextHolder.clearContext();
    }

    // Grava o que está pendente no Hibernate e lê a linha direto da tabela.
    private Map<String, Object> linha(Long idNegociacao) {
        entityManager.flush();
        return jdbcTemplate.queryForMap("""
                SELECT id_cliente, id_usuario, descricao, estagio,
                       data_criacao, data_ultima_movimentacao, data_fechamento
                FROM negociacao WHERE id_negociacao = ?
                """, idNegociacao);
    }

    @Test
    @DisplayName("CT-RF007-01: a negociação é gravada em PRIMEIRO_CONTATO, com o cliente e o usuário logado")
    void criar_gravaEmPrimeiroContatoComOUsuarioLogado() {
        NegociacaoResponse criada = negociacaoService.criar(
                new NegociacaoRequest(bella.id(), "IT mostruário de linho"));

        Map<String, Object> linha = linha(criada.id());
        Long idUsuarioV2 = jdbcTemplate.queryForObject(
                "SELECT id_usuario FROM usuario WHERE email = ?", Long.class, EMAIL_V2);
        assertThat(linha.get("id_cliente")).isEqualTo(bella.id());
        assertThat(linha.get("id_usuario")).isEqualTo(idUsuarioV2);
        assertThat(linha.get("descricao")).isEqualTo("IT mostruário de linho");
        assertThat(linha.get("estagio")).isEqualTo("PRIMEIRO_CONTATO");
        assertThat(linha.get("data_criacao")).isNotNull();
        assertThat(linha.get("data_ultima_movimentacao")).isEqualTo(linha.get("data_criacao"));
        assertThat(linha.get("data_fechamento")).isNull();
    }

    @Test
    @DisplayName("CT-RD010-01 a 03: as datas gravadas acompanham o caminho do card pelo funil")
    void moverEtapa_gravaEstagioEDatasNoBanco() {
        Long id = negociacaoService.criar(new NegociacaoRequest(bella.id(), "IT mostruário de linho")).id();

        // CT-RD010-01: etapa aberta, sem data de fechamento.
        negociacaoService.moverEtapa(id, new MoverEtapaRequest("MOSTRUARIO_ENVIADO"));
        Map<String, Object> aberta = linha(id);
        assertThat(aberta.get("estagio")).isEqualTo("MOSTRUARIO_ENVIADO");
        assertThat(aberta.get("data_fechamento")).isNull();

        // CT-RD010-02: FECHADA preenche a data de fechamento.
        negociacaoService.moverEtapa(id, new MoverEtapaRequest("FECHADA"));
        Map<String, Object> fechada = linha(id);
        assertThat(fechada.get("estagio")).isEqualTo("FECHADA");
        assertThat(fechada.get("data_fechamento")).isNotNull()
                .isEqualTo(fechada.get("data_ultima_movimentacao"));

        // CT-RD010-03: sair de FECHADA volta a data de fechamento para NULL.
        negociacaoService.moverEtapa(id, new MoverEtapaRequest("AGUARDANDO_RESPOSTA"));
        Map<String, Object> reaberta = linha(id);
        assertThat(reaberta.get("estagio")).isEqualTo("AGUARDANDO_RESPOSTA");
        assertThat(reaberta.get("data_fechamento")).isNull();
        assertThat(reaberta.get("data_criacao")).isEqualTo(aberta.get("data_criacao"));
    }

    // O CHECK ck_negociacao_estagio da V1 e o enum precisam ter os mesmos valores.
    @Test
    @DisplayName("Todos os estágios do enum são aceitos pelo CHECK da tabela negociacao")
    void moverEtapa_todosOsEstagiosDoEnumPassamNoCheckDoBanco() {
        Long id = negociacaoService.criar(new NegociacaoRequest(bella.id(), "IT mostruário de linho")).id();

        // Padrão Iterator: for-each percorre os estágios na ordem do funil (o primeiro é o inicial).
        for (EstagioNegociacao estagio : EstagioNegociacao.values()) {
            if (estagio != EstagioNegociacao.PRIMEIRO_CONTATO) {
                negociacaoService.moverEtapa(id, new MoverEtapaRequest(estagio.name()));
                assertThat(linha(id).get("estagio")).isEqualTo(estagio.name());
            }
        }
    }

    @Test
    @DisplayName("CT-RD014-01: criar negociação para cliente desativado é recusado e nada é gravado")
    void criar_clienteDesativado_recusaSemGravar() {
        clienteService.desativar(bella.id());

        assertThatThrownBy(() -> negociacaoService.criar(new NegociacaoRequest(bella.id(), "IT mostruário")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("está desativado");

        Integer gravadas = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM negociacao WHERE id_cliente = ?", Integer.class, bella.id());
        assertThat(gravadas).isZero();
    }

    // A desativação do cliente (issue #32) conta as negociações em andamento na tabela.
    @Test
    @DisplayName("CT-RF020-03: a negociação em andamento bloqueia a desativação até ser encerrada")
    void negociacaoEmAndamento_bloqueiaADesativacaoDoClienteAteSerEncerrada() {
        Long id = negociacaoService.criar(new NegociacaoRequest(bella.id(), "IT mostruário de linho")).id();
        entityManager.flush();

        assertThatThrownBy(() -> clienteService.desativar(bella.id()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("1 negociação em andamento");

        negociacaoService.moverEtapa(id, new MoverEtapaRequest("PERDIDA"));
        entityManager.flush();

        assertThat(clienteService.desativar(bella.id()).ativo()).isFalse();
    }

    // Regra 6.5: com o cliente desativado, a negociação encerrada não volta para uma etapa aberta.
    @Test
    @DisplayName("CT-RD014-01: a negociação encerrada de cliente desativado não pode ser reaberta")
    void moverEtapa_reabrirDeClienteDesativado_recusa() {
        Long id = negociacaoService.criar(new NegociacaoRequest(bella.id(), "IT mostruário de linho")).id();
        negociacaoService.moverEtapa(id, new MoverEtapaRequest("PERDIDA"));
        entityManager.flush();
        clienteService.desativar(bella.id());

        assertThatThrownBy(() -> negociacaoService.moverEtapa(id, new MoverEtapaRequest("PRIMEIRO_CONTATO")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Reative o cliente antes de reabrir a negociação");
    }
}
