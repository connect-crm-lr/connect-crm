package br.com.lrrepresentacoes.connectcrm.dto;

import br.com.lrrepresentacoes.connectcrm.model.FollowUp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Follow-up na ficha do cliente e em "Pendências de hoje". O nome do cliente vai junto
 * porque a lista de pendências mistura follow-ups de vários clientes.
 * dataConclusao vazia = pendente.
 */
public record FollowUpResponse(
        Long id,
        Long idCliente,
        String cliente,
        LocalDate dataPrevista,
        String descricao,
        LocalDateTime dataConclusao
) {

    // Padrão Factory Method: único ponto de conversão da entidade FollowUp para o DTO de resposta.
    public static FollowUpResponse de(FollowUp followUp) {
        return new FollowUpResponse(followUp.getId(), followUp.getCliente().getId(),
                followUp.getCliente().getNome(), followUp.getDataPrevista(), followUp.getDescricao(),
                followUp.getDataConclusao());
    }
}
