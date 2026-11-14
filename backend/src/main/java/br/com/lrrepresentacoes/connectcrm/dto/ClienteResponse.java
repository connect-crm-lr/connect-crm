package br.com.lrrepresentacoes.connectcrm.dto;

import br.com.lrrepresentacoes.connectcrm.model.Cliente;

import java.util.Comparator;
import java.util.List;

public record ClienteResponse(
        Long id,
        String nome,
        String telefone,
        String email,
        String cpfCnpj,
        String cidade,
        String uf,
        boolean ativo,
        List<TagResponse> tags
) {

    // Padrão Factory Method: único ponto de conversão da entidade Cliente para o DTO de resposta.
    public static ClienteResponse de(Cliente cliente) {
        // Padrão Iterator: stream() percorre as tags do cliente, em ordem de nome.
        List<TagResponse> tags = cliente.getTags().stream()
                .map(TagResponse::de)
                .sorted(Comparator.comparing(TagResponse::nome, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new ClienteResponse(cliente.getId(), cliente.getNome(), cliente.getTelefone(),
                cliente.getEmail(), cliente.getCpfCnpj(), cliente.getCidade(), cliente.getUf(),
                cliente.isAtivo(), tags);
    }
}
