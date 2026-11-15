package br.com.lrrepresentacoes.connectcrm.service;

import br.com.lrrepresentacoes.connectcrm.dto.NegociacaoResponse;
import br.com.lrrepresentacoes.connectcrm.model.Cliente;
import br.com.lrrepresentacoes.connectcrm.model.EstagioNegociacao;
import br.com.lrrepresentacoes.connectcrm.model.Negociacao;
import br.com.lrrepresentacoes.connectcrm.model.Usuario;
import br.com.lrrepresentacoes.connectcrm.repository.NegociacaoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Consulta do kanban (#71): listar as negociações com filtro opcional por cliente. */
@ExtendWith(MockitoExtension.class)
class NegociacaoListagemServiceTest {

    @Mock
    private NegociacaoRepository negociacaoRepository;

    @InjectMocks
    private NegociacaoService negociacaoService;

    private static final Usuario REVENDEDOR = new Usuario("Revendedor", "revendedor@exemplo.com", "hash");

    private static Negociacao negociacao(Long id, Long idCliente, String nomeCliente, EstagioNegociacao estagio) {
        Cliente cliente = new Cliente(REVENDEDOR, nomeCliente, "5562999998888", null, null, "Goiânia", "GO");
        ReflectionTestUtils.setField(cliente, "id", idCliente);
        Negociacao negociacao = new Negociacao(cliente, REVENDEDOR, "Coleção de verão",
                LocalDateTime.of(2026, 10, 1, 9, 0));
        ReflectionTestUtils.setField(negociacao, "id", id);
        ReflectionTestUtils.setField(negociacao, "estagio", estagio);
        return negociacao;
    }

    @Test
    @DisplayName("listar sem filtro passa null ao repository e devolve todas as etapas")
    void listar_semFiltro_devolveTodasAsEtapas() {
        when(negociacaoRepository.listar(null)).thenReturn(List.of(
                negociacao(3L, 1L, "Ateliê Bella Noiva", EstagioNegociacao.FECHADA),
                negociacao(2L, 2L, "Confecção Lima", EstagioNegociacao.PRIMEIRO_CONTATO),
                negociacao(1L, 1L, "Ateliê Bella Noiva", EstagioNegociacao.PERDIDA)));

        List<NegociacaoResponse> resultado = negociacaoService.listar(null);

        assertThat(resultado).extracting(NegociacaoResponse::estagio).containsExactly(
                EstagioNegociacao.FECHADA, EstagioNegociacao.PRIMEIRO_CONTATO, EstagioNegociacao.PERDIDA);
    }

    @Test
    @DisplayName("listar com clienteId repassa o filtro ao repository")
    void listar_comCliente_repassaOFiltro() {
        when(negociacaoRepository.listar(1L)).thenReturn(List.of(
                negociacao(1L, 1L, "Ateliê Bella Noiva", EstagioNegociacao.MOSTRUARIO_ENVIADO)));

        List<NegociacaoResponse> resultado = negociacaoService.listar(1L);

        assertThat(resultado).hasSize(1);
        verify(negociacaoRepository).listar(1L);
    }

    @Test
    @DisplayName("o card traz id e nome do cliente, descrição e etapa")
    void listar_cardTrazOsDadosDoKanban() {
        when(negociacaoRepository.listar(null)).thenReturn(List.of(
                negociacao(5L, 1L, "Ateliê Bella Noiva", EstagioNegociacao.AGUARDANDO_RESPOSTA)));

        NegociacaoResponse card = negociacaoService.listar(null).get(0);

        assertThat(card.id()).isEqualTo(5L);
        assertThat(card.idCliente()).isEqualTo(1L);
        assertThat(card.cliente()).isEqualTo("Ateliê Bella Noiva");
        assertThat(card.descricao()).isEqualTo("Coleção de verão");
        assertThat(card.estagio()).isEqualTo(EstagioNegociacao.AGUARDANDO_RESPOSTA);
    }

    @Test
    @DisplayName("listar sem negociações devolve lista vazia")
    void listar_semNegociacoes_devolveVazio() {
        when(negociacaoRepository.listar(9L)).thenReturn(List.of());

        assertThat(negociacaoService.listar(9L)).isEmpty();
    }
}
