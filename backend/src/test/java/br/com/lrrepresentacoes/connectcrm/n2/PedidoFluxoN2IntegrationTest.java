package br.com.lrrepresentacoes.connectcrm.n2;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Avaliação N2 (Teste de Software) — caso complementar C-01 (INTEGRAÇÃO).
 *
 * Partes reais trabalhando juntas: Spring Security (login e token JWT), controllers REST,
 * validação dos DTOs, services com @Transactional, repositórios JPA e o PostgreSQL do Docker
 * (migrations V1 e V2 aplicadas pelo Flyway). Nada é simulado: as chamadas passam pelo
 * MockMvc, que entrega a requisição HTTP à aplicação inteira sem abrir uma porta de rede.
 *
 * Fluxo: login → cadastra cliente, tecido e entrada de 40 m → confirma pedido de 12,5 m →
 * consulta o estoque (27,5 m) → cancela o pedido → consulta o estoque de novo (40 m).
 *
 * O teste roda dentro de uma transação desfeita no fim (@Transactional), então nada do que
 * ele cria fica no banco.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("N2 — Fluxo do pedido pela API com banco real (integração)")
class PedidoFluxoN2IntegrationTest {

    // Usuário inicial da migration V2. Senha só de desenvolvimento, a mesma do SegurancaIntegracaoTest.
    private static final String LOGIN_V2 =
            "{\"email\": \"revendedor@lrrepresentacoes.com.br\", \"senha\": \"Trocar@123\"}";

    @Autowired
    private MockMvc mockMvc;

    private String autorizacao;

    @BeforeEach
    void fazerLogin() throws Exception {
        String resposta = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_V2))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(resposta, "$.token");
        assertThat(token).isNotBlank();
        autorizacao = "Bearer " + token;
    }

    // Envia a requisição com o token, confere o status HTTP e devolve o corpo da resposta.
    private String chamar(MockHttpServletRequestBuilder requisicao, int statusEsperado) throws Exception {
        return mockMvc.perform(requisicao.header("Authorization", autorizacao))
                .andExpect(status().is(statusEsperado))
                .andReturn().getResponse().getContentAsString();
    }

    private String postJson(String url, String corpo, int statusEsperado) throws Exception {
        return chamar(post(url).contentType(MediaType.APPLICATION_JSON).content(corpo), statusEsperado);
    }

    private static Long id(String json, String caminho) {
        return ((Number) JsonPath.read(json, caminho)).longValue();
    }

    private static BigDecimal numero(String json, String caminho) {
        Object valor = JsonPath.read(json, caminho);
        return new BigDecimal(valor.toString());
    }

    // Saldo da linha de estoque consultado pela própria API (GET /estoque?busca=...).
    private BigDecimal saldoPelaApi(Long idEstoque) throws Exception {
        String lista = chamar(get("/estoque").param("busca", "N2 Tricoline"), 200);
        List<Object> saldos = JsonPath.read(lista, "$[?(@.id == " + idEstoque + ")].metragem");
        assertThat(saldos).hasSize(1);
        return new BigDecimal(saldos.get(0).toString());
    }

    @Test
    @DisplayName("C-01: pedido pela API desconta o estoque no banco e o cancelamento devolve a metragem")
    void c01_pedidoDescontaEstoqueECancelamentoDevolve() throws Exception {
        // 1) Dados iniciais, cadastrados pela própria API.
        String cliente = postJson("/clientes", """
                {"nome": "N2 Boutique Teste", "telefone": "5562999990000", "email": null,
                 "cpfCnpj": null, "cidade": "Goiânia", "uf": "GO", "tags": []}
                """, 201);
        Long idCliente = id(cliente, "$.id");

        String tecido = postJson("/tecidos", """
                {"nome": "N2 Tricoline Teste", "composicao": "100% Algodão", "gramatura": 110,
                 "largura": 1.50, "precoMetro": 21.90, "fotoUrl": null}
                """, 201);
        Long idTecido = id(tecido, "$.id");

        String entrada = postJson("/estoque/entradas", """
                {"idTecido": %d, "corCodigo": "07 Bege", "metragem": 40.00, "dataEntrada": null}
                """.formatted(idTecido), 201);
        Long idEstoque = id(entrada, "$.idEstoque");
        assertThat(numero(entrada, "$.saldoAtual")).isEqualByComparingTo("40.00");

        // 2) Confirma o pedido de 12,5 m: 12,5 × R$ 21,90 = R$ 273,75.
        String pedido = postJson("/pedidos", """
                {"idCliente": %d, "formaPagamento": "BOLETO",
                 "itens": [{"idEstoque": %d, "metragem": 12.50}]}
                """.formatted(idCliente, idEstoque), 201);
        Long idPedido = id(pedido, "$.id");
        assertThat((String) JsonPath.read(pedido, "$.status")).isEqualTo("EM_ABERTO");
        assertThat(numero(pedido, "$.total")).isEqualByComparingTo("273.75");
        assertThat(numero(pedido, "$.itens[0].precoMetro")).isEqualByComparingTo("21.90");

        // 3) A consulta do estoque mostra a baixa: 40 − 12,5 = 27,5 m.
        assertThat(saldoPelaApi(idEstoque)).isEqualByComparingTo("27.50");

        // 4) Cancela o pedido: o status muda e a metragem volta ao estoque.
        String cancelado = chamar(patch("/pedidos/" + idPedido + "/cancelar"), 200);
        assertThat((String) JsonPath.read(cancelado, "$.status")).isEqualTo("CANCELADO");
        assertThat(saldoPelaApi(idEstoque)).isEqualByComparingTo("40.00");

        // 5) O pedido cancelado continua no banco (RD-008): a consulta por id ainda o encontra.
        String consultado = chamar(get("/pedidos/" + idPedido), 200);
        assertThat((String) JsonPath.read(consultado, "$.status")).isEqualTo("CANCELADO");
    }
}
