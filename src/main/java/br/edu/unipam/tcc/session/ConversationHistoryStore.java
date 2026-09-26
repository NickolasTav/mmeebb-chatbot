package br.edu.unipam.tcc.session;

import java.util.List;

/**
 * Memória curta da conversa com o tutor de IA, por telefone. Guarda só as trocas mais recentes
 * e esquece sozinha depois de um período sem conversa.
 */
public interface ConversationHistoryStore {

    /** Trocas mais recentes, da mais antiga para a mais nova; vazia quando não há conversa. */
    List<ConversationTurn> recent(String phoneNumber);

    /** Registra uma troca, descarta as que excedem a janela e renova a expiração. */
    void append(String phoneNumber, String question, String answer);

    /** Esquece toda a conversa do telefone. */
    void clear(String phoneNumber);
}
