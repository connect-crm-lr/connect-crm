package br.com.lrrepresentacoes.connectcrm.service;

import br.com.lrrepresentacoes.connectcrm.dto.EntradaEstoqueRequest;
import br.com.lrrepresentacoes.connectcrm.dto.EntradaEstoqueResponse;
import br.com.lrrepresentacoes.connectcrm.dto.EstoqueResponse;
import br.com.lrrepresentacoes.connectcrm.exception.RecursoNaoEncontradoException;
import br.com.lrrepresentacoes.connectcrm.exception.RegraNegocioException;
import br.com.lrrepresentacoes.connectcrm.model.EntradaEstoque;
import br.com.lrrepresentacoes.connectcrm.model.Estoque;
import br.com.lrrepresentacoes.connectcrm.model.Tecido;
import br.com.lrrepresentacoes.connectcrm.repository.EntradaEstoqueRepository;
import br.com.lrrepresentacoes.connectcrm.repository.EstoqueRepository;
import br.com.lrrepresentacoes.connectcrm.repository.TecidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

// Padrão Singleton: bean do Spring, uma única instância na aplicação.
@Service
public class EstoqueService {

    // Regra 6.6: estoque baixo = metragem < 20 m.
    static final BigDecimal LIMITE_ESTOQUE_BAIXO = new BigDecimal("20");

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final EstoqueRepository estoqueRepository;
    private final EntradaEstoqueRepository entradaEstoqueRepository;
    private final TecidoRepository tecidoRepository;

    public EstoqueService(EstoqueRepository estoqueRepository,
                          EntradaEstoqueRepository entradaEstoqueRepository,
                          TecidoRepository tecidoRepository) {
        this.estoqueRepository = estoqueRepository;
        this.entradaEstoqueRepository = entradaEstoqueRepository;
        this.tecidoRepository = tecidoRepository;
    }

    /**
     * Saldo por tecido + cor, sem linhas de tecido desativado.
     * Com "somenteBaixo", devolve só as linhas abaixo de 20 m (usado também pelo Dashboard).
     */
    @Transactional(readOnly = true)
    public List<EstoqueResponse> listar(String busca, boolean somenteBaixo) {
        List<Estoque> linhas;
        if (somenteBaixo) {
            linhas = estoqueRepository.listarAbaixoDe(LIMITE_ESTOQUE_BAIXO);
        } else if (busca == null || busca.isBlank()) {
            linhas = estoqueRepository.listarDeTecidosAtivos();
        } else {
            linhas = estoqueRepository.buscarDeTecidosAtivos("%" + busca.trim().toLowerCase(Locale.ROOT) + "%");
        }
        // Padrão Iterator: stream() percorre as linhas convertendo cada uma.
        return linhas.stream()
                .map(linha -> EstoqueResponse.de(linha, LIMITE_ESTOQUE_BAIXO))
                .toList();
    }

    /**
     * Registrar entrada de estoque (regra 6.1), tudo na mesma transação:
     * 1) tecido desativado é recusado; 2) busca a linha tecido + cor ou cria com 0;
     * 3) grava a entrada no histórico; 4) soma a metragem ao saldo.
     * Se qualquer passo falhar, nada é gravado.
     */
    // Padrão Decorator: o Spring envolve o método num proxy que abre e fecha a transação.
    @Transactional
    public EntradaEstoqueResponse registrarEntrada(EntradaEstoqueRequest request) {
        Tecido tecido = tecidoRepository.findById(request.idTecido())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tecido não encontrado"));
        if (!tecido.isAtivo()) {
            throw new RegraNegocioException("O tecido " + tecido.getNome()
                    + " está desativado. Reative o tecido antes de registrar a entrada.");
        }

        String cor = request.corCodigo().trim();
        Estoque estoque = estoqueRepository.buscarParaAtualizar(tecido.getId(), cor)
                .orElseGet(() -> estoqueRepository.save(new Estoque(tecido, cor)));

        LocalDate data = request.dataEntrada() != null ? request.dataEntrada() : LocalDate.now(FUSO);
        EntradaEstoque entrada = entradaEstoqueRepository.save(
                new EntradaEstoque(estoque, request.metragem(), data));

        estoque.somar(request.metragem());
        return EntradaEstoqueResponse.de(entrada);
    }

    @Transactional(readOnly = true)
    public List<EntradaEstoqueResponse> listarEntradas(Long idEstoque) {
        if (!estoqueRepository.existsById(idEstoque)) {
            throw new RecursoNaoEncontradoException("Linha de estoque não encontrada");
        }
        return entradaEstoqueRepository.findByEstoqueIdOrderByDataEntradaDescIdDesc(idEstoque).stream()
                .map(EntradaEstoqueResponse::de)
                .toList();
    }
}
