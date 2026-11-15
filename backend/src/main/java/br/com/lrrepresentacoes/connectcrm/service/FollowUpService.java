package br.com.lrrepresentacoes.connectcrm.service;

import br.com.lrrepresentacoes.connectcrm.dto.FollowUpRequest;
import br.com.lrrepresentacoes.connectcrm.dto.FollowUpResponse;
import br.com.lrrepresentacoes.connectcrm.exception.RecursoNaoEncontradoException;
import br.com.lrrepresentacoes.connectcrm.exception.RegraNegocioException;
import br.com.lrrepresentacoes.connectcrm.model.Cliente;
import br.com.lrrepresentacoes.connectcrm.model.FollowUp;
import br.com.lrrepresentacoes.connectcrm.repository.ClienteRepository;
import br.com.lrrepresentacoes.connectcrm.repository.FollowUpRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

// Padrão Singleton: bean do Spring, uma única instância na aplicação.
@Service
public class FollowUpService {

    // Mesmo fuso do banco e do Hibernate (AGENTS 4): "hoje" não depende do fuso da JVM.
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final FollowUpRepository followUpRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioLogadoService usuarioLogadoService;

    public FollowUpService(FollowUpRepository followUpRepository, ClienteRepository clienteRepository,
                           UsuarioLogadoService usuarioLogadoService) {
        this.followUpRepository = followUpRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioLogadoService = usuarioLogadoService;
    }

    /**
     * Cria a próxima ação a fazer com o cliente. O follow-up nasce pendente (data_conclusao
     * vazia) e é gravado com o usuário logado (id_usuario).
     */
    // Padrão Decorator: @Transactional faz o Spring envolver o método num proxy que abre e fecha a transação.
    @Transactional
    public FollowUpResponse criar(Long idCliente, FollowUpRequest request) {
        Cliente cliente = buscarCliente(idCliente);
        FollowUp followUp = new FollowUp(cliente, usuarioLogadoService.obter(),
                request.dataPrevista(), request.descricao());
        return FollowUpResponse.de(followUpRepository.save(followUp));
    }

    // Aba Follow-ups da ficha do cliente: pendentes e concluídos, pela data prevista.
    @Transactional(readOnly = true)
    public List<FollowUpResponse> listarDoCliente(Long idCliente) {
        buscarCliente(idCliente);
        // Padrão Iterator: stream() percorre a lista convertendo cada follow-up.
        return followUpRepository.findByClienteIdOrderByDataPrevistaAscIdAsc(idCliente).stream()
                .map(FollowUpResponse::de)
                .toList();
    }

    /**
     * Concluir = preencher data_conclusao com a data/hora atual (regra 6.4).
     * Concluir de novo é recusado, para a data da primeira conclusão não ser trocada.
     */
    @Transactional
    public FollowUpResponse concluir(Long id) {
        FollowUp followUp = followUpRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Follow-up não encontrado"));
        if (followUp.isConcluido()) {
            throw new RegraNegocioException("O follow-up já foi concluído");
        }
        followUp.concluir(LocalDateTime.now(FUSO));
        return FollowUpResponse.de(followUp);
    }

    /**
     * "Pendências de hoje" (RD-011): não concluídos, com data prevista até hoje e de cliente
     * ativo. As três condições ficam na consulta do repository; aqui entra só o "hoje".
     */
    @Transactional(readOnly = true)
    public List<FollowUpResponse> listarPendenciasDeHoje() {
        return followUpRepository.listarPendencias(LocalDate.now(FUSO)).stream()
                .map(FollowUpResponse::de)
                .toList();
    }

    private Cliente buscarCliente(Long idCliente) {
        return clienteRepository.findById(idCliente)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado"));
    }
}
