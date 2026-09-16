package br.edu.unipam.tcc.dto;

/**
 * Veredito da avaliação de uma resposta de revisão.
 *
 * @param correct  Indica se a resposta foi aceita como correta pelo motor de avaliação.
 * @param feedback Comentário curto sobre a resposta, ou {@code null} quando não houver.
 */
public record AnswerEvaluationDto(boolean correct, String feedback) {

    public static AnswerEvaluationDto accepted() {
        return new AnswerEvaluationDto(true, null);
    }

    public static AnswerEvaluationDto rejected() {
        return new AnswerEvaluationDto(false, null);
    }
}
