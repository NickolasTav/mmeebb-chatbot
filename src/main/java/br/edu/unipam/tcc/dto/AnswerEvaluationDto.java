package br.edu.unipam.tcc.dto;

/**
 * Veredito da avaliação de uma resposta de revisão.
 *
 * @param correct  Indica se a resposta foi aceita como correta pelo motor de avaliação.
 * @param feedback Comentário curto sobre a resposta, ou {@code null} quando não houver.
 * @param isDoubt  Indica que o estudante pediu ajuda/explicação em vez de tentar responder;
 *                 quando {@code true}, a resposta não deve pontuar nem alterar o agendamento.
 */
public record AnswerEvaluationDto(boolean correct, String feedback, boolean isDoubt) {

    public static AnswerEvaluationDto accepted() {
        return new AnswerEvaluationDto(true, null, false);
    }

    public static AnswerEvaluationDto rejected() {
        return new AnswerEvaluationDto(false, null, false);
    }

    public static AnswerEvaluationDto doubt() {
        return new AnswerEvaluationDto(false, null, true);
    }
}
