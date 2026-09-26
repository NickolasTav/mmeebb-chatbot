package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.dto.TutorContextDto;

/**
 * Tutor de IA que responde livremente às dúvidas do estudante, orientado pelo perfil acadêmico,
 * pela questão em revisão (quando houver) e pelas últimas trocas da conversa.
 */
public interface AiTutorService {

    /**
     * Responde à dúvida e registra a troca no histórico da conversa. Falha da IA não lança
     * exceção: devolve uma mensagem de instabilidade pronta para o WhatsApp.
     *
     * @param context  Contexto do estudante; o telefone identifica o histórico.
     * @param question Dúvida enviada pelo estudante.
     * @return Texto pronto para o WhatsApp.
     * @throws IllegalArgumentException quando a dúvida é nula ou vazia.
     */
    String answer(TutorContextDto context, String question);

    /** Esquece a conversa do estudante (comando {@code sair}). */
    void clearConversation(String phoneNumber);
}
