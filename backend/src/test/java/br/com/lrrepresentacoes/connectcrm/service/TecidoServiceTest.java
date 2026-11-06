package br.com.lrrepresentacoes.connectcrm.service;

import br.com.lrrepresentacoes.connectcrm.dto.TecidoRequest;
import br.com.lrrepresentacoes.connectcrm.dto.TecidoResponse;
import br.com.lrrepresentacoes.connectcrm.exception.RecursoNaoEncontradoException;
import br.com.lrrepresentacoes.connectcrm.exception.RegraNegocioException;
import br.com.lrrepresentacoes.connectcrm.model.Tecido;
import br.com.lrrepresentacoes.connectcrm.repository.TecidoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TecidoServiceTest {

    @Mock
    private TecidoRepository tecidoRepository;

    @InjectMocks
    private TecidoService tecidoService;

    private static Tecido linho() {
        return new Tecido("Linho Natural", "100% Linho", 185, new BigDecimal("1.40"),
                new BigDecimal("42.50"), null);
    }

    // CT-RF014-01 / CT-RF023-01: a listagem padrão mostra só os ativos.
    @Test
    void listar_semBuscaESemInativos_usaAListagemDosAtivos() {
        when(tecidoRepository.findByAtivoTrueOrderByNomeAsc()).thenReturn(List.of(linho()));

        List<TecidoResponse> resposta = tecidoService.listar(null, false);

        assertThat(resposta).extracting(TecidoResponse::nome).containsExactly("Linho Natural");
        verify(tecidoRepository, never()).findAllByOrderByNomeAsc();
    }

    @Test
    void listar_comInativos_usaAListagemCompleta() {
        when(tecidoRepository.findAllByOrderByNomeAsc()).thenReturn(List.of(linho()));

        tecidoService.listar("   ", true);

        verify(tecidoRepository).findAllByOrderByNomeAsc();
        verify(tecidoRepository, never()).buscar(anyString(), anyBoolean());
    }

    // CT-RF015-01: a busca vai para o repository em minúsculas e entre '%'.
    @Test
    void listar_comBusca_montaOPadraoEmMinusculasSemEspacos() {
        when(tecidoRepository.buscar("%linho%", false)).thenReturn(List.of(linho()));

        List<TecidoResponse> resposta = tecidoService.listar("  LINHO ", false);

        assertThat(resposta).hasSize(1);
        verify(tecidoRepository).buscar("%linho%", false);
    }

    // CT-RF013-01: só nome e preço obrigatórios; opcionais em branco viram null.
    @Test
    void criar_comCamposOpcionaisEmBranco_gravaNullERemoveEspacosDoNome() {
        when(tecidoRepository.save(any(Tecido.class))).thenAnswer(inv -> inv.getArgument(0));
        TecidoRequest request = new TecidoRequest("  Viscose  ", " ", null, null,
                new BigDecimal("29.90"), "");

        TecidoResponse resposta = tecidoService.criar(request);

        ArgumentCaptor<Tecido> captor = ArgumentCaptor.forClass(Tecido.class);
        verify(tecidoRepository).save(captor.capture());
        Tecido salvo = captor.getValue();
        assertThat(salvo.getNome()).isEqualTo("Viscose");
        assertThat(salvo.getComposicao()).isNull();
        assertThat(salvo.getFotoUrl()).isNull();
        assertThat(salvo.getPrecoMetro()).isEqualByComparingTo("29.90");
        assertThat(resposta.ativo()).isTrue();
    }

    // CT-RF016-01: alterar grava os novos dados no tecido existente.
    @Test
    void atualizar_quandoTecidoExiste_alteraOsDados() {
        Tecido existente = linho();
        when(tecidoRepository.findById(1L)).thenReturn(Optional.of(existente));
        TecidoRequest request = new TecidoRequest("Linho Natural", "100% Linho", 185,
                new BigDecimal("1.40"), new BigDecimal("45.00"), null);

        TecidoResponse resposta = tecidoService.atualizar(1L, request);

        assertThat(resposta.precoMetro()).isEqualByComparingTo("45.00");
        assertThat(existente.getPrecoMetro()).isEqualByComparingTo("45.00");
    }

    @Test
    void atualizar_quandoTecidoNaoExiste_lancaRecursoNaoEncontrado() {
        when(tecidoRepository.findById(99L)).thenReturn(Optional.empty());
        TecidoRequest request = new TecidoRequest("Seda", null, null, null, BigDecimal.TEN, null);

        assertThatThrownBy(() -> tecidoService.atualizar(99L, request))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Tecido não encontrado");
    }

    // CT-RF023-02: "Excluir" desativa, nunca apaga.
    @Test
    void desativar_tecidoAtivo_marcaComoInativoSemApagar() {
        Tecido existente = linho();
        when(tecidoRepository.findById(1L)).thenReturn(Optional.of(existente));

        TecidoResponse resposta = tecidoService.desativar(1L);

        assertThat(resposta.ativo()).isFalse();
        assertThat(existente.isAtivo()).isFalse();
        verify(tecidoRepository, never()).delete(any(Tecido.class));
    }

    @Test
    void desativar_tecidoJaDesativado_lancaRegraNegocio() {
        Tecido existente = linho();
        existente.desativar();
        when(tecidoRepository.findById(1L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> tecidoService.desativar(1L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("O tecido já está desativado");
    }

    // CT-RF023-01: reativar volta o tecido para o catálogo.
    @Test
    void reativar_tecidoDesativado_marcaComoAtivo() {
        Tecido existente = linho();
        existente.desativar();
        when(tecidoRepository.findById(1L)).thenReturn(Optional.of(existente));

        TecidoResponse resposta = tecidoService.reativar(1L);

        assertThat(resposta.ativo()).isTrue();
    }

    @Test
    void reativar_tecidoJaAtivo_lancaRegraNegocio() {
        when(tecidoRepository.findById(1L)).thenReturn(Optional.of(linho()));

        assertThatThrownBy(() -> tecidoService.reativar(1L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("O tecido já está ativo");
    }
}
