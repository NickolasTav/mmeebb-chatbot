package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.dto.AnswerEvaluationDto;
import br.edu.unipam.tcc.entity.Flashcard;

/**
 * Avaliação da resposta enviada pelo estudante durante uma revisão.
 */
public interface AnswerEvaluationService {

    /**
     * Julga se a resposta do estudante corresponde ao gabarito do flashcard.
     * Questões de múltipla escolha são resolvidas por comparação direta; respostas
     * dissertativas são avaliadas semanticamente, aceitando o conceito correto
     * expresso com palavras próprias.
     *
     * @param studentAnswer Texto enviado pelo estudante.
     * @param flashcard     Flashcard em revisão.
     * @return Veredito e comentário pedagógico curto.
     */
    AnswerEvaluationDto evaluate(String studentAnswer, Flashcard flashcard);
}
