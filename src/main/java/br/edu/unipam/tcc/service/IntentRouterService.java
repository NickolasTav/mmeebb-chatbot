package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.dto.IntentResultDto;

/**
 * Classificação da intenção de mensagens em texto livre enviadas pelo estudante.
 */
public interface IntentRouterService {

    /**
     * Classifica a mensagem em uma das intenções suportadas pelo fluxo conversacional.
     * Nunca lança exceção: em caso de falha do modelo, devolve {@code ASK_DOUBT} como fallback
     * para que a mensagem do estudante siga para o RAG em vez de ser descartada.
     *
     * @param message Texto livre recebido do estudante.
     * @return Intenção detectada e, quando citada, a disciplina mencionada.
     */
    IntentResultDto classify(String message);
}
