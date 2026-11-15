package br.com.lrrepresentacoes.connectcrm.service;

import br.com.lrrepresentacoes.connectcrm.dto.DashboardResponse;
import br.com.lrrepresentacoes.connectcrm.dto.EstoqueResponse;
import br.com.lrrepresentacoes.connectcrm.dto.FollowUpResponse;
import br.com.lrrepresentacoes.connectcrm.model.EstagioNegociacao;
import br.com.lrrepresentacoes.connectcrm.repository.EstoqueRepository;
import br.com.lrrepresentacoes.connectcrm.repository.FollowUpRepository;
import br.com.lrrepresentacoes.connectcrm.repository.NegociacaoRepository;
import br.com.lrrepresentacoes.connectcrm.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// Padrão Singleton: bean do Spring, uma única instância na aplicação.
@Service
public class DashboardService {

    // Mesmo fuso do banco e do Hibernate (AGENTS 4): "hoje" e "mês atual" não dependem do fuso da JVM.
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final PedidoRepository pedidoRepository;
    private final NegociacaoRepository negociacaoRepository;
    private final FollowUpRepository followUpRepository;
    private final EstoqueRepository estoqueRepository;

    public DashboardService(PedidoRepository pedidoRepository, NegociacaoRepository negociacaoRepository,
                            FollowUpRepository followUpRepository, EstoqueRepository estoqueRepository) {
        this.pedidoRepository = pedidoRepository;
        this.negociacaoRepository = negociacaoRepository;
        this.followUpRepository = followUpRepository;
        this.estoqueRepository = estoqueRepository;
    }

    // Padrão Decorator: com @Transactional o proxy do Spring abre uma transação só de leitura.
    @Transactional(readOnly = true)
    public DashboardResponse resumo() {
        return resumo(LocalDate.now(FUSO));
    }

    // Recebe o "hoje" para os testes conseguirem fixar a data (virada de mês, por exemplo).
    DashboardResponse resumo(LocalDate hoje) {
        // Mês atual = do dia 1 às 00:00 até o dia 1 do mês seguinte (fim exclusivo).
        LocalDateTime inicioMes = hoje.withDayOfMonth(1).atStartOfDay();
        LocalDateTime inicioProximoMes = inicioMes.plusMonths(1);

        // Regra 6.2: pedidos CANCELADO ficam fora das vendas do mês (filtro na consulta).
        BigDecimal vendas = pedidoRepository.somarVendasEntre(inicioMes, inicioProximoMes)
                .setScale(2, RoundingMode.HALF_UP);
        long pedidos = pedidoRepository.contarVendasEntre(inicioMes, inicioProximoMes);

        // Regra 6.4: pendências = não concluídas, previstas até hoje e só de cliente ativo.
        // Padrão Iterator: stream() percorre as listas convertendo cada entidade no DTO.
        List<FollowUpResponse> pendencias = followUpRepository.listarPendencias(hoje).stream()
                .map(FollowUpResponse::de)
                .toList();

        // Regra 6.6: estoque baixo = metragem < 20 m, só de tecido ativo (regra 6.5).
        List<EstoqueResponse> estoqueBaixo = estoqueRepository
                .listarAbaixoDe(EstoqueService.LIMITE_ESTOQUE_BAIXO).stream()
                .map(linha -> EstoqueResponse.de(linha, EstoqueService.LIMITE_ESTOQUE_BAIXO))
                .toList();

        return new DashboardResponse(vendas, pedidos, negociacoesPorEtapa(), pendencias, estoqueBaixo);
    }

    // EnumMap mantém as etapas na ordem do enum (ordem do funil, regra 6.3); todas começam com zero.
    private Map<EstagioNegociacao, Long> negociacoesPorEtapa() {
        Map<EstagioNegociacao, Long> porEtapa = new EnumMap<>(EstagioNegociacao.class);
        // Padrão Iterator: for-each pelas etapas do enum e pelas linhas da contagem.
        for (EstagioNegociacao estagio : EstagioNegociacao.values()) {
            porEtapa.put(estagio, 0L);
        }
        for (Object[] linha : negociacaoRepository.contarPorEstagio()) {
            porEtapa.put((EstagioNegociacao) linha[0], ((Number) linha[1]).longValue());
        }
        return porEtapa;
    }
}
