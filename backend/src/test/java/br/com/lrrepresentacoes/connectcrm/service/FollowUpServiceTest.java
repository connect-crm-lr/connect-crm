package br.com.lrrepresentacoes.connectcrm.service;

import br.com.lrrepresentacoes.connectcrm.dto.FollowUpRequest;
import br.com.lrrepresentacoes.connectcrm.dto.FollowUpResponse;
import br.com.lrrepresentacoes.connectcrm.exception.RecursoNaoEncontradoException;
import br.com.lrrepresentacoes.connectcrm.exception.RegraNegocioException;
import br.com.lrrepresentacoes.connectcrm.model.Cliente;
import br.com.lrrepresentacoes.connectcrm.model.FollowUp;
import br.com.lrrepresentacoes.connectcrm.model.Usuario;
import br.com.lrrepresentacoes.connectcrm.repository.ClienteRepository;
import br.com.lrrepresentacoes.connectcrm.repository.FollowUpRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FollowUpServiceTest {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    @Mock
    private FollowUpRepository followUpRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private UsuarioLogadoService usuarioLogadoService;

    @InjectMocks
    private FollowUpService followUpService;

    private static final Usuario REVENDEDOR = new Usuario("Revendedor", "revendedor@exemplo.com", "hash");

    private static Cliente cliente(Long id) {
        Cliente cliente = new Cliente(REVENDEDOR, "Ateliê Bella Noiva", "5562999998888", null, null, "Goiânia", "GO");
        ReflectionTestUtils.setField(cliente, "id", id);
        return cliente;
    }

    private static FollowUp followUp(Cliente cliente, LocalDate dataPrevista) {
        return new FollowUp(cliente, REVENDEDOR, dataPrevista, "Retornar sobre o mostruário");
    }

    @Test
    @DisplayName("CT-RF012-01: criar follow-up grava data prevista e descrição, com data_conclusao vazia")
    void criar_nascePendenteComOUsuarioLogado() {
        Cliente atelie = cliente(1L);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(atelie));
        when(usuarioLogadoService.obter()).thenReturn(REVENDEDOR);
        when(followUpRepository.save(any(FollowUp.class))).thenAnswer(inv -> inv.getArgument(0));

        FollowUpResponse resposta = followUpService.criar(1L,
                new FollowUpRequest(LocalDate.of(2026, 11, 20), "  Retornar sobre o mostruário "));

        ArgumentCaptor<FollowUp> captor = ArgumentCaptor.forClass(FollowUp.class);
        verify(followUpRepository).save(captor.capture());
        FollowUp gravado = captor.getValue();
        assertThat(gravado.getCliente()).isSameAs(atelie);
        assertThat(gravado.getUsuario()).isSameAs(REVENDEDOR);
        assertThat(gravado.getDataPrevista()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(gravado.getDescricao()).isEqualTo("Retornar sobre o mostruário");
        assertThat(gravado.getDataConclusao()).isNull();
        assertThat(resposta.dataConclusao()).isNull();
        assertThat(resposta.cliente()).isEqualTo("Ateliê Bella Noiva");
    }

    @Test
    @DisplayName("Criar follow-up para cliente que não existe responde não encontrado e não grava")
    void criar_clienteInexistente_lancaRecursoNaoEncontrado() {
        when(clienteRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> followUpService.criar(9L,
                new FollowUpRequest(LocalDate.of(2026, 11, 20), "Retornar")))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Cliente não encontrado");
        verify(followUpRepository, never()).save(any());
    }

    @Test
    @DisplayName("CT-RF012-03: concluir follow-up preenche data_conclusao com a data/hora atual")
    void concluir_preencheADataDeConclusaoComAgora() {
        FollowUp pendente = followUp(cliente(1L), LocalDate.of(2026, 11, 10));
        when(followUpRepository.findById(5L)).thenReturn(Optional.of(pendente));

        LocalDateTime antes = LocalDateTime.now(FUSO);
        FollowUpResponse resposta = followUpService.concluir(5L);
        LocalDateTime depois = LocalDateTime.now(FUSO);

        assertThat(pendente.getDataConclusao()).isBetween(antes, depois);
        assertThat(resposta.dataConclusao()).isEqualTo(pendente.getDataConclusao());
    }

    // Decisão: concluir de novo não troca a data da primeira conclusão.
    @Test
    @DisplayName("Concluir um follow-up já concluído é recusado e mantém a data da primeira conclusão")
    void concluir_jaConcluido_recusaSemTrocarAData() {
        FollowUp concluido = followUp(cliente(1L), LocalDate.of(2026, 11, 10));
        LocalDateTime primeiraConclusao = LocalDateTime.of(2026, 11, 10, 9, 30);
        concluido.concluir(primeiraConclusao);
        when(followUpRepository.findById(5L)).thenReturn(Optional.of(concluido));

        assertThatThrownBy(() -> followUpService.concluir(5L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("O follow-up já foi concluído");
        assertThat(concluido.getDataConclusao()).isEqualTo(primeiraConclusao);
    }

    @Test
    @DisplayName("Concluir follow-up que não existe responde não encontrado")
    void concluir_inexistente_lancaRecursoNaoEncontrado() {
        when(followUpRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> followUpService.concluir(9L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Follow-up não encontrado");
    }

    // As condições (não concluído, até hoje, cliente ativo) ficam na consulta e são
    // conferidas no banco pelo FollowUpIntegrationTest; aqui se confere o "hoje" enviado.
    @Test
    @DisplayName("CT-RD011-01: as pendências de hoje são consultadas com a data de hoje no fuso de São Paulo")
    void listarPendenciasDeHoje_consultaComADataDeHoje() {
        Cliente atelie = cliente(1L);
        when(followUpRepository.listarPendencias(any(LocalDate.class)))
                .thenReturn(List.of(followUp(atelie, LocalDate.of(2026, 11, 10))));

        LocalDate antes = LocalDate.now(FUSO);
        List<FollowUpResponse> resposta = followUpService.listarPendenciasDeHoje();
        LocalDate depois = LocalDate.now(FUSO);

        ArgumentCaptor<LocalDate> captor = ArgumentCaptor.forClass(LocalDate.class);
        verify(followUpRepository).listarPendencias(captor.capture());
        assertThat(captor.getValue()).isBetween(antes, depois);
        assertThat(resposta).extracting(FollowUpResponse::cliente).containsExactly("Ateliê Bella Noiva");
    }

    @Test
    @DisplayName("Listar devolve os follow-ups do cliente, pendentes e concluídos")
    void listarDoCliente_devolveOsFollowUpsDoCliente() {
        Cliente atelie = cliente(1L);
        FollowUp concluido = followUp(atelie, LocalDate.of(2026, 11, 1));
        concluido.concluir(LocalDateTime.of(2026, 11, 1, 15, 0));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(atelie));
        when(followUpRepository.findByClienteIdOrderByDataPrevistaAscIdAsc(1L))
                .thenReturn(List.of(concluido, followUp(atelie, LocalDate.of(2026, 11, 20))));

        List<FollowUpResponse> resposta = followUpService.listarDoCliente(1L);

        assertThat(resposta).hasSize(2);
        assertThat(resposta.get(0).dataConclusao()).isNotNull();
        assertThat(resposta.get(1).dataConclusao()).isNull();
    }
}
