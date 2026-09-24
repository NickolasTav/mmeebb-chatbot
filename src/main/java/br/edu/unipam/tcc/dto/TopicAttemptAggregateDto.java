package br.edu.unipam.tcc.dto;

/**
 * Tentativas e erros de um estudante em um tópico dentro da janela de análise.
 * Projeção crua do banco: a classificação de domínio é derivada disso na camada de serviço.
 */
public record TopicAttemptAggregateDto(
        Long subjectId,
        String subjectName,
        String topic,
        long attempts,
        long errors
) {

    public double errorRate() {
        return attempts == 0 ? 0.0 : (double) errors / attempts;
    }
}
