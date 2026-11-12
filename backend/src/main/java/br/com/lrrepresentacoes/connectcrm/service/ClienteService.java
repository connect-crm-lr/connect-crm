package br.com.lrrepresentacoes.connectcrm.service;

import br.com.lrrepresentacoes.connectcrm.dto.ClienteRequest;
import br.com.lrrepresentacoes.connectcrm.dto.ClienteResponse;
import br.com.lrrepresentacoes.connectcrm.exception.ConflitoException;
import br.com.lrrepresentacoes.connectcrm.exception.RecursoNaoEncontradoException;
import br.com.lrrepresentacoes.connectcrm.exception.RegraNegocioException;
import br.com.lrrepresentacoes.connectcrm.model.Cliente;
import br.com.lrrepresentacoes.connectcrm.model.Tag;
import br.com.lrrepresentacoes.connectcrm.repository.ClienteRepository;
import br.com.lrrepresentacoes.connectcrm.repository.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// Padrão Singleton: bean do Spring, uma única instância na aplicação.
@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final TagRepository tagRepository;
    private final UsuarioLogadoService usuarioLogadoService;

    public ClienteService(ClienteRepository clienteRepository, TagRepository tagRepository,
                          UsuarioLogadoService usuarioLogadoService) {
        this.clienteRepository = clienteRepository;
        this.tagRepository = tagRepository;
        this.usuarioLogadoService = usuarioLogadoService;
    }

    /**
     * Lista em ordem de nome. Sem "incluirInativos", só os ativos (regra 6.5).
     * A busca procura o texto no nome, sem diferenciar maiúsculas (RF-004);
     * idTag filtra pelos clientes que têm a tag.
     */
    @Transactional(readOnly = true)
    public List<ClienteResponse> listar(String busca, Long idTag, boolean incluirInativos) {
        String padrao = (busca == null || busca.isBlank())
                ? "%"
                : "%" + escaparLike(busca.trim().toLowerCase(Locale.ROOT)) + "%";
        // Padrão Iterator: stream() percorre a lista convertendo cada cliente.
        return clienteRepository.listar(padrao, idTag, incluirInativos).stream()
                .map(ClienteResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscarPorId(Long id) {
        return ClienteResponse.de(buscar(id));
    }

    // O cliente pertence ao usuário logado (id_usuario) e nasce ativo.
    // Padrão Decorator: @Transactional faz o Spring envolver o método num proxy que abre e fecha a transação.
    @Transactional
    public ClienteResponse criar(ClienteRequest request) {
        verificarCpfCnpjLivre(request.cpfCnpj(), null);
        Cliente cliente = new Cliente(usuarioLogadoService.obter(), request.nome(), request.telefone(),
                request.email(), request.cpfCnpj(), request.cidade(), request.uf());
        cliente.definirTags(resolverTags(request.tags()));
        return ClienteResponse.de(clienteRepository.save(cliente));
    }

    // As tags enviadas substituem as atuais: a que não vier na lista é desvinculada do cliente.
    @Transactional
    public ClienteResponse atualizar(Long id, ClienteRequest request) {
        Cliente cliente = buscar(id);
        verificarCpfCnpjLivre(request.cpfCnpj(), id);
        cliente.atualizarDados(request.nome(), request.telefone(), request.email(),
                request.cpfCnpj(), request.cidade(), request.uf());
        cliente.definirTags(resolverTags(request.tags()));
        return ClienteResponse.de(cliente);
    }

    /**
     * Cliente nunca é apagado, é desativado (regra 6.5). A desativação é bloqueada enquanto
     * houver pedido EM_ABERTO ou negociação em andamento, e a mensagem diz o que resolver.
     * Follow-ups pendentes não bloqueiam: eles só saem de "Pendências de hoje".
     */
    @Transactional
    public ClienteResponse desativar(Long id) {
        Cliente cliente = buscar(id);
        if (!cliente.isAtivo()) {
            throw new RegraNegocioException("O cliente já está desativado");
        }

        long pedidosEmAberto = clienteRepository.contarPedidosEmAberto(id);
        long negociacoesEmAndamento = clienteRepository.contarNegociacoesEmAndamento(id);
        if (pedidosEmAberto > 0 || negociacoesEmAndamento > 0) {
            throw new RegraNegocioException(
                    mensagemDesativacaoBloqueada(cliente.getNome(), pedidosEmAberto, negociacoesEmAndamento));
        }

        cliente.desativar();
        return ClienteResponse.de(cliente);
    }

    // Reativação de cliente: RF-024 (regra 6.5).
    @Transactional
    public ClienteResponse reativar(Long id) {
        Cliente cliente = buscar(id);
        if (cliente.isAtivo()) {
            throw new RegraNegocioException("O cliente já está ativo");
        }
        cliente.reativar();
        return ClienteResponse.de(cliente);
    }

    private Cliente buscar(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado"));
    }

    /**
     * CPF/CNPJ é opcional e único (RD-016 / uk_cliente_cpf_cnpj). Conferir antes de gravar
     * permite responder 409 dizendo qual cliente já tem o documento. Se dois cadastros
     * chegarem juntos, o UNIQUE do banco ainda barra o segundo (409 genérico do handler).
     */
    private void verificarCpfCnpjLivre(String cpfCnpj, Long idClienteAtual) {
        if (cpfCnpj == null) {
            return;
        }
        clienteRepository.findByCpfCnpj(cpfCnpj)
                .filter(outro -> !outro.getId().equals(idClienteAtual))
                .ifPresent(outro -> {
                    throw new ConflitoException("Este CPF/CNPJ já está cadastrado para o cliente " + outro.getNome()
                            + (outro.isAtivo() ? "" : " (desativado)"));
                });
    }

    /**
     * Tags são criadas na hora, no cadastro do cliente (regra 6.4): cada nome é procurado sem
     * diferenciar maiúsculas e a tag existente é reaproveitada; só o que não existe é criado.
     * Nomes repetidos na mesma lista ("Linho" e "linho") viram uma tag só.
     */
    private Set<Tag> resolverTags(List<String> nomes) {
        Map<String, String> unicos = new LinkedHashMap<>();
        // Padrão Iterator: for-each percorre os nomes enviados.
        for (String nome : nomes) {
            unicos.putIfAbsent(nome.toLowerCase(Locale.ROOT), nome);
        }
        Set<Tag> tags = new HashSet<>();
        for (String nome : unicos.values()) {
            tags.add(tagRepository.findByNomeIgnoreCase(nome)
                    .orElseGet(() -> tagRepository.save(new Tag(nome))));
        }
        return tags;
    }

    // Ex.: "Não é possível desativar o cliente Ateliê Bella Noiva. Resolva antes: 1 pedido em aberto
    // (marque como pago ou cancele) e 2 negociações em andamento (mova para Fechada ou Perdida)."
    static String mensagemDesativacaoBloqueada(String nomeCliente, long pedidos, long negociacoes) {
        List<String> pendencias = new ArrayList<>();
        if (pedidos > 0) {
            pendencias.add(pedidos + (pedidos == 1 ? " pedido em aberto" : " pedidos em aberto")
                    + " (marque como pago ou cancele)");
        }
        if (negociacoes > 0) {
            pendencias.add(negociacoes + (negociacoes == 1 ? " negociação em andamento" : " negociações em andamento")
                    + " (mova para Fechada ou Perdida)");
        }
        return "Não é possível desativar o cliente " + nomeCliente + ". Resolva antes: "
                + String.join(" e ", pendencias) + ".";
    }

    // No LIKE, '%' e '_' são curingas: o texto digitado é escapado para ser procurado literalmente.
    // A barra vem primeiro, por ser o próprio caractere de escape (ESCAPE '\' na query).
    static String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
