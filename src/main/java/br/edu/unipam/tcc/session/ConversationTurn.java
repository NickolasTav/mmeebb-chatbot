package br.edu.unipam.tcc.session;

/**
 * Uma troca do estudante com o tutor de IA: a pergunta enviada e a resposta devolvida.
 * Nunca carrega o gabarito da questão em revisão (spec-09, RN-05).
 */
public record ConversationTurn(String question, String answer) {
}
