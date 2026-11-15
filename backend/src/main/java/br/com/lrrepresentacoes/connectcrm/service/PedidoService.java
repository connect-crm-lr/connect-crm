package br.com.lrrepresentacoes.connectcrm.service;

import br.com.lrrepresentacoes.connectcrm.dto.ItemPedidoRequest;
import br.com.lrrepresentacoes.connectcrm.dto.PedidoRequest;
import br.com.lrrepresentacoes.connectcrm.dto.PedidoResponse;
import br.com.lrrepresentacoes.connectcrm.exception.RecursoNaoEncontradoException;
import br.com.lrrepresentacoes.connectcrm.exception.RegraNegocioException;
import br.com.lrrepresentacoes.connectcrm.model.Cliente;
import br.com.lrrepresentacoes.connectcrm.model.Estoque;
import br.com.lrrepresentacoes.connectcrm.model.FormaPagamento;
import br.com.lrrepresentacoes.connectcrm.model.ItemPedido;
import br.com.lrrepresentacoes.connectcrm.model.Pedido;
import br.com.lrrepresentacoes.connectcrm.model.StatusPedido;
import br.com.lrrepresentacoes.connectcrm.model.Tecido;
import br.com.lrrepresentacoes.connectcrm.repository.ClienteRepository;
import br.com.lrrepresentacoes.connectcrm.repository.EstoqueRepository;
import br.com.lrrepresentacoes.connectcrm.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Padrão Singleton: bean do Spring, uma única instância na aplicação.
@Service
public class PedidoService {

    // Mesmo fuso do banco e do Hibernate (AGENTS 4), independente do fuso da JVM.
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final PedidoRepository pedidoRepository;
    private final EstoqueRepository estoqueRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioLogadoService usuarioLogadoService;

    public PedidoService(PedidoRepository pedidoRepository, EstoqueRepository estoqueRepository,
                         ClienteRepository clienteRepository, UsuarioLogadoService usuarioLogadoService) {
        this.pedidoRepository = pedidoRepository;
        this.estoqueRepository = estoqueRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioLogadoService = usuarioLogadoService;
    }

    /**
     * Confirmar pedido (regra 6.1), tudo na mesma transação: ou o pedido, os itens e a baixa
     * do estoque são gravados juntos, ou nada é gravado.
     * 1) cliente e tecidos ativos; 2) pedido de negociação: o cliente é o da negociação;
     * 3) saldo de cada item; 4) grava pedido e itens; 5) desconta a metragem do estoque;
     * 6) calcula o total; 7) status EM_ABERTO, ou PAGO se pago no ato.
     *
     * Concorrência: as linhas de estoque do pedido são travadas (SELECT ... FOR UPDATE) antes
     * de o saldo ser lido. Um segundo pedido da mesma linha espera o primeiro terminar e só
     * então lê o saldo, já descontado; assim dois pedidos não vendem o mesmo metro.
     */
    // Padrão Decorator: @Transactional faz o Spring envolver o método num proxy que abre e fecha a transação.
    @Transactional
    public PedidoResponse confirmar(PedidoRequest request) {
        Cliente cliente = clienteRepository.findById(request.idCliente())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado"));
        if (!cliente.isAtivo()) {
            throw new RegraNegocioException("O cliente " + cliente.getNome()
                    + " está desativado. Reative o cliente antes de criar o pedido.");
        }
        verificarClienteDaNegociacao(request.idNegociacao(), cliente);

        Map<Long, BigDecimal> metragemPorEstoque = juntarItensRepetidos(request.itens());
        Map<Long, Estoque> linhas = travarLinhasDeEstoque(metragemPorEstoque);

        // Valida todos os itens antes de gravar qualquer coisa.
        // Padrão Iterator: for-each percorre os itens do pedido.
        for (Map.Entry<Long, BigDecimal> item : metragemPorEstoque.entrySet()) {
            Estoque linha = linhas.get(item.getKey());
            Tecido tecido = linha.getTecido();
            if (!tecido.isAtivo()) {
                throw new RegraNegocioException("O tecido " + tecido.getNome()
                        + " está desativado. Reative o tecido antes de vender.");
            }
            if (linha.getMetragem().compareTo(item.getValue()) < 0) {
                throw new RegraNegocioException("Estoque insuficiente: " + tecido.getNome() + " "
                        + linha.getCorCodigo() + " tem " + emMetros(linha.getMetragem()) + " m");
            }
        }

        FormaPagamento formaPagamento = FormaPagamento.valueOf(request.formaPagamento());
        Pedido pedido = new Pedido(cliente, usuarioLogadoService.obter(), request.idNegociacao(),
                formaPagamento, request.pagoNoAto(), LocalDateTime.now(FUSO));
        for (Map.Entry<Long, BigDecimal> item : metragemPorEstoque.entrySet()) {
            Estoque linha = linhas.get(item.getKey());
            // O item guarda o preço do tecido de agora; o total é somado pelo próprio pedido.
            pedido.adicionarItem(linha, item.getValue(), linha.getTecido().getPrecoMetro());
            linha.baixar(item.getValue());
        }
        return PedidoResponse.de(pedidoRepository.save(pedido));
    }

    /**
     * Marcar como pago: só o pedido EM_ABERTO muda para PAGO. O cancelamento é definitivo,
     * então o pedido cancelado não volta a ser pago.
     */
    @Transactional
    public PedidoResponse marcarComoPago(Long id) {
        Pedido pedido = buscarParaAtualizar(id);
        if (pedido.getStatus() == StatusPedido.PAGO) {
            throw new RegraNegocioException("O pedido já está pago");
        }
        if (pedido.getStatus() == StatusPedido.CANCELADO) {
            throw new RegraNegocioException("O pedido está cancelado e não pode ser marcado como pago");
        }
        pedido.marcarComoPago();
        return PedidoResponse.de(pedido);
    }

    /**
     * Cancelar pedido (regra 6.1), na mesma transação: 1) aceita só pedido que não está
     * CANCELADO; 2) devolve a metragem de cada item ao estoque; 3) status CANCELADO.
     * O cancelamento é definitivo e o pedido continua no banco.
     */
    @Transactional
    public PedidoResponse cancelar(Long id) {
        // A trava no pedido impede que dois cancelamentos simultâneos devolvam o estoque duas vezes.
        Pedido pedido = buscarParaAtualizar(id);
        if (pedido.getStatus() == StatusPedido.CANCELADO) {
            throw new RegraNegocioException("O pedido já está cancelado");
        }

        Map<Long, BigDecimal> metragemPorEstoque = new LinkedHashMap<>();
        for (ItemPedido item : pedido.getItens()) {
            metragemPorEstoque.put(item.getEstoque().getId(), item.getMetragem());
        }
        Map<Long, Estoque> linhas = travarLinhasDeEstoque(metragemPorEstoque);
        for (Map.Entry<Long, BigDecimal> item : metragemPorEstoque.entrySet()) {
            linhas.get(item.getKey()).somar(item.getValue());
        }

        pedido.cancelar();
        return PedidoResponse.de(pedido);
    }

    private Pedido buscarParaAtualizar(Long id) {
        return pedidoRepository.buscarParaAtualizar(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado"));
    }

    // Regra 6.1, passo 2: se o pedido vier de uma negociação, o cliente do pedido é o cliente dela.
    private void verificarClienteDaNegociacao(Long idNegociacao, Cliente cliente) {
        if (idNegociacao == null) {
            return; // venda direta
        }
        Long idClienteDaNegociacao = pedidoRepository.buscarIdClienteDaNegociacao(idNegociacao)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Negociação não encontrada"));
        if (!idClienteDaNegociacao.equals(cliente.getId())) {
            throw new RegraNegocioException("A negociação informada é de outro cliente. "
                    + "O pedido de uma negociação precisa ser para o cliente dela.");
        }
    }

    /**
     * Cada tecido + cor aparece uma vez por pedido (regra 6.2 e uk_item_pedido_pedido_estoque):
     * se a mesma linha de estoque vier duas vezes, as metragens são somadas numa linha só.
     * O LinkedHashMap mantém os itens na ordem em que foram enviados.
     */
    private static Map<Long, BigDecimal> juntarItensRepetidos(List<ItemPedidoRequest> itens) {
        Map<Long, BigDecimal> metragemPorEstoque = new LinkedHashMap<>();
        for (ItemPedidoRequest item : itens) {
            metragemPorEstoque.merge(item.idEstoque(), item.metragem(), BigDecimal::add);
        }
        return metragemPorEstoque;
    }

    // Trava as linhas de estoque até o fim da transação e as devolve por id.
    private Map<Long, Estoque> travarLinhasDeEstoque(Map<Long, BigDecimal> metragemPorEstoque) {
        Map<Long, Estoque> linhas = new HashMap<>();
        for (Estoque linha : estoqueRepository.buscarPorIdsParaAtualizar(metragemPorEstoque.keySet())) {
            linhas.put(linha.getId(), linha);
        }
        if (linhas.size() != metragemPorEstoque.size()) {
            throw new RecursoNaoEncontradoException("Linha de estoque não encontrada");
        }
        return linhas;
    }

    // 18.00 vira "18" e 18.50 vira "18,5", como na mensagem "Linho Bege 102 tem 18 m".
    static String emMetros(BigDecimal metragem) {
        return metragem.stripTrailingZeros().toPlainString().replace('.', ',');
    }
}
