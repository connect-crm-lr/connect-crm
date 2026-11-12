package br.com.lrrepresentacoes.connectcrm.service;

import br.com.lrrepresentacoes.connectcrm.dto.MoverEtapaRequest;
import br.com.lrrepresentacoes.connectcrm.dto.NegociacaoRequest;
import br.com.lrrepresentacoes.connectcrm.dto.NegociacaoResponse;
import br.com.lrrepresentacoes.connectcrm.exception.RecursoNaoEncontradoException;
import br.com.lrrepresentacoes.connectcrm.exception.RegraNegocioException;
import br.com.lrrepresentacoes.connectcrm.model.Cliente;
import br.com.lrrepresentacoes.connectcrm.model.EstagioNegociacao;
import br.com.lrrepresentacoes.connectcrm.model.Negociacao;
import br.com.lrrepresentacoes.connectcrm.model.Usuario;
import br.com.lrrepresentacoes.connectcrm.repository.ClienteRepository;
import br.com.lrrepresentacoes.connectcrm.repository.NegociacaoRepository;
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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NegociacaoServiceTest {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    // Data antiga, para o teste enxergar que a movimentação trocou a data.
    private static final LocalDateTime CRIACAO = LocalDateTime.of(2026, 10, 1, 9, 0);

    @Mock
    private NegociacaoRepository negociacaoRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private UsuarioLogadoService usuarioLogadoService;

    @InjectMocks
    private NegociacaoService negociacaoService;

    private static final Usuario REVENDEDOR = new Usuario("Revendedor", "revendedor@exemplo.com", "hash");

    private static Cliente cliente(Long id) {
        Cliente cliente = new Cliente(REVENDEDOR, "Ateliê Bella Noiva", "5562999998888", null, null, "Goiânia", "GO");
        ReflectionTestUtils.setField(cliente, "id", id);
        return cliente;
    }

    // Negociação criada em CRIACAO e já movida (também em CRIACAO) para o estágio pedido.
    private Negociacao negociacaoEm(EstagioNegociacao estagio, Cliente cliente) {
        Negociacao negociacao = new Negociacao(cliente, REVENDEDOR, "Mostruário de linho", CRIACAO);
        if (estagio != EstagioNegociacao.PRIMEIRO_CONTATO) {
            negociacao.moverPara(estagio, CRIACAO);
        }
        when(negociacaoRepository.findById(3L)).thenReturn(Optional.of(negociacao));
        return negociacao;
    }

    // ---------- criar ----------

    @Test
    @DisplayName("CT-RF007-01: criar negociação para cliente ativo nasce em PRIMEIRO_CONTATO, com o usuário logado")
    void criar_clienteAtivo_nasceEmPrimeiroContato() {
        Cliente atelie = cliente(1L);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(atelie));
        when(usuarioLogadoService.obter()).thenReturn(REVENDEDOR);
        when(negociacaoRepository.save(any(Negociacao.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime antes = LocalDateTime.now(FUSO);
        NegociacaoResponse resposta = negociacaoService.criar(new NegociacaoRequest(1L, "  Mostruário de linho "));
        LocalDateTime depois = LocalDateTime.now(FUSO);

        ArgumentCaptor<Negociacao> captor = ArgumentCaptor.forClass(Negociacao.class);
        verify(negociacaoRepository).save(captor.capture());
        Negociacao gravada = captor.getValue();
        assertThat(gravada.getCliente()).isSameAs(atelie);
        assertThat(gravada.getUsuario()).isSameAs(REVENDEDOR);
        assertThat(gravada.getDescricao()).isEqualTo("Mostruário de linho");
        assertThat(gravada.getEstagio()).isEqualTo(EstagioNegociacao.PRIMEIRO_CONTATO);
        assertThat(gravada.getDataCriacao()).isBetween(antes, depois);
        assertThat(gravada.getDataUltimaMovimentacao()).isEqualTo(gravada.getDataCriacao());
        assertThat(gravada.getDataFechamento()).isNull();
        assertThat(resposta.estagio()).isEqualTo(EstagioNegociacao.PRIMEIRO_CONTATO);
        assertThat(resposta.cliente()).isEqualTo("Ateliê Bella Noiva");
    }

    @Test
    @DisplayName("CT-RD014-01: criar negociação para cliente desativado é recusado e nada é gravado")
    void criar_clienteDesativado_recusaSemGravar() {
        Cliente desativado = cliente(1L);
        desativado.desativar();
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(desativado));

        assertThatThrownBy(() -> negociacaoService.criar(new NegociacaoRequest(1L, "Mostruário de linho")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("O cliente Ateliê Bella Noiva está desativado. "
                        + "Reative o cliente antes de criar a negociação.");
        verify(negociacaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Criar negociação para cliente que não existe responde não encontrado")
    void criar_clienteInexistente_lancaRecursoNaoEncontrado() {
        when(clienteRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> negociacaoService.criar(new NegociacaoRequest(9L, "Mostruário")))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Cliente não encontrado");
        verify(negociacaoRepository, never()).save(any());
    }

    // ---------- mover etapa (RD-010) ----------

    @Test
    @DisplayName("CT-RD010-01: mover entre etapas abertas atualiza data_ultima_movimentacao e mantém data_fechamento vazia")
    void moverEtapa_entreEtapasAbertas_atualizaSoAMovimentacao() {
        Negociacao negociacao = negociacaoEm(EstagioNegociacao.PRIMEIRO_CONTATO, cliente(1L));

        LocalDateTime antes = LocalDateTime.now(FUSO);
        NegociacaoResponse resposta = negociacaoService.moverEtapa(3L, new MoverEtapaRequest("MOSTRUARIO_ENVIADO"));
        LocalDateTime depois = LocalDateTime.now(FUSO);

        assertThat(negociacao.getEstagio()).isEqualTo(EstagioNegociacao.MOSTRUARIO_ENVIADO);
        assertThat(negociacao.getDataUltimaMovimentacao()).isBetween(antes, depois);
        assertThat(negociacao.getDataFechamento()).isNull();
        assertThat(negociacao.getDataCriacao()).isEqualTo(CRIACAO);
        assertThat(resposta.estagio()).isEqualTo(EstagioNegociacao.MOSTRUARIO_ENVIADO);
    }

    @ParameterizedTest
    @EnumSource(value = EstagioNegociacao.class, names = {"FECHADA", "PERDIDA"})
    @DisplayName("CT-RD010-02: mover para FECHADA ou PERDIDA preenche data_fechamento")
    void moverEtapa_paraFechadaOuPerdida_preencheODataFechamento(EstagioNegociacao destino) {
        Negociacao negociacao = negociacaoEm(EstagioNegociacao.AGUARDANDO_RESPOSTA, cliente(1L));

        LocalDateTime antes = LocalDateTime.now(FUSO);
        negociacaoService.moverEtapa(3L, new MoverEtapaRequest(destino.name()));
        LocalDateTime depois = LocalDateTime.now(FUSO);

        assertThat(negociacao.getEstagio()).isEqualTo(destino);
        assertThat(negociacao.getDataFechamento()).isBetween(antes, depois);
        assertThat(negociacao.getDataUltimaMovimentacao()).isEqualTo(negociacao.getDataFechamento());
    }

    @Test
    @DisplayName("CT-RD010-03: tirar de FECHADA volta data_fechamento para null")
    void moverEtapa_saindoDeFechada_limpaODataFechamento() {
        Negociacao negociacao = negociacaoEm(EstagioNegociacao.FECHADA, cliente(1L));
        assertThat(negociacao.getDataFechamento()).isEqualTo(CRIACAO);

        negociacaoService.moverEtapa(3L, new MoverEtapaRequest("AGUARDANDO_RESPOSTA"));

        assertThat(negociacao.getEstagio()).isEqualTo(EstagioNegociacao.AGUARDANDO_RESPOSTA);
        assertThat(negociacao.getDataFechamento()).isNull();
        assertThat(negociacao.getDataUltimaMovimentacao()).isAfter(CRIACAO);
    }

    @Test
    @DisplayName("CT-RD010-03: tirar de PERDIDA também volta data_fechamento para null")
    void moverEtapa_saindoDePerdida_limpaODataFechamento() {
        Negociacao negociacao = negociacaoEm(EstagioNegociacao.PERDIDA, cliente(1L));

        negociacaoService.moverEtapa(3L, new MoverEtapaRequest("PRIMEIRO_CONTATO"));

        assertThat(negociacao.getDataFechamento()).isNull();
    }

    // De FECHADA para PERDIDA o card entra numa etapa de encerramento: vale a data da nova entrada.
    @Test
    @DisplayName("CT-RD010-02: de FECHADA para PERDIDA a data_fechamento passa a ser a da nova etapa")
    void moverEtapa_deFechadaParaPerdida_atualizaODataFechamento() {
        Negociacao negociacao = negociacaoEm(EstagioNegociacao.FECHADA, cliente(1L));

        negociacaoService.moverEtapa(3L, new MoverEtapaRequest("PERDIDA"));

        assertThat(negociacao.getEstagio()).isEqualTo(EstagioNegociacao.PERDIDA);
        assertThat(negociacao.getDataFechamento()).isAfter(CRIACAO);
    }

    // Decisão: soltar o card na mesma coluna não é mudança de etapa, então nenhuma data muda.
    @Test
    @DisplayName("Mover para a etapa em que a negociação já está é recusado e as datas não mudam")
    void moverEtapa_paraAMesmaEtapa_recusaSemMexerNasDatas() {
        Negociacao negociacao = negociacaoEm(EstagioNegociacao.MOSTRUARIO_ENVIADO, cliente(1L));

        assertThatThrownBy(() -> negociacaoService.moverEtapa(3L, new MoverEtapaRequest("MOSTRUARIO_ENVIADO")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("A negociação já está na etapa MOSTRUARIO_ENVIADO");
        assertThat(negociacao.getDataUltimaMovimentacao()).isEqualTo(CRIACAO);
    }

    // Regra 6.5: cliente desativado não tem negociação em andamento.
    @Test
    @DisplayName("CT-RD014-01: reabrir a negociação encerrada de cliente desativado é recusado")
    void moverEtapa_reabrirDeClienteDesativado_recusa() {
        Cliente desativado = cliente(1L);
        Negociacao negociacao = negociacaoEm(EstagioNegociacao.PERDIDA, desativado);
        desativado.desativar();

        assertThatThrownBy(() -> negociacaoService.moverEtapa(3L, new MoverEtapaRequest("PRIMEIRO_CONTATO")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("O cliente Ateliê Bella Noiva está desativado. "
                        + "Reative o cliente antes de reabrir a negociação.");
        assertThat(negociacao.getEstagio()).isEqualTo(EstagioNegociacao.PERDIDA);
        assertThat(negociacao.getDataFechamento()).isEqualTo(CRIACAO);
    }

    // Trocar PERDIDA por FECHADA não reabre nada, então continua permitido com o cliente desativado.
    @Test
    @DisplayName("Trocar PERDIDA por FECHADA é permitido mesmo com o cliente desativado")
    void moverEtapa_entreEtapasEncerradasDeClienteDesativado_permite() {
        Cliente desativado = cliente(1L);
        Negociacao negociacao = negociacaoEm(EstagioNegociacao.PERDIDA, desativado);
        desativado.desativar();

        negociacaoService.moverEtapa(3L, new MoverEtapaRequest("FECHADA"));

        assertThat(negociacao.getEstagio()).isEqualTo(EstagioNegociacao.FECHADA);
    }

    @Test
    @DisplayName("Mover negociação que não existe responde não encontrado")
    void moverEtapa_inexistente_lancaRecursoNaoEncontrado() {
        when(negociacaoRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> negociacaoService.moverEtapa(9L, new MoverEtapaRequest("FECHADA")))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Negociação não encontrada");
    }

    // ---------- enum ----------

    // Critério de aceitação: as etapas ficam na ordem do enum, que é a ordem das colunas do funil.
    @Test
    @DisplayName("CT-RF007-01: as etapas do funil estão na ordem do enum EstagioNegociacao")
    void estagios_estaoNaOrdemDoFunil() {
        assertThat(EstagioNegociacao.values()).containsExactly(
                EstagioNegociacao.PRIMEIRO_CONTATO,
                EstagioNegociacao.MOSTRUARIO_ENVIADO,
                EstagioNegociacao.AGUARDANDO_RESPOSTA,
                EstagioNegociacao.FECHADA,
                EstagioNegociacao.PERDIDA);
        assertThat(EstagioNegociacao.FECHADA.isEncerrada()).isTrue();
        assertThat(EstagioNegociacao.PERDIDA.isEncerrada()).isTrue();
        assertThat(EstagioNegociacao.AGUARDANDO_RESPOSTA.isEncerrada()).isFalse();
    }
}
