package br.com.lrrepresentacoes.connectcrm.service;

import br.com.lrrepresentacoes.connectcrm.dto.ClienteRequest;
import br.com.lrrepresentacoes.connectcrm.dto.ClienteResponse;
import br.com.lrrepresentacoes.connectcrm.dto.TagResponse;
import br.com.lrrepresentacoes.connectcrm.exception.ConflitoException;
import br.com.lrrepresentacoes.connectcrm.exception.RecursoNaoEncontradoException;
import br.com.lrrepresentacoes.connectcrm.exception.RegraNegocioException;
import br.com.lrrepresentacoes.connectcrm.model.Cliente;
import br.com.lrrepresentacoes.connectcrm.model.Tag;
import br.com.lrrepresentacoes.connectcrm.model.Usuario;
import br.com.lrrepresentacoes.connectcrm.repository.ClienteRepository;
import br.com.lrrepresentacoes.connectcrm.repository.TagRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private UsuarioLogadoService usuarioLogadoService;

    @InjectMocks
    private ClienteService clienteService;

    private static final Usuario REVENDEDOR = new Usuario("Revendedor", "revendedor@exemplo.com", "hash");

    private static Cliente cliente(Long id, String nome, String cpfCnpj) {
        Cliente cliente = new Cliente(REVENDEDOR, nome, "5562999998888", null, cpfCnpj, "Goiânia", "GO");
        ReflectionTestUtils.setField(cliente, "id", id);
        return cliente;
    }

    private static ClienteRequest request(String cpfCnpj, List<String> tags) {
        return new ClienteRequest("Ateliê Bella Noiva", "+55 (62) 99999-8888", null, cpfCnpj, "Goiânia", "go", tags);
    }

    // ---------- listar ----------

    // A listagem padrão pede só os ativos, sem busca e sem tag.
    @Test
    @DisplayName("CT-RF004-03: a listagem padrão pede só os clientes ativos")
    void listar_padrao_pedeSoAtivosSemFiltro() {
        when(clienteRepository.listar("%", null, false)).thenReturn(List.of(cliente(1L, "Ateliê", null)));

        List<ClienteResponse> resposta = clienteService.listar(null, null, false);

        assertThat(resposta).extracting(ClienteResponse::nome).containsExactly("Ateliê");
        verify(clienteRepository).listar("%", null, false);
    }

    // A busca vai em minúsculas, entre '%', com curingas escapados; a tag e os inativos repassados.
    @Test
    @DisplayName("CT-RF003-04 / CT-RF004-01: busca por parte do nome e filtro por tag")
    void listar_comBuscaTagEInativos_montaOPadraoEscapado() {
        when(clienteRepository.listar(anyString(), any(), any(Boolean.class))).thenReturn(List.of());

        clienteService.listar("  BELLA_100% ", 3L, true);

        verify(clienteRepository).listar("%bella\\_100\\%%", 3L, true);
    }

    @Test
    void buscarPorId_inexistente_lancaRecursoNaoEncontrado() {
        when(clienteRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.buscarPorId(9L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Cliente não encontrado");
    }

    // ---------- criar ----------

    // Grava com o usuário logado, telefone só com dígitos e ativo.
    @Test
    @DisplayName("CT-RF003-01 / CT-RF003-03: cadastra o cliente ativo, com o telefone só com dígitos e DDI 55")
    void criar_gravaComUsuarioLogadoETelefoneSoComDigitos() {
        when(usuarioLogadoService.obter()).thenReturn(REVENDEDOR);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        ClienteResponse resposta = clienteService.criar(request(null, List.of()));

        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).save(captor.capture());
        Cliente gravado = captor.getValue();
        assertThat(gravado.getUsuario()).isSameAs(REVENDEDOR);
        assertThat(gravado.getTelefone()).isEqualTo("5562999998888");
        assertThat(gravado.getUf()).isEqualTo("GO");
        assertThat(resposta.ativo()).isTrue();
    }

    // Regra 6.4: tag existente (sem diferenciar maiúsculas) é reaproveitada; a nova é criada; repetida vira uma só.
    @Test
    void criar_reaproveitaTagExistenteCriaANovaEIgnoraRepetida() {
        Tag linho = new Tag("Linho");
        when(usuarioLogadoService.obter()).thenReturn(REVENDEDOR);
        when(tagRepository.findByNomeIgnoreCase("linho")).thenReturn(Optional.of(linho));
        when(tagRepository.findByNomeIgnoreCase("Tricoline")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> inv.getArgument(0));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        ClienteResponse resposta = clienteService.criar(request(null, List.of("linho", "Tricoline", "LINHO")));

        assertThat(resposta.tags()).extracting(TagResponse::nome).containsExactly("Linho", "Tricoline");
        verify(tagRepository, times(1)).save(any(Tag.class));
        verify(tagRepository, times(1)).findByNomeIgnoreCase("linho");
    }

    // Critério de aceitação: CPF/CNPJ duplicado responde 409 (ConflitoException) e nada é gravado.
    @Test
    void criar_cpfCnpjDeOutroCliente_lancaConflitoSemGravar() {
        when(clienteRepository.findByCpfCnpj("11222333000181"))
                .thenReturn(Optional.of(cliente(5L, "Casa Têxtil Moreno", "11222333000181")));

        assertThatThrownBy(() -> clienteService.criar(request("11.222.333/0001-81", List.of())))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("Este CPF/CNPJ já está cadastrado para o cliente Casa Têxtil Moreno");
        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    void criar_cpfCnpjDeClienteDesativado_avisaQueEleEstaDesativado() {
        Cliente desativado = cliente(5L, "Casa Têxtil Moreno", "52998224725");
        desativado.desativar();
        when(clienteRepository.findByCpfCnpj("52998224725")).thenReturn(Optional.of(desativado));

        assertThatThrownBy(() -> clienteService.criar(request("529.982.247-25", List.of())))
                .isInstanceOf(ConflitoException.class)
                .hasMessageEndingWith("Casa Têxtil Moreno (desativado)");
    }

    // ---------- atualizar ----------

    // Manter o próprio CPF/CNPJ não é conflito; as tags enviadas substituem as anteriores.
    @Test
    @DisplayName("CT-RF003-05: editar o cliente grava os novos dados")
    void atualizar_mantendoOProprioCpfCnpj_alteraDadosETrocaAsTags() {
        Cliente existente = cliente(1L, "Nome antigo", "52998224725");
        existente.definirTags(java.util.Set.of(new Tag("Denim")));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(clienteRepository.findByCpfCnpj("52998224725")).thenReturn(Optional.of(existente));
        when(tagRepository.findByNomeIgnoreCase("Malha")).thenReturn(Optional.of(new Tag("Malha")));

        ClienteResponse resposta = clienteService.atualizar(1L, request("52998224725", List.of("Malha")));

        assertThat(resposta.nome()).isEqualTo("Ateliê Bella Noiva");
        assertThat(resposta.tags()).extracting(TagResponse::nome).containsExactly("Malha");
    }

    @Test
    void atualizar_cpfCnpjDeOutroCliente_lancaConflito() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente(1L, "Ateliê", null)));
        when(clienteRepository.findByCpfCnpj("52998224725"))
                .thenReturn(Optional.of(cliente(2L, "Studio Decor SP", "52998224725")));

        assertThatThrownBy(() -> clienteService.atualizar(1L, request("52998224725", List.of())))
                .isInstanceOf(ConflitoException.class);
    }

    @Test
    void atualizar_inexistente_lancaRecursoNaoEncontrado() {
        when(clienteRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.atualizar(9L, request(null, List.of())))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // ---------- desativar ----------

    // Critério de aceitação: desativar com pedido em aberto responde 422 (RegraNegocioException) com o que resolver.
    @Test
    @DisplayName("CT-RF020-02: desativar com pedido EM_ABERTO é recusado com o que resolver")
    void desativar_comPedidoEmAberto_bloqueiaComMensagem() {
        Cliente atelie = cliente(1L, "Ateliê Bella Noiva", null);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(atelie));
        when(clienteRepository.contarPedidosEmAberto(1L)).thenReturn(1L);
        when(clienteRepository.contarNegociacoesEmAndamento(1L)).thenReturn(0L);

        assertThatThrownBy(() -> clienteService.desativar(1L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Não é possível desativar o cliente Ateliê Bella Noiva. Resolva antes: "
                        + "1 pedido em aberto (marque como pago ou cancele).");
        assertThat(atelie.isAtivo()).isTrue();
    }

    // Regra 6.5: negociação em andamento (estágio diferente de FECHADA/PERDIDA) também bloqueia.
    @Test
    @DisplayName("CT-RF020-03: desativar com negociação em andamento é recusado com o que resolver")
    void desativar_comNegociacaoEmAndamento_bloqueiaComMensagem() {
        Cliente atelie = cliente(1L, "Ateliê Bella Noiva", null);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(atelie));
        when(clienteRepository.contarPedidosEmAberto(1L)).thenReturn(0L);
        when(clienteRepository.contarNegociacoesEmAndamento(1L)).thenReturn(1L);

        assertThatThrownBy(() -> clienteService.desativar(1L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("1 negociação em andamento (mova para Fechada ou Perdida)");
        assertThat(atelie.isAtivo()).isTrue();
    }

    // Com as duas pendências, a mensagem lista as duas, no plural.
    @Test
    void mensagemDesativacaoBloqueada_listaAsDuasPendenciasNoPlural() {
        assertThat(ClienteService.mensagemDesativacaoBloqueada("Studio Decor SP", 2, 3))
                .isEqualTo("Não é possível desativar o cliente Studio Decor SP. Resolva antes: "
                        + "2 pedidos em aberto (marque como pago ou cancele) e "
                        + "3 negociações em andamento (mova para Fechada ou Perdida).");
    }

    @Test
    @DisplayName("CT-RF020-01: desativar cliente sem pendências marca ativo = false")
    void desativar_semPendencias_desativa() {
        Cliente atelie = cliente(1L, "Ateliê Bella Noiva", null);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(atelie));
        when(clienteRepository.contarPedidosEmAberto(1L)).thenReturn(0L);
        when(clienteRepository.contarNegociacoesEmAndamento(1L)).thenReturn(0L);

        ClienteResponse resposta = clienteService.desativar(1L);

        assertThat(resposta.ativo()).isFalse();
        assertThat(atelie.isAtivo()).isFalse();
    }

    @Test
    void desativar_jaDesativado_recusaSemConsultarPendencias() {
        Cliente atelie = cliente(1L, "Ateliê Bella Noiva", null);
        atelie.desativar();
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(atelie));

        assertThatThrownBy(() -> clienteService.desativar(1L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("O cliente já está desativado");
        verify(clienteRepository, never()).contarPedidosEmAberto(anyLong());
    }

    // ---------- reativar (RF-024) ----------

    @Test
    @DisplayName("CT-RF024-01: reativar cliente desativado volta para ativo = true")
    void reativar_desativado_voltaAAtivo() {
        Cliente atelie = cliente(1L, "Ateliê Bella Noiva", null);
        atelie.desativar();
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(atelie));

        assertThat(clienteService.reativar(1L).ativo()).isTrue();
    }

    @Test
    void reativar_jaAtivo_recusa() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente(1L, "Ateliê", null)));

        assertThatThrownBy(() -> clienteService.reativar(1L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("O cliente já está ativo");
    }
}
