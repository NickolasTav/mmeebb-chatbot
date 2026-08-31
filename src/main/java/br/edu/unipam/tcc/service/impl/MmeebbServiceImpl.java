package br.edu.unipam.tcc.service.impl;

import br.edu.unipam.tcc.entity.Flashcard;
import br.edu.unipam.tcc.entity.RepetitionSchedule;
import br.edu.unipam.tcc.entity.Student;
import br.edu.unipam.tcc.entity.enums.ScheduleStatus;
import br.edu.unipam.tcc.service.MmeebbService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class MmeebbServiceImpl implements MmeebbService {

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
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule não pode ser nulo");
        }
        if (answeredAt == null) {
            throw new IllegalArgumentException("Timestamp de resposta (answeredAt) não pode ser nulo");
        }

        schedule.setRepetitionCount(schedule.getRepetitionCount() + 1);
        schedule.setLastReviewedAt(answeredAt);
        schedule.setStatus(ScheduleStatus.COMPLETED);

        if (isCorrect) {
            int newN = Math.min(schedule.getNIndex() + 1, MAX_N_INDEX);
            schedule.setNIndex(newN);
            schedule.setIntervalDays(calculateIntervalDays(newN));
            schedule.setConsecutiveCorrect(schedule.getConsecutiveCorrect() + 1);
        } else {
            schedule.setNIndex(0);
            schedule.setIntervalDays(1);
            schedule.setConsecutiveCorrect(0);
        }

        LocalDate nextDate = calculateNextReviewDate(answeredAt.toLocalDate(), schedule.getNIndex());
        schedule.setNextReviewDate(nextDate);

        return schedule;
    }
}
