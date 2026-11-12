package br.com.lrrepresentacoes.connectcrm.repository;

import br.com.lrrepresentacoes.connectcrm.dto.ClienteRequest;
import br.com.lrrepresentacoes.connectcrm.dto.ClienteResponse;
import br.com.lrrepresentacoes.connectcrm.dto.FollowUpRequest;
import br.com.lrrepresentacoes.connectcrm.dto.FollowUpResponse;
import br.com.lrrepresentacoes.connectcrm.dto.InteracaoRequest;
import br.com.lrrepresentacoes.connectcrm.dto.InteracaoResponse;
import br.com.lrrepresentacoes.connectcrm.model.TipoInteracao;
import br.com.lrrepresentacoes.connectcrm.service.ClienteService;
import br.com.lrrepresentacoes.connectcrm.service.FollowUpService;
import br.com.lrrepresentacoes.connectcrm.service.InteracaoService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integração com o PostgreSQL real (migrations V1 e V2 aplicadas pelo Flyway).
 * Confere o mapeamento das tabelas interacao e follow_up e, principalmente, a consulta de
 * "Pendências de hoje", que é onde a regra RD-011 está escrita.
 * Cada teste roda numa transação desfeita no fim (@Transactional), então o banco não muda.
 * Os nomes usam o prefixo "IT " para não colidir com dados já existentes no banco local.
 */
@SpringBootTest
@Transactional
class InteracaoFollowUpIntegrationTest {

    // Usuário inicial criado pela V2.
    private static final String EMAIL_V2 = "revendedor@lrrepresentacoes.com.br";

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    @Autowired
    private InteracaoService interacaoService;

    @Autowired
    private FollowUpService followUpService;

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ClienteResponse bella;
    private LocalDate hoje;

    @BeforeEach
    void logarECriarCliente() {
        // Simula a requisição autenticada: o JWT leva o e-mail do usuário como nome.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(EMAIL_V2, null, List.of()));

        bella = clienteService.criar(new ClienteRequest("IT Ateliê Bella Noiva", "5562999998888",
                null, null, "Goiânia", "GO", List.of()));
        hoje = LocalDate.now(FUSO);
    }

    @AfterEach
    void sair() {
        SecurityContextHolder.clearContext();
    }

    private Long idUsuarioV2() {
        return jdbcTemplate.queryForObject("SELECT id_usuario FROM usuario WHERE email = ?", Long.class, EMAIL_V2);
    }

    // Só as pendências criadas por este teste: o banco local pode ter outras.
    private List<String> pendenciasDoTeste() {
        return followUpService.listarPendenciasDeHoje().stream()
                .filter(f -> f.descricao().startsWith("IT "))
                .map(FollowUpResponse::descricao)
                .toList();
    }

    // ---------- interações ----------

    @Test
    @DisplayName("CT-RF011-01: a interação é gravada na tabela com tipo, data, descrição, cliente e usuário logado")
    void registrarInteracao_gravaNaTabelaComOUsuarioLogado() {
        InteracaoResponse resposta = interacaoService.registrar(bella.id(), new InteracaoRequest(
                "WHATSAPP", hoje.minusDays(1), "IT enviei o mostruário de linho"));
        entityManager.flush();

        var linha = jdbcTemplate.queryForMap(
                "SELECT id_cliente, id_usuario, tipo, data_interacao, descricao FROM interacao WHERE id_interacao = ?",
                resposta.id());
        assertThat(linha.get("id_cliente")).isEqualTo(bella.id());
        assertThat(linha.get("id_usuario")).isEqualTo(idUsuarioV2());
        assertThat(linha.get("tipo")).isEqualTo("WHATSAPP");
        assertThat(linha.get("data_interacao")).hasToString(hoje.minusDays(1).toString());
        assertThat(linha.get("descricao")).isEqualTo("IT enviei o mostruário de linho");
    }

    @Test
    @DisplayName("CT-RF011-02: o banco só aceita os tipos WHATSAPP, LIGACAO, VISITA e OUTRO (ck_interacao_tipo)")
    void interacao_tipoForaDaLista_oBancoRecusa() {
        // Padrão Iterator: for-each percorre os valores do enum; todos passam no CHECK da V1.
        for (TipoInteracao tipo : TipoInteracao.values()) {
            interacaoService.registrar(bella.id(), new InteracaoRequest(tipo.name(), hoje, "IT " + tipo));
        }
        entityManager.flush();
        assertThat(interacaoService.listarDoCliente(bella.id())).hasSize(4);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO interacao (id_cliente, id_usuario, tipo, data_interacao, descricao) "
                        + "VALUES (?, ?, 'EMAIL', CURRENT_DATE, 'IT e-mail')", bella.id(), idUsuarioV2()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Listar interações traz só as do cliente, a mais recente primeiro")
    void listarInteracoes_soDoClienteEMaisRecentePrimeiro() {
        ClienteResponse outro = clienteService.criar(new ClienteRequest("IT Casa Têxtil Moreno", "551134567890",
                null, null, null, null, List.of()));
        interacaoService.registrar(bella.id(), new InteracaoRequest("LIGACAO", hoje.minusDays(5), "IT antiga"));
        interacaoService.registrar(bella.id(), new InteracaoRequest("VISITA", hoje, "IT recente"));
        interacaoService.registrar(outro.id(), new InteracaoRequest("OUTRO", hoje, "IT de outro cliente"));

        assertThat(interacaoService.listarDoCliente(bella.id()))
                .extracting(InteracaoResponse::descricao)
                .containsExactly("IT recente", "IT antiga");
    }

    // ---------- follow-ups ----------

    @Test
    @DisplayName("CT-RF012-01: o follow-up é gravado com data_conclusao vazia e o usuário logado")
    void criarFollowUp_gravaPendenteComOUsuarioLogado() {
        FollowUpResponse resposta = followUpService.criar(bella.id(),
                new FollowUpRequest(hoje.plusDays(3), "IT retornar sobre o mostruário"));
        entityManager.flush();

        var linha = jdbcTemplate.queryForMap(
                "SELECT id_cliente, id_usuario, data_prevista, data_conclusao FROM follow_up WHERE id_follow_up = ?",
                resposta.id());
        assertThat(linha.get("id_cliente")).isEqualTo(bella.id());
        assertThat(linha.get("id_usuario")).isEqualTo(idUsuarioV2());
        assertThat(linha.get("data_prevista")).hasToString(hoje.plusDays(3).toString());
        assertThat(linha.get("data_conclusao")).isNull();
    }

    @Test
    @DisplayName("CT-RF012-03: concluir grava a data_conclusao no banco")
    void concluirFollowUp_gravaADataDeConclusao() {
        FollowUpResponse criado = followUpService.criar(bella.id(), new FollowUpRequest(hoje, "IT ligar hoje"));

        followUpService.concluir(criado.id());
        entityManager.flush();

        Integer concluidos = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM follow_up WHERE id_follow_up = ? AND data_conclusao IS NOT NULL",
                Integer.class, criado.id());
        assertThat(concluidos).isEqualTo(1);
    }

    // ---------- pendências de hoje (RD-011) ----------

    @Test
    @DisplayName("CT-RD011-01: follow-ups de ontem e de hoje aparecem nas pendências; o de amanhã não")
    void pendencias_mostraOntemEHoje_naoMostraAmanha() {
        followUpService.criar(bella.id(), new FollowUpRequest(hoje.minusDays(1), "IT de ontem"));
        followUpService.criar(bella.id(), new FollowUpRequest(hoje, "IT de hoje"));
        followUpService.criar(bella.id(), new FollowUpRequest(hoje.plusDays(1), "IT de amanhã"));

        // Em ordem de data prevista: o atrasado vem primeiro.
        assertThat(pendenciasDoTeste()).containsExactly("IT de ontem", "IT de hoje");
    }

    @Test
    @DisplayName("CT-RD011-02: follow-up já concluído não aparece nas pendências")
    void pendencias_naoMostraConcluido() {
        FollowUpResponse concluido = followUpService.criar(bella.id(), new FollowUpRequest(hoje, "IT concluído"));
        followUpService.criar(bella.id(), new FollowUpRequest(hoje, "IT pendente"));

        followUpService.concluir(concluido.id());

        assertThat(pendenciasDoTeste()).containsExactly("IT pendente");
    }

    @Test
    @DisplayName("CT-RD011-03: follow-up de cliente desativado não aparece nas pendências")
    void pendencias_naoMostraDeClienteDesativado() {
        ClienteResponse moreno = clienteService.criar(new ClienteRequest("IT Casa Têxtil Moreno", "551134567890",
                null, null, null, null, List.of()));
        followUpService.criar(bella.id(), new FollowUpRequest(hoje, "IT da cliente ativa"));
        followUpService.criar(moreno.id(), new FollowUpRequest(hoje, "IT do cliente desativado"));

        clienteService.desativar(moreno.id());

        assertThat(pendenciasDoTeste()).containsExactly("IT da cliente ativa");
    }

    // Regra 6.5: o follow-up pendente não bloqueia a desativação; ele sai das pendências,
    // continua na ficha do cliente e volta para a lista se o cliente for reativado.
    @Test
    @DisplayName("CT-RD016-01: desativar o cliente tira o follow-up das pendências sem apagá-lo")
    void pendencias_followUpSaiAoDesativarEVoltaAoReativar() {
        followUpService.criar(bella.id(), new FollowUpRequest(hoje, "IT pendente da Bella"));
        assertThat(pendenciasDoTeste()).containsExactly("IT pendente da Bella");

        clienteService.desativar(bella.id());
        assertThat(pendenciasDoTeste()).isEmpty();
        assertThat(followUpService.listarDoCliente(bella.id())).hasSize(1);

        clienteService.reativar(bella.id());
        assertThat(pendenciasDoTeste()).containsExactly("IT pendente da Bella");
    }
}
