package br.edu.unipam.tcc.service.impl;

import br.edu.unipam.tcc.config.DailyLimitProperties;
import br.edu.unipam.tcc.dto.DailyReviewPlanDto;
import br.edu.unipam.tcc.entity.RepetitionSchedule;
import br.edu.unipam.tcc.entity.Student;
import br.edu.unipam.tcc.entity.enums.ScheduleStatus;
import br.edu.unipam.tcc.repository.RepetitionScheduleRepository;
import br.edu.unipam.tcc.repository.ReviewAttemptRepository;
import br.edu.unipam.tcc.service.AdaptiveReviewService;
import br.edu.unipam.tcc.service.DailyReviewQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DailyReviewQueueServiceImpl implements DailyReviewQueueService {

    private final RepetitionScheduleRepository repetitionScheduleRepository;
    private final ReviewAttemptRepository reviewAttemptRepository;
    private final AdaptiveReviewService adaptiveReviewService;
    private final DailyLimitProperties dailyLimitProperties;
    private final Clock clock;

    @Override
    public DailyReviewPlanDto planToday(Student student) {
        LocalDate today = LocalDate.now(clock);
        DayBudget budget = budgetFor(student, today);

        List<RepetitionSchedule> due = adaptiveReviewService.prioritize(student.getId(),
                repetitionScheduleRepository.findPendingReviewsByStudent(student.getId(), today, ScheduleStatus.PENDING));

        // Partições estáveis: cada grupo mantém a ordem decidida pela camada adaptativa.
        List<RepetitionSchedule> dueReviews = due.stream().filter(DailyReviewQueueServiceImpl::alreadyAnswered).toList();
        List<RepetitionSchedule> dueNew = due.stream().filter(schedule -> !alreadyAnswered(schedule)).toList();
        Allocation allocation = budget.allocate(dueReviews.size(), dueNew.size());

        List<RepetitionSchedule> queue = new ArrayList<>(dueReviews.subList(0, allocation.reviews()));
        queue.addAll(dueNew.subList(0, allocation.newQuestions()));

        return new DailyReviewPlanDto(List.copyOf(queue), allocation.reviews(), allocation.newQuestions(),
                budget.limit(), budget.answeredToday(), due.size() - queue.size());
    }

    @Override
    public DailyReviewPlanDto summarizeToday(Student student) {
        LocalDate today = LocalDate.now(clock);
        DayBudget budget = budgetFor(student, today);

        long dueReviews = repetitionScheduleRepository
                .countDueReviewsByStudent(student.getId(), today, ScheduleStatus.PENDING);
        long dueNew = repetitionScheduleRepository
                .countDueNewQuestionsByStudent(student.getId(), today, ScheduleStatus.PENDING);
        Allocation allocation = budget.allocate(dueReviews, dueNew);

        return new DailyReviewPlanDto(List.of(), allocation.reviews(), allocation.newQuestions(),
                budget.limit(), budget.answeredToday(), dueReviews + dueNew - allocation.total());
    }

    /** O dia é o de São Paulo (o {@link Clock} da aplicação), o mesmo em que a tentativa é gravada. */
    private DayBudget budgetFor(Student student, LocalDate day) {
        UUID studentId = student.getId();
        LocalDateTime from = day.atStartOfDay();
        LocalDateTime to = day.plusDays(1).atStartOfDay();
        int limit = dailyLimitProperties.resolve(student.getDailyQuestionLimit());

        return new DayBudget(limit,
                reviewAttemptRepository.countAnsweredBetween(studentId, from, to),
                reviewAttemptRepository.countFirstAnswersBetween(studentId, from, to),
                dailyLimitProperties.reservedNewQuestions(limit));
    }

    /** Revisão é a questão já respondida ao menos uma vez; nova é a que nunca foi. */
    private static boolean alreadyAnswered(RepetitionSchedule schedule) {
        return schedule.getLastReviewedAt() != null;
    }

    private record Allocation(int reviews, int newQuestions) {
        int total() {
            return reviews + newQuestions;
        }
    }

    /**
     * O que resta do limite hoje e como ele se divide. As revisões vencidas têm prioridade, exceto
     * sobre as vagas reservadas a questões novas que ainda não foram usadas hoje; vaga reservada
     * sem questão nova disponível volta para as revisões, e o que as revisões não ocuparem também
     * vai para as novas.
     */
    private record DayBudget(int limit, long answeredToday, long newAnsweredToday, int reservedNew) {

        Allocation allocate(long dueReviews, long dueNew) {
            long remaining = Math.max(0, limit - answeredToday);
            long reservedLeft = Math.max(0, reservedNew - newAnsweredToday);
            long keptForNew = Math.min(Math.min(reservedLeft, dueNew), remaining);

            int reviews = (int) Math.min(dueReviews, remaining - keptForNew);
            int newQuestions = (int) Math.min(dueNew, remaining - reviews);
            return new Allocation(reviews, newQuestions);
        }
    }
}
