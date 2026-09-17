package br.edu.unipam.tcc.scheduler;

import br.edu.unipam.tcc.entity.Student;
import br.edu.unipam.tcc.messaging.OutgoingMessagePublisher;
import br.edu.unipam.tcc.observability.CorrelationMdcHelper;
import br.edu.unipam.tcc.repository.RepetitionScheduleRepository;
import br.edu.unipam.tcc.repository.StudentRepository;
import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Enfileira o lembrete diário de revisões do método MMEEBB no horário escolhido por cada
 * estudante nas Configurações. A rotina roda em rodadas curtas (padrão: 5 minutos) e usa a
 * marcação diária do próprio aluno para garantir no máximo um lembrete por dia: um restart
 * não duplica o envio e o lembrete de quem ficou de fora durante uma indisponibilidade é
 * recuperado na primeira rodada seguinte.
 */
@Slf4j
@Component
public class DailyReviewNotificationScheduler {

    private final StudentRepository studentRepository;
    private final RepetitionScheduleRepository repetitionScheduleRepository;
    private final OutgoingMessagePublisher outgoingMessagePublisher;
    private final Tracer tracer;
    private final Clock clock;

    public DailyReviewNotificationScheduler(
            StudentRepository studentRepository,
            RepetitionScheduleRepository repetitionScheduleRepository,
            OutgoingMessagePublisher outgoingMessagePublisher,
            Tracer tracer,
            Clock clock
    ) {
        this.studentRepository = studentRepository;
        this.repetitionScheduleRepository = repetitionScheduleRepository;
        this.outgoingMessagePublisher = outgoingMessagePublisher;
        this.tracer = tracer;
        this.clock = clock;
    }

    /**
     * Consulta os estudantes cujo horário de lembrete já chegou e que ainda não foram
     * avaliados hoje, quantifica as pendências e publica na fila de saída anti-ban.
     */
    @Scheduled(cron = "${mmeebb.scheduler.cron:0 */5 * * * *}", zone = "America/Sao_Paulo")
    public void sendDailyReviewNotifications() {
        CorrelationMdcHelper.ensureTraceId(tracer);

        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);
        LocalDate today = now.toLocalDate();

        List<Student> dueStudents = studentRepository.findDueForReviewNotification(now.toLocalTime(), today);
        if (dueStudents.isEmpty()) {
            log.debug("[ReviewScheduler] Nenhum estudante com lembrete devido às {}.", now.toLocalTime());
            return;
        }

        log.info("[ReviewScheduler] {} estudante(s) com lembrete devido às {}.", dueStudents.size(), now.toLocalTime());
        int notificationsSent = 0;

        for (Student student : dueStudents) {
            int[] sentInThisIteration = {0};
            CorrelationMdcHelper.runWithContext(student.getPhoneNumber(), "DAILY_SCHEDULER", () -> {
                try {
                    long pendingCount = repetitionScheduleRepository
                            .countByStudentIdAndNextReviewDateLessThanEqualAndIsActiveTrue(student.getId(), today);

                    if (pendingCount > 0) {
                        outgoingMessagePublisher.publish(
                                student.getPhoneNumber(),
                                buildNotificationMessage(student.displayName(), pendingCount));

                        sentInThisIteration[0] = 1;
                        log.info("[ReviewScheduler] Lembrete enfileirado para [{}] ({} pendências)",
                                student.getPhoneNumber(), pendingCount);
                    } else {
                        log.debug("[ReviewScheduler] Nenhuma revisão pendente para o estudante [{}] hoje.",
                                student.getPhoneNumber());
                    }

                    // Pendência depende só da data: avaliado hoje, só volta a ser avaliado amanhã.
                    student.setLastReviewNotificationOn(today);
                    studentRepository.save(student);
                } catch (Exception e) {
                    log.error("[ReviewScheduler] Falha ao processar o lembrete do estudante [{}]; "
                                    + "nova tentativa na próxima rodada: {}",
                            student.getPhoneNumber(), e.getMessage(), e);
                }
            });
            notificationsSent += sentInThisIteration[0];
        }

        log.info("[ReviewScheduler] Rodada concluída: {} lembrete(s) enfileirado(s) de {} estudante(s) devido(s).",
                notificationsSent, dueStudents.size());
    }

    /**
     * Monta a mensagem formatada para o WhatsApp com negrito, emojis e indicação clara de ação.
     */
    private String buildNotificationMessage(String studentName, long pendingCount) {
        return String.format(
                "Olá, *%s*! 👋\n\n" +
                "Você tem *%d* revisão(ões) pendente(s) do método MMEEBB para hoje no seu internato/curso.\n\n" +
                "Envie *revisar* para iniciar sua sessão de memorização espaçada! 🚀",
                studentName, pendingCount
        );
    }
}
