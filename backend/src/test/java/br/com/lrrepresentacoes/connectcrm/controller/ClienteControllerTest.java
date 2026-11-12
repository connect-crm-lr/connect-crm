package br.com.lrrepresentacoes.connectcrm.controller;

import br.com.lrrepresentacoes.connectcrm.dto.ClienteRequest;
import br.com.lrrepresentacoes.connectcrm.dto.ClienteResponse;
import br.com.lrrepresentacoes.connectcrm.exception.ConflitoException;
import br.com.lrrepresentacoes.connectcrm.exception.GlobalExceptionHandler;
import br.com.lrrepresentacoes.connectcrm.exception.RecursoNaoEncontradoException;
import br.com.lrrepresentacoes.connectcrm.exception.RegraNegocioException;
import br.com.lrrepresentacoes.connectcrm.service.ClienteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa a requisição HTTP passando pelo Spring MVC: controller, validação (@Valid)
 * e GlobalExceptionHandler. O service é simulado com Mockito.
 */
@ExtendWith(MockitoExtension.class)
class ClienteControllerTest {

    private static final String CORPO_VALIDO = """
            {"nome": "Ateliê Bella Noiva", "telefone": "+55 (62) 99999-8888",
             "cpfCnpj": "11.222.333/0001-81", "uf": "go", "tags": ["Linho"]}
            """;

    private static final ClienteResponse ATELIE = new ClienteResponse(1L, "Ateliê Bella Noiva", "5562999998888",
            null, "11222333000181", null, "GO", true, List.of());

    @Mock
    private ClienteService clienteService;

    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ClienteController(clienteService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // Sem parâmetros: listagem padrão, só os ativos.
    @Test
    void listar_semParametros_pedeSoOsAtivos() throws Exception {
        when(clienteService.listar(null, null, false)).thenReturn(List.of(ATELIE));

        mockMvc.perform(get("/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].telefone").value("5562999998888"));
    }

    @Test
    void listar_comBuscaTagEInativos_repassaOsFiltros() throws Exception {
        mockMvc.perform(get("/clientes").param("busca", "bella").param("tag", "3").param("inativos", "true"))
                .andExpect(status().isOk());

        verify(clienteService).listar("bella", 3L, true);
    }

    // O corpo chega ao service já sem máscara; a resposta é 201.
    @Test
    void criar_valido_responde201ComTelefoneECnpjSoComDigitos() throws Exception {
        when(clienteService.criar(any(ClienteRequest.class))).thenReturn(ATELIE);

        mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        ArgumentCaptor<ClienteRequest> captor = ArgumentCaptor.forClass(ClienteRequest.class);
        verify(clienteService).criar(captor.capture());
        assertThat(captor.getValue().telefone()).isEqualTo("5562999998888");
        assertThat(captor.getValue().cpfCnpj()).isEqualTo("11222333000181");
        assertThat(captor.getValue().uf()).isEqualTo("GO");
    }

    @Test
    @DisplayName("CT-RF003-02: cadastrar sem nome responde 400 com o erro no campo nome")
    void criar_semNome_responde400NoCampoNome() throws Exception {
        mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \" \", \"telefone\": \"5562999998888\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("nome"))
                .andExpect(jsonPath("$.campos[0].mensagem").value("O nome é obrigatório"));

        verify(clienteService, never()).criar(any());
    }

    // Telefone sem o DDI 55: 400 com o erro no campo (o front mostra abaixo do campo).
    @Test
    void criar_telefoneSemDdi55_responde400NoCampoTelefone() throws Exception {
        mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"Ateliê\", \"telefone\": \"(62) 99999-8888\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("telefone"))
                .andExpect(jsonPath("$.campos[0].mensagem")
                        .value("Informe o telefone com DDI 55, DDD e número (ex.: 55 62 99999-8888)"));

        verify(clienteService, never()).criar(any());
    }

    // Critério de aceitação: CPF/CNPJ duplicado responde 409.
    @Test
    void criar_cpfCnpjDuplicado_responde409ComMensagem() throws Exception {
        when(clienteService.criar(any(ClienteRequest.class)))
                .thenThrow(new ConflitoException("Este CPF/CNPJ já está cadastrado para o cliente Casa Têxtil Moreno"));

        mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensagem")
                        .value("Este CPF/CNPJ já está cadastrado para o cliente Casa Têxtil Moreno"));
    }

    // Critério de aceitação: desativar com pedido em aberto responde 422 com a mensagem.
    @Test
    void desativar_comPedidoEmAberto_responde422ComAMensagem() throws Exception {
        String mensagem = "Não é possível desativar o cliente Ateliê Bella Noiva. Resolva antes: "
                + "1 pedido em aberto (marque como pago ou cancele).";
        when(clienteService.desativar(1L)).thenThrow(new RegraNegocioException(mensagem));

        mockMvc.perform(patch("/clientes/1/desativar"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.mensagem").value(mensagem));
    }

    @Test
    void reativar_responde200ComOCliente() throws Exception {
        when(clienteService.reativar(1L)).thenReturn(ATELIE);

        mockMvc.perform(patch("/clientes/1/reativar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void buscarPorId_inexistente_responde404() throws Exception {
        when(clienteService.buscarPorId(9L)).thenThrow(new RecursoNaoEncontradoException("Cliente não encontrado"));

        mockMvc.perform(get("/clientes/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Cliente não encontrado"));
    }
}
