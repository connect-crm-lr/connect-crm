package br.com.lrrepresentacoes.connectcrm.repository;

import br.com.lrrepresentacoes.connectcrm.dto.ClienteRequest;
import br.com.lrrepresentacoes.connectcrm.dto.ClienteResponse;
import br.com.lrrepresentacoes.connectcrm.dto.TagResponse;
import br.com.lrrepresentacoes.connectcrm.exception.ConflitoException;
import br.com.lrrepresentacoes.connectcrm.exception.RegraNegocioException;
import br.com.lrrepresentacoes.connectcrm.service.ClienteService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integração com o PostgreSQL real (migrations V1 e V2 aplicadas pelo Flyway).
 * Confere a consulta de listagem, as tags, o UNIQUE do CPF/CNPJ e o bloqueio da
 * desativação com pedidos e negociações gravados de verdade nas tabelas da V1.
 * Cada teste roda numa transação desfeita no fim (@Transactional), então o banco não muda.
 * Os nomes usam o prefixo "IT " para não colidir com dados já existentes no banco local.
 */
@SpringBootTest
@Transactional
class ClienteIntegrationTest {

    // Usuário inicial criado pela V2.
    private static final String EMAIL_V2 = "revendedor@lrrepresentacoes.com.br";

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ClienteResponse bella;
    private ClienteResponse moreno;

    @BeforeEach
    void logarECriarClientes() {
        // Simula a requisição autenticada: o JWT leva o e-mail do usuário como nome.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(EMAIL_V2, null, List.of()));

        bella = clienteService.criar(new ClienteRequest("IT Ateliê Bella Noiva", "+55 (62) 99999-8888",
                null, null, "Goiânia", "go", List.of("IT Noivas", "it noivas")));
        moreno = clienteService.criar(new ClienteRequest("IT Casa Têxtil Moreno", "55 11 3456-7890",
                null, "11.222.333/0001-81", "São Paulo", "SP", List.of("IT Estofaria")));
        // Grava a cliente_tag agora, para as consultas via JdbcTemplate enxergarem os vínculos.
        clienteRepository.flush();
    }

    @AfterEach
    void sair() {
        SecurityContextHolder.clearContext();
    }

    private Long idUsuarioV2() {
        return jdbcTemplate.queryForObject("SELECT id_usuario FROM usuario WHERE email = ?", Long.class, EMAIL_V2);
    }

    // Critério de aceitação: telefone gravado só com dígitos e DDI 55; cliente gravado com o usuário logado.
    @Test
    void criar_gravaTelefoneSoComDigitosEOUsuarioLogado() {
        var linha = jdbcTemplate.queryForMap(
                "SELECT telefone, cpf_cnpj, uf, id_usuario, ativo FROM cliente WHERE id_cliente = ?", moreno.id());

        assertThat(linha.get("telefone")).isEqualTo("551134567890");
        assertThat(linha.get("cpf_cnpj")).isEqualTo("11222333000181");
        assertThat(linha.get("uf")).isEqualTo("SP");
        assertThat(linha.get("id_usuario")).isEqualTo(idUsuarioV2());
        assertThat(linha.get("ativo")).isEqualTo(true);
    }

    // Regra 6.4: "IT Noivas" e "it noivas" viram uma tag só, gravada uma vez na cliente_tag.
    @Test
    void criar_tagRepetidaViraUmaSoNaClienteTag() {
        Integer vinculos = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cliente_tag WHERE id_cliente = ?", Integer.class, bella.id());

        assertThat(vinculos).isEqualTo(1);
        assertThat(clienteService.buscarPorId(bella.id()).tags())
                .extracting(TagResponse::nome).containsExactly("IT Noivas");
    }

    // CT-RF004: busca no nome sem diferenciar maiúsculas.
    @Test
    void listar_buscaPeloNomeSemDiferenciarMaiusculas() {
        assertThat(clienteService.listar("BELLA", null, false))
                .extracting(ClienteResponse::nome)
                .contains("IT Ateliê Bella Noiva")
                .doesNotContain("IT Casa Têxtil Moreno");
    }

    // Filtro por tag: só quem tem a tag; as tags do cliente vêm na resposta.
    @Test
    void listar_filtraPelaTag() {
        Long idEstofaria = moreno.tags().getFirst().id();

        List<ClienteResponse> resultado = clienteService.listar(null, idEstofaria, false);

        assertThat(resultado).extracting(ClienteResponse::nome)
                .contains("IT Casa Têxtil Moreno")
                .doesNotContain("IT Ateliê Bella Noiva");
        assertThat(resultado).allSatisfy(c ->
                assertThat(c.tags()).extracting(TagResponse::id).contains(idEstofaria));
    }

    // Critério de aceitação: a listagem padrão mostra só os ativos; com "inativos", o desativado aparece.
    @Test
    void listar_padraoEscondeDesativados_eComInativosMostra() {
        clienteService.desativar(bella.id());

        assertThat(clienteService.listar("it ", null, false))
                .extracting(ClienteResponse::nome).doesNotContain("IT Ateliê Bella Noiva");
        assertThat(clienteService.listar("it ", null, true))
                .extracting(ClienteResponse::nome).contains("IT Ateliê Bella Noiva", "IT Casa Têxtil Moreno");
    }

    // Critério de aceitação: CPF/CNPJ duplicado é recusado (409), com ou sem máscara.
    @Test
    void criar_cpfCnpjJaCadastrado_recusa() {
        assertThatThrownBy(() -> clienteService.criar(new ClienteRequest("IT Outro", "5562988887777",
                null, "11222333000181", null, null, List.of())))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("IT Casa Têxtil Moreno");
    }

    // Critério de aceitação: com pedido EM_ABERTO gravado no banco, a desativação é bloqueada.
    @Test
    void desativar_comPedidoEmAbertoNoBanco_bloqueia() {
        jdbcTemplate.update("INSERT INTO pedido (id_cliente, id_usuario, forma_pagamento, status, total) "
                + "VALUES (?, ?, 'BOLETO', 'EM_ABERTO', 578.00)", bella.id(), idUsuarioV2());

        assertThatThrownBy(() -> clienteService.desativar(bella.id()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("1 pedido em aberto");
    }

    // Regra 6.5: negociação em andamento bloqueia; FECHADA/PERDIDA e pedido PAGO/CANCELADO não bloqueiam.
    @Test
    void desativar_soConsideraNegociacoesEmAndamentoEPedidosEmAberto() {
        Long idUsuario = idUsuarioV2();
        jdbcTemplate.update("INSERT INTO negociacao (id_cliente, id_usuario, descricao, estagio) "
                + "VALUES (?, ?, 'IT proposta', 'MOSTRUARIO_ENVIADO')", bella.id(), idUsuario);

        assertThatThrownBy(() -> clienteService.desativar(bella.id()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("1 negociação em andamento");

        jdbcTemplate.update("UPDATE negociacao SET estagio = 'PERDIDA' WHERE id_cliente = ?", bella.id());
        jdbcTemplate.update("INSERT INTO negociacao (id_cliente, id_usuario, descricao, estagio) "
                + "VALUES (?, ?, 'IT fechada', 'FECHADA')", bella.id(), idUsuario);
        jdbcTemplate.update("INSERT INTO pedido (id_cliente, id_usuario, forma_pagamento, status, total) "
                + "VALUES (?, ?, 'A_VISTA', 'PAGO', 100.00), (?, ?, 'CHEQUE', 'CANCELADO', 50.00)",
                bella.id(), idUsuario, bella.id(), idUsuario);

        assertThat(clienteService.desativar(bella.id()).ativo()).isFalse();
    }

    // RF-024: o cliente desativado pode ser reativado.
    @Test
    void reativar_clienteDesativado_voltaParaAListagemPadrao() {
        clienteService.desativar(moreno.id());
        clienteService.reativar(moreno.id());

        assertThat(clienteService.listar("moreno", null, false))
                .extracting(ClienteResponse::nome).contains("IT Casa Têxtil Moreno");
    }
}
