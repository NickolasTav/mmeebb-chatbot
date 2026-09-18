package br.edu.unipam.tcc.service.impl;

import br.edu.unipam.tcc.config.AdaptiveProperties;
import br.edu.unipam.tcc.dto.AdaptiveReviewOutcomeDto;
import br.edu.unipam.tcc.dto.TopicPerformanceDto;
import br.edu.unipam.tcc.entity.Flashcard;
import br.edu.unipam.tcc.entity.RepetitionSchedule;
import br.edu.unipam.tcc.entity.ReviewAttempt;
import br.edu.unipam.tcc.entity.Student;
import br.edu.unipam.tcc.entity.enums.TopicMastery;
import br.edu.unipam.tcc.observability.MmeebbMetrics;
import br.edu.unipam.tcc.repository.FlashcardRepository;
import br.edu.unipam.tcc.repository.RepetitionScheduleRepository;
import br.edu.unipam.tcc.repository.ReviewAttemptRepository;
import br.edu.unipam.tcc.service.AdaptiveReviewService;
import br.edu.unipam.tcc.service.MmeebbService;
import br.edu.unipam.tcc.service.PerformanceAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdaptiveReviewServiceImpl implements AdaptiveReviewService {

    private final RepetitionScheduleRepository repetitionScheduleRepository;
    private final ReviewAttemptRepository reviewAttemptRepository;
    private final FlashcardRepository flashcardRepository;
    private final PerformanceAnalysisService performanceAnalysisService;
    private final MmeebbService mmeebbService;
    private final AdaptiveProperties adaptiveProperties;
    private final MmeebbMetrics mmeebbMetrics;
    private final Clock clock;

    @Override
    public AdaptiveReviewOutcomeDto processAnswer(Student student, Flashcard flashcard,
                                                  boolean correct, LocalDateTime answeredAt) {
        LocalDate today = LocalDate.now(clock);
        Long subjectId = resolveSubjectId(flashcard);
        TopicPerformanceDto performance =
                performanceAnalysisService.analyzeTopic(student.getId(), subjectId, flashcard.getTopic());

        RepetitionSchedule schedule = repetitionScheduleRepository
                .findByStudentIdAndFlashcardId(student.getId(), flashcard.getId())
                .orElseGet(() -> mmeebbService.initializeSchedule(student, flashcard, today));

        int nIndexBefore = schedule.getNIndex();
        RepetitionSchedule updated =
                mmeebbService.processAnswer(schedule, correct, answeredAt, performance.mastery());
        repetitionScheduleRepository.save(updated);

        recordAttempt(student, flashcard, correct, answeredAt, performance.mastery(), nIndexBefore, updated);

        boolean intervalCapped = wasIntervalCapped(correct, nIndexBefore, updated.getNIndex());
        boolean lapseSoftened = !correct && updated.getNIndex() > 0;
        int reinforcedCards = reinforceWeakTopic(student.getId(), subjectId, flashcard.getTopic(),
                correct, performance.mastery(), today);

        mmeebbMetrics.recordAdaptiveDecision(performance.mastery().name(),
                decisionLabel(intervalCapped, lapseSoftened));

        if (intervalCapped || lapseSoftened || reinforcedCards > 0) {
            log.info("[Adaptive] Decisão personalizada para [{}] no tópico \"{}\" ({}): "
                            + "N {} -> {}, teto aplicado={}, lapso amortecido={}, reforços={}",
                    student.getPhoneNumber(), flashcard.getTopic(), performance.mastery(),
                    nIndexBefore, updated.getNIndex(), intervalCapped, lapseSoftened, reinforcedCards);
        }

        return new AdaptiveReviewOutcomeDto(updated, performance, intervalCapped, lapseSoftened, reinforcedCards);
    }

    @Override
    public List<RepetitionSchedule> prioritize(UUID studentId, List<RepetitionSchedule> pending) {
        if (pending == null) {
            return List.of();
        }
        if (pending.size() < 2) {
            return pending;
        }

        Map<String, TopicPerformanceDto> byTopic = performanceAnalysisService.analyzeTopics(studentId).stream()
                .collect(java.util.stream.Collectors.toMap(
                        performance -> topicKey(performance.subjectId(), performance.topic()),
                        Function.identity(),
                        (first, second) -> first));

        return pending.stream().sorted(doMaisFragilAoMaisDominado(byTopic)).toList();
    }

    /**
     * A disciplina é lida por projeção porque o flashcard chega ao orquestrador fora de uma
     * transação: navegar pela relação lazy dispararia LazyInitializationException.
     */
    private Long resolveSubjectId(Flashcard flashcard) {
        return flashcardRepository.findSubjectIdById(flashcard.getId()).orElse(null);
    }

    private void recordAttempt(Student student, Flashcard flashcard, boolean correct, LocalDateTime answeredAt,
                               TopicMastery mastery, int nIndexBefore, RepetitionSchedule updated) {
        reviewAttemptRepository.save(ReviewAttempt.builder()
                .student(student)
                .flashcard(flashcard)
                .correct(correct)
                .nIndexBefore(nIndexBefore)
                .nIndexAfter(updated.getNIndex())
                .intervalDaysAfter(updated.getIntervalDays())
                .topicMastery(mastery)
                .answeredAt(answeredAt)
                .build());
    }

    /**
     * Distingue o teto adaptativo da saturação clássica em N=13: só o primeiro é uma decisão
     * personalizada e merece explicação na mensagem do estudante.
     */
    private boolean wasIntervalCapped(boolean correct, int nIndexBefore, int nIndexAfter) {
        if (!correct) {
            return false;
        }
        int classicNext = Math.min(nIndexBefore + 1, MmeebbService.MAX_N_INDEX);
        return nIndexAfter < classicNext;
    }

    /**
     * Antecipa questões do tópico errado para a sessão do dia — é a recomendação de conteúdo
     * relacionado ao desempenho individual.
     *
     * <p>O limite se auto-regula: conta quantas questões daquele tópico já estão vencidas e
     * antecipa apenas o que falta para o teto configurado, então errar várias vezes seguidas não
     * transforma a sessão em uma fila infinita.
     */
    private int reinforceWeakTopic(UUID studentId, Long subjectId, String topic,
                                   boolean correct, TopicMastery mastery, LocalDate today) {
        int maxCards = adaptiveProperties.getReinforcementMaxCards();
        if (correct || mastery != TopicMastery.FRAGIL || subjectId == null || maxCards <= 0) {
            return 0;
        }

        long alreadyDue = repetitionScheduleRepository.countDueSchedulesByTopic(studentId, subjectId, topic, today);
        int slots = (int) Math.max(0, maxCards - alreadyDue);
        if (slots == 0) {
            return 0;
        }

        List<RepetitionSchedule> anticipated = repetitionScheduleRepository
                .findFutureSchedulesByTopic(studentId, subjectId, topic, today).stream()
                .limit(slots)
                .toList();

        if (anticipated.isEmpty()) {
            return 0;
        }

        anticipated.forEach(schedule -> schedule.setNextReviewDate(today));
        repetitionScheduleRepository.saveAll(anticipated);
        mmeebbMetrics.recordAdaptiveReinforcement(anticipated.size());
        return anticipated.size();
    }

    private String decisionLabel(boolean intervalCapped, boolean lapseSoftened) {
        if (intervalCapped) {
            return "interval_capped";
        }
        return lapseSoftened ? "lapse_softened" : "classic";
    }

    private Comparator<RepetitionSchedule> doMaisFragilAoMaisDominado(Map<String, TopicPerformanceDto> byTopic) {
        return Comparator
                .comparingInt((RepetitionSchedule schedule) -> masteryOf(schedule, byTopic).reviewPriority())
                .thenComparing(Comparator.comparingDouble(
                        (RepetitionSchedule schedule) -> errorRateOf(schedule, byTopic)).reversed())
                .thenComparing(RepetitionSchedule::getNextReviewDate,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(RepetitionSchedule::getId, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private TopicMastery masteryOf(RepetitionSchedule schedule, Map<String, TopicPerformanceDto> byTopic) {
        TopicPerformanceDto performance = performanceOf(schedule, byTopic);
        return performance != null ? performance.mastery() : TopicMastery.SEM_DADOS;
    }

    private double errorRateOf(RepetitionSchedule schedule, Map<String, TopicPerformanceDto> byTopic) {
        TopicPerformanceDto performance = performanceOf(schedule, byTopic);
        return performance != null ? performance.errorRate() : 0.0;
    }

    private TopicPerformanceDto performanceOf(RepetitionSchedule schedule, Map<String, TopicPerformanceDto> byTopic) {
        Flashcard flashcard = schedule.getFlashcard();
        if (flashcard == null || flashcard.getSubject() == null) {
            return null;
        }
        return byTopic.get(topicKey(flashcard.getSubject().getId(), flashcard.getTopic()));
    }

    private String topicKey(Long subjectId, String topic) {
        return subjectId + "|" + topic;
    }
}
