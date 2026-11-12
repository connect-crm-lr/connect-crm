package br.com.lrrepresentacoes.connectcrm.service;

import br.com.lrrepresentacoes.connectcrm.dto.InteracaoRequest;
import br.com.lrrepresentacoes.connectcrm.dto.InteracaoResponse;
import br.com.lrrepresentacoes.connectcrm.exception.RecursoNaoEncontradoException;
import br.com.lrrepresentacoes.connectcrm.model.Cliente;
import br.com.lrrepresentacoes.connectcrm.model.Interacao;
import br.com.lrrepresentacoes.connectcrm.model.TipoInteracao;
import br.com.lrrepresentacoes.connectcrm.repository.ClienteRepository;
import br.com.lrrepresentacoes.connectcrm.repository.InteracaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Padrão Singleton: bean do Spring, uma única instância na aplicação.
@Service
public class InteracaoService {

    private final InteracaoRepository interacaoRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioLogadoService usuarioLogadoService;

    public InteracaoService(InteracaoRepository interacaoRepository, ClienteRepository clienteRepository,
                            UsuarioLogadoService usuarioLogadoService) {
        this.interacaoRepository = interacaoRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioLogadoService = usuarioLogadoService;
    }

    /**
     * Registra uma conversa já realizada (regra 6.4): tipo, data em que ocorreu e descrição.
     * A interação pertence ao cliente e é gravada com o usuário logado (id_usuario).
     * A regra 6.5 só bloqueia pedido e negociação para cliente desativado; a interação é
     * histórico e continua permitida.
     */
    // Padrão Decorator: @Transactional faz o Spring envolver o método num proxy que abre e fecha a transação.
    @Transactional
    public InteracaoResponse registrar(Long idCliente, InteracaoRequest request) {
        Cliente cliente = buscarCliente(idCliente);
        // O @Pattern do request já garantiu que o texto é um dos valores do enum.
        TipoInteracao tipo = TipoInteracao.valueOf(request.tipo());
        Interacao interacao = new Interacao(cliente, usuarioLogadoService.obter(), tipo,
                request.dataInteracao(), request.descricao());
        return InteracaoResponse.de(interacaoRepository.save(interacao));
    }

    // Aba Interações da ficha do cliente: a mais recente primeiro.
    @Transactional(readOnly = true)
    public List<InteracaoResponse> listarDoCliente(Long idCliente) {
        buscarCliente(idCliente);
        // Padrão Iterator: stream() percorre a lista convertendo cada interação.
        return interacaoRepository.findByClienteIdOrderByDataInteracaoDescIdDesc(idCliente).stream()
                .map(InteracaoResponse::de)
                .toList();
    }

    private Cliente buscarCliente(Long idCliente) {
        return clienteRepository.findById(idCliente)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado"));
    }
}
