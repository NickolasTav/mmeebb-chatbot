package br.edu.unipam.tcc.dto;

import br.edu.unipam.tcc.entity.enums.TopicMastery;

import java.util.List;
import java.util.UUID;

/**
 * Diagnóstico de desempenho de um estudante na janela de análise. Serve à mensagem do WhatsApp e
 * ao endpoint administrativo que acompanha o grupo piloto.
 */
public record StudentPerformanceReportDto(
        UUID studentId,
        String studentName,
        int windowDays,
        long totalAttempts,
        long correctAttempts,
        double overallAccuracy,
        boolean enoughData,
        List<TopicPerformanceDto> topics
) {

    public int accuracyPercentage() {
        return (int) Math.round(overallAccuracy * 100);
    }

    /** Tópicos que a camada adaptativa está segurando em ciclo curto, do pior para o melhor. */
    public List<TopicPerformanceDto> weakestTopics(int limit) {
        return topics.stream()
                .filter(topic -> topic.mastery().limitsInterval())
                .limit(limit)
                .toList();
    }

    /** Tópicos já consolidados, que seguem o MMEEBB clássico até o teto de 2^13 dias. */
    public List<TopicPerformanceDto> masteredTopics(int limit) {
        return topics.stream()
                .filter(topic -> topic.mastery() == TopicMastery.DOMINADO)
                .limit(limit)
                .toList();
    }
}
