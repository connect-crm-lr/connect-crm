package br.com.lrrepresentacoes.connectcrm.model;

/**
 * Etapas fixas do funil de vendas, na ordem em que aparecem no kanban (regra 6.3).
 * Os valores são os mesmos do CHECK ck_negociacao_estagio da V1; o front converte
 * para o texto de exibição ("Aguardando resposta").
 */
public enum EstagioNegociacao {
    PRIMEIRO_CONTATO,
    MOSTRUARIO_ENVIADO,
    AGUARDANDO_RESPOSTA,
    FECHADA,
    PERDIDA;

    // FECHADA e PERDIDA encerram a negociação; as outras etapas são "em andamento" (regra 6.5).
    public boolean isEncerrada() {
        return this == FECHADA || this == PERDIDA;
    }
}
