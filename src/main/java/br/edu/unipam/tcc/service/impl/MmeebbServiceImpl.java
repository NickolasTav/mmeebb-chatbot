package br.edu.unipam.tcc.service.impl;

import br.edu.unipam.tcc.entity.Flashcard;
import br.edu.unipam.tcc.entity.RepetitionSchedule;
import br.edu.unipam.tcc.entity.Student;
import br.edu.unipam.tcc.entity.enums.ScheduleStatus;
import br.edu.unipam.tcc.entity.enums.TopicMastery;
import br.edu.unipam.tcc.observability.MmeebbMetrics;
import br.edu.unipam.tcc.service.MmeebbService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class MmeebbServiceImpl implements MmeebbService {

    /** Casas que o expoente recua quando o erro acontece em um tópico já dominado. */
    private static final int LAPSE_STEP_BACK = 2;

    private final MmeebbMetrics mmeebbMetrics;

    public MmeebbServiceImpl(MmeebbMetrics mmeebbMetrics) {
        this.mmeebbMetrics = mmeebbMetrics;
    }

    @Override
    public int calculateIntervalDays(int nIndex) {
        if (nIndex < 0) {
            throw new IllegalArgumentException("Índice n não pode ser negativo: " + nIndex);
        }
        int clampedN = Math.min(nIndex, MAX_N_INDEX);
        return 1 << clampedN;
    }

    @Override
    public LocalDate calculateNextReviewDate(LocalDate baseDate, int nIndex) {
        if (baseDate == null) {
            throw new IllegalArgumentException("Data de referência não pode ser nula");
        }
        int intervalDays = calculateIntervalDays(nIndex);
        return baseDate.plusDays(intervalDays);
    }

    @Override
    public RepetitionSchedule initializeSchedule(Student student, Flashcard flashcard, LocalDate startDate) {
        if (student == null || flashcard == null) {
            throw new IllegalArgumentException("Estudante e Flashcard são obrigatórios para inicializar agendamento");
        }
        LocalDate baseDate = startDate != null ? startDate : LocalDate.now();
        return RepetitionSchedule.builder()
                .student(student)
                .flashcard(flashcard)
                .nIndex(0)
                .intervalDays(1)
                .repetitionCount(0)
                .consecutiveCorrect(0)
                .nextReviewDate(baseDate.plusDays(1))
                .status(ScheduleStatus.PENDING)
                .build();
    }

    @Override
    public RepetitionSchedule processAnswer(RepetitionSchedule schedule, boolean isCorrect, LocalDateTime answeredAt) {
        return processAnswer(schedule, isCorrect, answeredAt, TopicMastery.SEM_DADOS);
    }

    @Override
    public RepetitionSchedule processAnswer(RepetitionSchedule schedule, boolean isCorrect,
                                            LocalDateTime answeredAt, TopicMastery mastery) {
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule não pode ser nulo");
        }
        if (answeredAt == null) {
            throw new IllegalArgumentException("Timestamp de resposta (answeredAt) não pode ser nulo");
        }

        TopicMastery topicMastery = mastery != null ? mastery : TopicMastery.SEM_DADOS;

        schedule.setRepetitionCount(schedule.getRepetitionCount() + 1);
        schedule.setLastReviewedAt(answeredAt);
        // O agendamento permanece PENDING: a repetição espaçada é cíclica e o card
        // volta a ser elegível quando nextReviewDate chegar. COMPLETED encerraria o ciclo.
        schedule.setStatus(ScheduleStatus.PENDING);

        int newN = nextNIndex(schedule.getNIndex(), isCorrect, topicMastery);
        schedule.setNIndex(newN);
        schedule.setIntervalDays(calculateIntervalDays(newN));
        schedule.setConsecutiveCorrect(isCorrect ? schedule.getConsecutiveCorrect() + 1 : 0);

        LocalDate nextDate = calculateNextReviewDate(answeredAt.toLocalDate(), newN);
        schedule.setNextReviewDate(nextDate);

        String specialty = schedule.getFlashcard() != null && schedule.getFlashcard().getSubject() != null
                ? schedule.getFlashcard().getSubject().getName()
                : null;
        mmeebbMetrics.recordReview(isCorrect, specialty);

        return schedule;
    }

    /**
     * Fórmula única do MMEEBB adaptativo. O teto do domínio é o que personaliza a revisão:
     * enquanto o tópico for frágil, o expoente não cresce além de 3 e o cartão permanece no ciclo
     * curto por mais que o aluno acerte. O recuo de duas casas vale só para tópico dominado, onde
     * o erro é um lapso isolado e zerar meses de consolidação seria punitivo demais.
     */
    private int nextNIndex(int currentN, boolean isCorrect, TopicMastery mastery) {
        int candidate;
        if (isCorrect) {
            candidate = currentN + 1;
        } else if (mastery == TopicMastery.DOMINADO) {
            candidate = currentN - LAPSE_STEP_BACK;
        } else {
            candidate = 0;
        }

        int ceiling = Math.min(MAX_N_INDEX, mastery.maxNIndex());
        return Math.min(Math.max(candidate, 0), ceiling);
    }
}
