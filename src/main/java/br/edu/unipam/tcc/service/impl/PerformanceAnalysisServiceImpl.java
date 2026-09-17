package br.edu.unipam.tcc.service.impl;

import br.edu.unipam.tcc.config.AdaptiveProperties;
import br.edu.unipam.tcc.dto.AttemptTotalsDto;
import br.edu.unipam.tcc.dto.TopicAttemptAggregateDto;
import br.edu.unipam.tcc.dto.TopicPerformanceDto;
import br.edu.unipam.tcc.entity.enums.TopicMastery;
import br.edu.unipam.tcc.repository.ReviewAttemptRepository;
import br.edu.unipam.tcc.service.PerformanceAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceAnalysisServiceImpl implements PerformanceAnalysisService {

    private static final Comparator<TopicPerformanceDto> DO_MAIS_FRAGIL_AO_MAIS_DOMINADO =
            Comparator.comparingInt((TopicPerformanceDto t) -> t.mastery().reviewPriority())
                    .thenComparing(Comparator.comparingDouble(TopicPerformanceDto::errorRate).reversed())
                    .thenComparing(TopicPerformanceDto::topic);

    private final ReviewAttemptRepository reviewAttemptRepository;
    private final AdaptiveProperties adaptiveProperties;
    private final Clock clock;

    @Override
    public List<TopicPerformanceDto> analyzeTopics(UUID studentId) {
        return reviewAttemptRepository.aggregateByTopic(studentId, windowStart()).stream()
                .map(this::toPerformance)
                .sorted(DO_MAIS_FRAGIL_AO_MAIS_DOMINADO)
                .toList();
    }

    @Override
    public TopicPerformanceDto analyzeTopic(UUID studentId, Long subjectId, String topic) {
        return analyzeTopics(studentId).stream()
                .filter(performance -> Objects.equals(performance.subjectId(), subjectId))
                .filter(performance -> Objects.equals(performance.topic(), topic))
                .findFirst()
                .orElseGet(() -> TopicPerformanceDto.noData(subjectId, null, topic));
    }

    @Override
    public AttemptTotalsDto totals(UUID studentId) {
        return reviewAttemptRepository.totalsByStudent(studentId, windowStart());
    }

    @Override
    public int windowDays() {
        return adaptiveProperties.getWindowDays();
    }

    private LocalDateTime windowStart() {
        return LocalDateTime.now(clock).minusDays(adaptiveProperties.getWindowDays());
    }

    private TopicPerformanceDto toPerformance(TopicAttemptAggregateDto aggregate) {
        double errorRate = aggregate.errorRate();
        return new TopicPerformanceDto(
                aggregate.subjectId(),
                aggregate.subjectName(),
                aggregate.topic(),
                aggregate.attempts(),
                aggregate.errors(),
                errorRate,
                classify(aggregate.attempts(), errorRate)
        );
    }

    /**
     * Abaixo da amostra mínima não há evidência: o tópico fica {@code SEM_DADOS} e o MMEEBB roda
     * na forma clássica. Desligar a camada adaptativa tem o mesmo efeito, sem apagar as
     * estatísticas que o relatório de desempenho continua exibindo.
     */
    private TopicMastery classify(long attempts, double errorRate) {
        if (!adaptiveProperties.isEnabled() || attempts < adaptiveProperties.getMinAttempts()) {
            return TopicMastery.SEM_DADOS;
        }
        if (errorRate >= adaptiveProperties.getFragileErrorRate()) {
            return TopicMastery.FRAGIL;
        }
        if (errorRate >= adaptiveProperties.getConsolidatingErrorRate()) {
            return TopicMastery.EM_CONSOLIDACAO;
        }
        return TopicMastery.DOMINADO;
    }
}
