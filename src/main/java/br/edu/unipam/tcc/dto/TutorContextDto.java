package br.edu.unipam.tcc.dto;

/**
 * Contexto já resolvido para o tutor de IA: quem é o aluno, o que ele cursa e, quando a dúvida
 * surge no meio de uma revisão, a questão ativa. Campos desconhecidos ficam nulos e são omitidos
 * do prompt (spec-09, RN-02).
 *
 * @param phoneNumber    Telefone do aluno, chave do histórico da conversa.
 * @param studentName    Nome de exibição (apelido ou primeiro nome).
 * @param courseName     Curso ativo na sessão.
 * @param academicPeriod Período da matrícula no curso ativo.
 * @param subjectName    Disciplina em foco: a do flashcard em revisão ou a citada pelo aluno.
 * @param reviewQuestion Questão em revisão; nula fora do modo de revisão.
 */
public record TutorContextDto(
        String phoneNumber,
        String studentName,
        String courseName,
        Integer academicPeriod,
        String subjectName,
        ReviewQuestion reviewQuestion
) {

    /**
     * Questão que o aluno ainda vai responder. Gabarito e explicação existem só no prompt da
     * chamada em que a questão está ativa; nunca vão para o histórico (spec-09, RN-05).
     */
    public record ReviewQuestion(String topic, String question, String options, String answer, String explanation) {
    }
}
