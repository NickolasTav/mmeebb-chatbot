package br.edu.unipam.tcc.dto;

import br.edu.unipam.tcc.entity.enums.TopicMastery;

/**
 * Desempenho do estudante em um tópico dentro da janela de análise, já classificado.
 * É o insumo das três decisões adaptativas: teto de intervalo, ordem da fila e reforço dirigido.
 */
public record TopicPerformanceDto(
        Long subjectId,
        String subjectName,
        String topic,
        long attempts,
        long errors,
        double errorRate,
        TopicMastery mastery
) {

    public static TopicPerformanceDto noData(Long subjectId, String subjectName, String topic) {
        return new TopicPerformanceDto(subjectId, subjectName, topic, 0L, 0L, 0.0, TopicMastery.SEM_DADOS);
    }

    public long correctAttempts() {
        return attempts - errors;
    }

    /** Maior intervalo que um cartão deste tópico pode alcançar hoje: 2^(teto de N). */
    public int maxIntervalDays() {
        return 1 << mastery.maxNIndex();
    }

    public int errorPercentage() {
        return (int) Math.round(errorRate * 100);
    }
}
