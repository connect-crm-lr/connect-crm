package br.com.lrrepresentacoes.connectcrm.service;

import br.com.lrrepresentacoes.connectcrm.dto.MoverEtapaRequest;
import br.com.lrrepresentacoes.connectcrm.dto.NegociacaoRequest;
import br.com.lrrepresentacoes.connectcrm.dto.NegociacaoResponse;
import br.com.lrrepresentacoes.connectcrm.exception.RecursoNaoEncontradoException;
import br.com.lrrepresentacoes.connectcrm.exception.RegraNegocioException;
import br.com.lrrepresentacoes.connectcrm.model.Cliente;
import br.com.lrrepresentacoes.connectcrm.model.EstagioNegociacao;
import br.com.lrrepresentacoes.connectcrm.model.Negociacao;
import br.com.lrrepresentacoes.connectcrm.repository.ClienteRepository;
import br.com.lrrepresentacoes.connectcrm.repository.NegociacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

// Padrão Singleton: bean do Spring, uma única instância na aplicação.
@Service
public class NegociacaoService {

    // Mesmo fuso do banco e do Hibernate (AGENTS 4), independente do fuso da JVM.
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final NegociacaoRepository negociacaoRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioLogadoService usuarioLogadoService;

    public NegociacaoService(NegociacaoRepository negociacaoRepository, ClienteRepository clienteRepository,
                             UsuarioLogadoService usuarioLogadoService) {
        this.negociacaoRepository = negociacaoRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioLogadoService = usuarioLogadoService;
    }

    /**
     * Cria o card na primeira etapa do funil (PRIMEIRO_CONTATO), com o usuário logado.
     * Não se cria negociação para cliente desativado (regras 6.3 e 6.5).
     */
    // Padrão Decorator: @Transactional faz o Spring envolver o método num proxy que abre e fecha a transação.
    @Transactional
    public NegociacaoResponse criar(NegociacaoRequest request) {
        Cliente cliente = clienteRepository.findById(request.idCliente())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado"));
        if (!cliente.isAtivo()) {
            throw new RegraNegocioException("O cliente " + cliente.getNome()
                    + " está desativado. Reative o cliente antes de criar a negociação.");
        }
        Negociacao negociacao = new Negociacao(cliente, usuarioLogadoService.obter(),
                request.descricao(), LocalDateTime.now(FUSO));
        return NegociacaoResponse.de(negociacaoRepository.save(negociacao));
    }

    /**
     * Move o card de etapa (regra 6.3 / RD-010). As etapas são fixas e o card é movido
     * manualmente, para qualquer outra etapa. As datas são atualizadas pela entidade:
     * data_ultima_movimentacao sempre; data_fechamento ao entrar em FECHADA/PERDIDA ou sair delas.
     */
    @Transactional
    public NegociacaoResponse moverEtapa(Long id, MoverEtapaRequest request) {
        Negociacao negociacao = negociacaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Negociação não encontrada"));
        // O @Pattern do request já garantiu que o texto é um dos valores do enum.
        EstagioNegociacao destino = EstagioNegociacao.valueOf(request.estagio());

        // Soltar o card na mesma coluna não é uma mudança de etapa: as datas não podem mudar.
        if (negociacao.getEstagio() == destino) {
            throw new RegraNegocioException("A negociação já está na etapa " + destino);
        }
        // Cliente desativado não pode ter negociação em andamento (regra 6.5), então a
        // negociação encerrada dele não volta para uma etapa aberta.
        Cliente cliente = negociacao.getCliente();
        if (!destino.isEncerrada() && !cliente.isAtivo()) {
            throw new RegraNegocioException("O cliente " + cliente.getNome()
                    + " está desativado. Reative o cliente antes de reabrir a negociação.");
        }

        negociacao.moverPara(destino, LocalDateTime.now(FUSO));
        return NegociacaoResponse.de(negociacao);
    }
}
