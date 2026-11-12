package br.com.lrrepresentacoes.connectcrm.service;

import br.com.lrrepresentacoes.connectcrm.dto.InteracaoRequest;
import br.com.lrrepresentacoes.connectcrm.dto.InteracaoResponse;
import br.com.lrrepresentacoes.connectcrm.exception.RecursoNaoEncontradoException;
import br.com.lrrepresentacoes.connectcrm.model.Cliente;
import br.com.lrrepresentacoes.connectcrm.model.Interacao;
import br.com.lrrepresentacoes.connectcrm.model.TipoInteracao;
import br.com.lrrepresentacoes.connectcrm.model.Usuario;
import br.com.lrrepresentacoes.connectcrm.repository.ClienteRepository;
import br.com.lrrepresentacoes.connectcrm.repository.InteracaoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InteracaoServiceTest {

    @Mock
    private InteracaoRepository interacaoRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private UsuarioLogadoService usuarioLogadoService;

    @InjectMocks
    private InteracaoService interacaoService;

    private static final Usuario REVENDEDOR = new Usuario("Revendedor", "revendedor@exemplo.com", "hash");

    private static Cliente cliente(Long id) {
        Cliente cliente = new Cliente(REVENDEDOR, "Ateliê Bella Noiva", "5562999998888", null, null, "Goiânia", "GO");
        ReflectionTestUtils.setField(cliente, "id", id);
        return cliente;
    }

    @Test
    @DisplayName("CT-RF011-01: registrar interação grava tipo, data e descrição no cliente, com o usuário logado")
    void registrar_gravaNoClienteComOUsuarioLogado() {
        Cliente atelie = cliente(1L);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(atelie));
        when(usuarioLogadoService.obter()).thenReturn(REVENDEDOR);
        when(interacaoRepository.save(any(Interacao.class))).thenAnswer(inv -> inv.getArgument(0));

        InteracaoResponse resposta = interacaoService.registrar(1L, new InteracaoRequest(
                "WHATSAPP", LocalDate.of(2026, 11, 3), "  Enviei o mostruário de linho  "));

        ArgumentCaptor<Interacao> captor = ArgumentCaptor.forClass(Interacao.class);
        verify(interacaoRepository).save(captor.capture());
        Interacao gravada = captor.getValue();
        assertThat(gravada.getCliente()).isSameAs(atelie);
        assertThat(gravada.getUsuario()).isSameAs(REVENDEDOR);
        assertThat(gravada.getTipo()).isEqualTo(TipoInteracao.WHATSAPP);
        assertThat(gravada.getDataInteracao()).isEqualTo(LocalDate.of(2026, 11, 3));
        assertThat(gravada.getDescricao()).isEqualTo("Enviei o mostruário de linho");
        assertThat(resposta.idCliente()).isEqualTo(1L);
        assertThat(resposta.tipo()).isEqualTo(TipoInteracao.WHATSAPP);
    }

    // Critério de aceitação: os quatro tipos do enum são aceitos pelo service.
    @ParameterizedTest
    @EnumSource(TipoInteracao.class)
    @DisplayName("CT-RF011-01: aceita os tipos WHATSAPP, LIGACAO, VISITA e OUTRO")
    void registrar_aceitaTodosOsTiposDoEnum(TipoInteracao tipo) {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente(1L)));
        when(usuarioLogadoService.obter()).thenReturn(REVENDEDOR);
        when(interacaoRepository.save(any(Interacao.class))).thenAnswer(inv -> inv.getArgument(0));

        InteracaoResponse resposta = interacaoService.registrar(1L,
                new InteracaoRequest(tipo.name(), LocalDate.of(2026, 11, 3), "Conversa"));

        assertThat(resposta.tipo()).isEqualTo(tipo);
    }

    @Test
    @DisplayName("Registrar interação para cliente que não existe responde não encontrado e não grava")
    void registrar_clienteInexistente_lancaRecursoNaoEncontrado() {
        when(clienteRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interacaoService.registrar(9L,
                new InteracaoRequest("LIGACAO", LocalDate.of(2026, 11, 3), "Liguei")))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Cliente não encontrado");
        verify(interacaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Listar devolve as interações do cliente na ordem do repository (mais recente primeiro)")
    void listarDoCliente_devolveAsInteracoesDoCliente() {
        Cliente atelie = cliente(1L);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(atelie));
        when(interacaoRepository.findByClienteIdOrderByDataInteracaoDescIdDesc(1L)).thenReturn(List.of(
                new Interacao(atelie, REVENDEDOR, TipoInteracao.VISITA, LocalDate.of(2026, 11, 5), "Visita"),
                new Interacao(atelie, REVENDEDOR, TipoInteracao.LIGACAO, LocalDate.of(2026, 11, 1), "Ligação")));

        List<InteracaoResponse> resposta = interacaoService.listarDoCliente(1L);

        assertThat(resposta).extracting(InteracaoResponse::tipo)
                .containsExactly(TipoInteracao.VISITA, TipoInteracao.LIGACAO);
    }

    @Test
    @DisplayName("Listar interações de cliente que não existe responde não encontrado")
    void listarDoCliente_clienteInexistente_lancaRecursoNaoEncontrado() {
        when(clienteRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interacaoService.listarDoCliente(9L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
