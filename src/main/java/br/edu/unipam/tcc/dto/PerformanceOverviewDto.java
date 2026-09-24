package br.edu.unipam.tcc.dto;

import java.util.List;

/**
 * Visão agregada do desempenho de todos os estudantes na janela de análise. É o insumo mínimo
 * para a seção de resultados da monografia: sem isso não há como discutir adesão nem retenção.
 */
public record PerformanceOverviewDto(
        int windowDays,
        long students,
        long totalAttempts,
        long correctAttempts,
        double overallAccuracy,
        List<SubjectAttemptAggregateDto> bySubject
) {
}
