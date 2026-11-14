package br.com.lrrepresentacoes.connectcrm.repository;

import br.com.lrrepresentacoes.connectcrm.model.EstagioNegociacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integração com o PostgreSQL real: confere as consultas do Dashboard (JPQL validada pelo
 * Hibernate e executada no banco). Os dados entram por SQL para fixar data_pedido e estágio.
 * O banco pode já ter outros dados, então os testes comparam a diferença (antes x depois).
 * Cada teste roda numa transação desfeita no fim (@Transactional), então o banco não muda.
 */
@SpringBootTest
@Transactional
class DashboardIntegrationTest {

    // Usuário inicial criado pela V2.
    private static final String EMAIL_V2 = "revendedor@lrrepresentacoes.com.br";

    // Mês fixo e afastado de dados reais, para não depender do dia em que o teste roda.
    private static final LocalDateTime INICIO = LocalDateTime.of(2030, 3, 1, 0, 0);
    private static final LocalDateTime FIM = LocalDateTime.of(2030, 4, 1, 0, 0);

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private NegociacaoRepository negociacaoRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long idUsuario;
    private Long idCliente;

    @BeforeEach
    void criarCliente() {
        idUsuario = jdbcTemplate.queryForObject("SELECT id_usuario FROM usuario WHERE email = ?",
                Long.class, EMAIL_V2);
        idCliente = jdbcTemplate.queryForObject("""
                INSERT INTO cliente (id_usuario, nome, telefone) VALUES (?, 'IT Dashboard', '5562999990000')
                RETURNING id_cliente
                """, Long.class, idUsuario);
    }

    private void pedido(LocalDateTime data, String status, String total) {
        jdbcTemplate.update("""
                INSERT INTO pedido (id_cliente, id_usuario, data_pedido, forma_pagamento, status, total)
                VALUES (?, ?, ?, 'A_VISTA', ?, ?)
                """, idCliente, idUsuario, data, status, new BigDecimal(total));
    }

    // CT-RF021-01: soma só pedidos do mês e sem os cancelados; o limite final é exclusivo.
    @Test
    void somarVendasEntre_ignoraCanceladosEPedidosForaDoMes() {
        BigDecimal antes = pedidoRepository.somarVendasEntre(INICIO, FIM);
        long qtdAntes = pedidoRepository.contarVendasEntre(INICIO, FIM);

        pedido(INICIO, "PAGO", "100.00");                         // primeiro instante do mês: entra
        pedido(INICIO.plusDays(10), "EM_ABERTO", "50.50");        // em aberto também é venda: entra
        pedido(INICIO.plusDays(5), "CANCELADO", "999.00");        // cancelado: fora (regra 6.2)
        pedido(INICIO.minusSeconds(1), "PAGO", "300.00");         // mês anterior: fora
        pedido(FIM, "PAGO", "400.00");                            // primeiro instante do mês seguinte: fora

        assertThat(pedidoRepository.somarVendasEntre(INICIO, FIM).subtract(antes))
                .isEqualByComparingTo("150.50");
        assertThat(pedidoRepository.contarVendasEntre(INICIO, FIM) - qtdAntes).isEqualTo(2);
    }

    // CT-RF021-02: a contagem agrupa por estágio e devolve o enum.
    @Test
    void contarPorEstagio_agrupaAsNegociacoesPorEtapa() {
        Map<EstagioNegociacao, Long> antes = contagem();

        for (String estagio : List.of("AGUARDANDO_RESPOSTA", "AGUARDANDO_RESPOSTA", "PERDIDA")) {
            jdbcTemplate.update("""
                    INSERT INTO negociacao (id_cliente, id_usuario, descricao, estagio)
                    VALUES (?, ?, 'IT negociação', ?)
                    """, idCliente, idUsuario, estagio);
        }

        Map<EstagioNegociacao, Long> depois = contagem();
        assertThat(depois.getOrDefault(EstagioNegociacao.AGUARDANDO_RESPOSTA, 0L)
                - antes.getOrDefault(EstagioNegociacao.AGUARDANDO_RESPOSTA, 0L)).isEqualTo(2);
        assertThat(depois.getOrDefault(EstagioNegociacao.PERDIDA, 0L)
                - antes.getOrDefault(EstagioNegociacao.PERDIDA, 0L)).isEqualTo(1);
    }

    private Map<EstagioNegociacao, Long> contagem() {
        return negociacaoRepository.contarPorEstagio().stream()
                .collect(Collectors.toMap(l -> (EstagioNegociacao) l[0], l -> ((Number) l[1]).longValue()));
    }
}
