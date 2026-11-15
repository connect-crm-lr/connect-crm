package br.com.lrrepresentacoes.connectcrm.repository;

import br.com.lrrepresentacoes.connectcrm.model.Cliente;
import br.com.lrrepresentacoes.connectcrm.model.Estoque;
import br.com.lrrepresentacoes.connectcrm.model.FormaPagamento;
import br.com.lrrepresentacoes.connectcrm.model.Negociacao;
import br.com.lrrepresentacoes.connectcrm.model.Pedido;
import br.com.lrrepresentacoes.connectcrm.model.StatusPedido;
import br.com.lrrepresentacoes.connectcrm.model.Tecido;
import br.com.lrrepresentacoes.connectcrm.model.Usuario;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integração com o PostgreSQL real: confere as consultas de listagem de pedidos e negociações (#71).
 * Cada teste roda numa transação desfeita no fim (@Transactional). Os nomes usam o prefixo "IT ".
 */
@SpringBootTest
@Transactional
class ListagensIntegrationTest {

    // Usuário inicial criado pela V2.
    private static final String EMAIL_V2 = "revendedor@lrrepresentacoes.com.br";

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private NegociacaoRepository negociacaoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TecidoRepository tecidoRepository;

    @Autowired
    private EstoqueRepository estoqueRepository;

    @Autowired
    private EntityManager entityManager;

    private Usuario usuario;
    private Cliente bella;
    private Cliente lima;
    private Estoque linhaLinho;

    @BeforeEach
    void criarDados() {
        usuario = usuarioRepository.findByEmailIgnoreCase(EMAIL_V2).orElseThrow();
        bella = clienteRepository.save(new Cliente(usuario, "IT Ateliê Bella", "5562999998888",
                null, null, "Goiânia", "GO"));
        lima = clienteRepository.save(new Cliente(usuario, "IT Confecção Lima", "5562988887777",
                null, null, "Goiânia", "GO"));
        Tecido linho = tecidoRepository.save(new Tecido("IT Linho Natural", null, null, null,
                new BigDecimal("42.50"), null));
        linhaLinho = new Estoque(linho, "Natural");
        linhaLinho.somar(new BigDecimal("100.00"));
        linhaLinho = estoqueRepository.save(linhaLinho);
    }

    private Pedido pedido(Cliente cliente, boolean pagoNoAto, LocalDateTime data) {
        Pedido pedido = new Pedido(cliente, usuario, null, FormaPagamento.BOLETO, pagoNoAto, data);
        pedido.adicionarItem(linhaLinho, new BigDecimal("10.00"), new BigDecimal("42.50"));
        return pedidoRepository.save(pedido);
    }

    // Grava e limpa o cache para a consulta recarregar tudo do banco, como numa requisição nova.
    private void gravarELimpar() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("pedidos vêm do mais novo para o mais antigo, com os itens")
    void listarPedidos_maisNovosPrimeiroComItens() {
        Pedido antigo = pedido(bella, false, LocalDateTime.of(2026, 11, 1, 9, 0));
        Pedido novo = pedido(lima, false, LocalDateTime.of(2026, 11, 3, 9, 0));
        gravarELimpar();

        List<Pedido> resultado = pedidoRepository.listar(null, null, null, null).stream()
                .filter(p -> p.getId().equals(antigo.getId()) || p.getId().equals(novo.getId()))
                .toList();

        assertThat(resultado).extracting(Pedido::getId).containsExactly(novo.getId(), antigo.getId());
        assertThat(resultado.get(0).getItens()).hasSize(1);
    }

    @Test
    @DisplayName("filtro por cliente devolve só os pedidos dele")
    void listarPedidos_filtraPorCliente() {
        Pedido daBella = pedido(bella, false, LocalDateTime.of(2026, 11, 1, 9, 0));
        pedido(lima, false, LocalDateTime.of(2026, 11, 2, 9, 0));
        gravarELimpar();

        List<Pedido> resultado = pedidoRepository.listar(bella.getId(), null, null, null);

        assertThat(resultado).extracting(Pedido::getId).containsExactly(daBella.getId());
    }

    @Test
    @DisplayName("filtro por status separa EM_ABERTO, PAGO e CANCELADO; cancelado aparece com o status")
    void listarPedidos_filtraPorStatusECanceladoAparece() {
        Pedido emAberto = pedido(bella, false, LocalDateTime.of(2026, 11, 1, 9, 0));
        Pedido pago = pedido(bella, true, LocalDateTime.of(2026, 11, 2, 9, 0));
        Pedido cancelado = pedido(bella, false, LocalDateTime.of(2026, 11, 3, 9, 0));
        cancelado.cancelar();
        gravarELimpar();

        assertThat(pedidoRepository.listar(bella.getId(), StatusPedido.EM_ABERTO, null, null))
                .extracting(Pedido::getId).containsExactly(emAberto.getId());
        assertThat(pedidoRepository.listar(bella.getId(), StatusPedido.PAGO, null, null))
                .extracting(Pedido::getId).containsExactly(pago.getId());
        assertThat(pedidoRepository.listar(bella.getId(), StatusPedido.CANCELADO, null, null))
                .extracting(Pedido::getId).containsExactly(cancelado.getId());
        assertThat(pedidoRepository.listar(bella.getId(), null, null, null))
                .extracting(Pedido::getId).containsExactly(cancelado.getId(), pago.getId(), emAberto.getId());
    }

    @Test
    @DisplayName("período: início inclusivo, fim inclusivo (dia inteiro) e pedido fora do intervalo não entra")
    void listarPedidos_filtraPorPeriodo() {
        Pedido antes = pedido(bella, false, LocalDateTime.of(2026, 10, 31, 23, 59));
        Pedido noPrimeiroDia = pedido(bella, false, LocalDateTime.of(2026, 11, 1, 0, 0));
        Pedido noUltimoDia = pedido(bella, false, LocalDateTime.of(2026, 11, 30, 23, 59));
        Pedido depois = pedido(bella, false, LocalDateTime.of(2026, 12, 1, 0, 0));
        gravarELimpar();

        List<Pedido> resultado = pedidoRepository.listar(bella.getId(), null,
                LocalDateTime.of(2026, 11, 1, 0, 0), LocalDateTime.of(2026, 12, 1, 0, 0));

        assertThat(resultado).extracting(Pedido::getId)
                .containsExactly(noUltimoDia.getId(), noPrimeiroDia.getId())
                .doesNotContain(antes.getId(), depois.getId());
    }

    @Test
    @DisplayName("total do período soma só os não cancelados e respeita o filtro de status")
    void total_somaSemCanceladosNoPeriodo() {
        pedido(bella, false, LocalDateTime.of(2026, 11, 5, 9, 0));   // EM_ABERTO, 425,00
        pedido(bella, true, LocalDateTime.of(2026, 11, 6, 9, 0));    // PAGO, 425,00
        pedido(bella, false, LocalDateTime.of(2026, 11, 7, 9, 0)).cancelar(); // CANCELADO fora do total
        pedido(bella, false, LocalDateTime.of(2026, 12, 2, 9, 0));   // fora do período
        gravarELimpar();
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 1, 0, 0);
        LocalDateTime fim = LocalDateTime.of(2026, 12, 1, 0, 0);

        // Filtra pelo período inteiro de novembro: o banco local pode ter outros pedidos nele,
        // então compara com a diferença causada pelos três pedidos criados aqui.
        assertThat(pedidoRepository.somarTotalSemCancelados(StatusPedido.PAGO, inicio, fim))
                .isGreaterThanOrEqualTo(new BigDecimal("425.00"));
        assertThat(pedidoRepository.contarSemCancelados(null, inicio, fim)).isGreaterThanOrEqualTo(2);
        assertThat(pedidoRepository.contarSemCancelados(StatusPedido.CANCELADO, inicio, fim)).isZero();
    }

    @Test
    @DisplayName("negociações: todas as etapas, filtro por cliente e mais recentemente movidas primeiro")
    void listarNegociacoes_filtraPorClienteEOrdena() {
        Negociacao antiga = negociacaoRepository.save(new Negociacao(bella, usuario, "IT antiga",
                LocalDateTime.of(2026, 10, 1, 9, 0)));
        Negociacao recente = negociacaoRepository.save(new Negociacao(bella, usuario, "IT recente",
                LocalDateTime.of(2026, 10, 5, 9, 0)));
        Negociacao deLima = negociacaoRepository.save(new Negociacao(lima, usuario, "IT lima",
                LocalDateTime.of(2026, 10, 3, 9, 0)));
        gravarELimpar();

        assertThat(negociacaoRepository.listar(bella.getId()))
                .extracting(Negociacao::getId).containsExactly(recente.getId(), antiga.getId());
        assertThat(negociacaoRepository.listar(lima.getId()))
                .extracting(Negociacao::getId).containsExactly(deLima.getId());
        assertThat(negociacaoRepository.listar(null))
                .extracting(Negociacao::getId).contains(recente.getId(), antiga.getId(), deLima.getId());
    }
}
