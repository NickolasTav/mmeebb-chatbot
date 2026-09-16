package br.edu.unipam.tcc.session;

import java.util.Optional;

/**
 * Armazenamento do estado conversacional do chatbot.
 */
public interface ChatSessionStore {

    Optional<ChatSessionState> find(String phoneNumber);

    /**
     * Persiste o estado e renova o TTL da sessão.
     */
    void save(ChatSessionState state);

    void delete(String phoneNumber);
}
