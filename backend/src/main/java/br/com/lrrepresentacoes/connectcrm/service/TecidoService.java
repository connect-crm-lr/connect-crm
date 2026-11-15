package br.com.lrrepresentacoes.connectcrm.service;

import br.com.lrrepresentacoes.connectcrm.dto.TecidoRequest;
import br.com.lrrepresentacoes.connectcrm.dto.TecidoResponse;
import br.com.lrrepresentacoes.connectcrm.exception.RecursoNaoEncontradoException;
import br.com.lrrepresentacoes.connectcrm.exception.RegraNegocioException;
import br.com.lrrepresentacoes.connectcrm.model.Tecido;
import br.com.lrrepresentacoes.connectcrm.repository.TecidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

// Padrão Singleton: bean do Spring, uma única instância na aplicação.
@Service
public class TecidoService {

    private final TecidoRepository tecidoRepository;

    public TecidoService(TecidoRepository tecidoRepository) {
        this.tecidoRepository = tecidoRepository;
    }

    /**
     * Lista o catálogo em ordem de nome. Sem "incluirInativos", mostra só os ativos (regra 6.5).
     * A busca procura o texto no nome ou na composição, sem diferenciar maiúsculas (RF-015).
     */
    @Transactional(readOnly = true)
    public List<TecidoResponse> listar(String busca, boolean incluirInativos) {
        List<Tecido> tecidos;
        if (busca == null || busca.isBlank()) {
            tecidos = incluirInativos
                    ? tecidoRepository.findAllByOrderByNomeAsc()
                    : tecidoRepository.findByAtivoTrueOrderByNomeAsc();
        } else {
            String padrao = "%" + escaparLike(busca.trim().toLowerCase(Locale.ROOT)) + "%";
            tecidos = tecidoRepository.buscar(padrao, incluirInativos);
        }
        // Padrão Iterator: stream() percorre a lista convertendo cada tecido.
        return tecidos.stream().map(TecidoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public TecidoResponse buscarPorId(Long id) {
        return TecidoResponse.de(buscar(id));
    }

    @Transactional
    public TecidoResponse criar(TecidoRequest request) {
        Tecido tecido = new Tecido(
                request.nome().trim(),
                textoOpcional(request.composicao()),
                request.gramatura(),
                request.largura(),
                request.precoMetro(),
                textoOpcional(request.fotoUrl()));
        return TecidoResponse.de(tecidoRepository.save(tecido));
    }

    /**
     * Alterar o preço não muda pedidos antigos: cada item do pedido guarda o
     * preco_metro do momento da venda (regra 6.2).
     */
    @Transactional
    public TecidoResponse atualizar(Long id, TecidoRequest request) {
        Tecido tecido = buscar(id);
        tecido.atualizar(
                request.nome().trim(),
                textoOpcional(request.composicao()),
                request.gramatura(),
                request.largura(),
                request.precoMetro(),
                textoOpcional(request.fotoUrl()));
        return TecidoResponse.de(tecido);
    }

    /**
     * Tecido nunca é apagado (regra 6.5). Desativado, ele sai do catálogo e da tela
     * Estoque, e não pode ser vendido nem receber entrada de estoque.
     */
    @Transactional
    public TecidoResponse desativar(Long id) {
        Tecido tecido = buscar(id);
        if (!tecido.isAtivo()) {
            throw new RegraNegocioException("O tecido já está desativado");
        }
        tecido.desativar();
        return TecidoResponse.de(tecido);
    }

    // Reativação de tecido: RF-023, confirmada no MVP em 03/11.
    @Transactional
    public TecidoResponse reativar(Long id) {
        Tecido tecido = buscar(id);
        if (tecido.isAtivo()) {
            throw new RegraNegocioException("O tecido já está ativo");
        }
        tecido.reativar();
        return TecidoResponse.de(tecido);
    }

    private Tecido buscar(Long id) {
        return tecidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tecido não encontrado"));
    }

    /**
     * No LIKE, '%' e '_' são curingas. A composição costuma ter '%' ("100% Linho"),
     * então o texto digitado é escapado para ser procurado literalmente.
     * A barra é escapada primeiro, por ser o próprio caractere de escape (ESCAPE '\' na query).
     */
    static String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    // Campo opcional enviado em branco é gravado como null, e não como texto vazio.
    private static String textoOpcional(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }
}
