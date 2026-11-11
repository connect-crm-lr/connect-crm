package br.com.lrrepresentacoes.connectcrm.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integração da segurança: sobe a aplicação inteira, com o banco do Docker
 * (usuário inicial da V2) e o filtro do Spring Security.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SegurancaIntegracaoTest {

    // Senha só de desenvolvimento do usuário inicial (V2). Não é a senha real.
    private static final String LOGIN_V2 =
            "{\"email\": \"revendedor@lrrepresentacoes.com.br\", \"senha\": \"Trocar@123\"}";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rotaProtegida_semToken_responde401() throws Exception {
        mockMvc.perform(get("/tags"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Faça login para continuar"));
    }

    @Test
    void login_comSenhaErrada_responde401() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"revendedor@lrrepresentacoes.com.br\", \"senha\": \"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos"));
    }

    @Test
    void login_comUsuarioDaV2_devolveTokenQueAbreARotaProtegida() throws Exception {
        String corpo = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_V2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andReturn().getResponse().getContentAsString();

        Matcher m = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"").matcher(corpo);
        assertThat(m.find()).isTrue();

        mockMvc.perform(get("/tags").header("Authorization", "Bearer " + m.group(1)))
                .andExpect(status().isOk());
    }

    @Test
    void tokenFalsificado_responde401() throws Exception {
        mockMvc.perform(get("/tags").header("Authorization", "Bearer token.falso.qualquer"))
                .andExpect(status().isUnauthorized());
    }
}
