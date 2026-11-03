package br.com.lrrepresentacoes.connectcrm.controller;

import br.com.lrrepresentacoes.connectcrm.exception.GlobalExceptionHandler;
import br.com.lrrepresentacoes.connectcrm.exception.RecursoNaoEncontradoException;
import br.com.lrrepresentacoes.connectcrm.service.TagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa a requisição HTTP passando pelo Spring MVC: controller, validação (@Valid)
 * e GlobalExceptionHandler. O service é simulado com Mockito.
 */
@ExtendWith(MockitoExtension.class)
class TagControllerTest {

    @Mock
    private TagService tagService;

    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TagController(tagService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void criar_comNomeVazio_responde400ComOErroDoCampo() throws Exception {
        mockMvc.perform(post("/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.campos[0].campo").value("nome"))
                .andExpect(jsonPath("$.campos[0].mensagem").value("O nome da tag é obrigatório"));
    }

    @Test
    void excluir_tagInexistente_responde404ComMensagemClara() throws Exception {
        doThrow(new RecursoNaoEncontradoException("Tag não encontrada")).when(tagService).excluir(999L);

        mockMvc.perform(delete("/tags/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensagem").value("Tag não encontrada"))
                .andExpect(jsonPath("$.caminho").value("/tags/999"));
    }
}
