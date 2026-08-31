package br.edu.unipam.tcc.scheduler;

import br.edu.unipam.tcc.config.RabbitMQConfig;
import br.edu.unipam.tcc.dto.OutgoingMessageDto;
import br.edu.unipam.tcc.entity.Student;
import br.edu.unipam.tcc.repository.RepetitionScheduleRepository;
import br.edu.unipam.tcc.repository.StudentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Agendador diário responsável por identificar estudantes com revisões pendentes
 * no método MMEEBB e enfileirar notificações ativas (push) na fila do RabbitMQ.
 */
@Slf4j
@Component
public class DailyReviewNotificationScheduler {

    private final StudentRepository studentRepository;
    private final RepetitionScheduleRepository repetitionScheduleRepository;
    private final RabbitTemplate rabbitTemplate;

    public DailyReviewNotificationScheduler(
            StudentRepository studentRepository,
            RepetitionScheduleRepository repetitionScheduleRepository,
            RabbitTemplate rabbitTemplate
    ) {
        this.studentRepository = studentRepository;
        this.repetitionScheduleRepository = repetitionScheduleRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Executa diariamente no horário configurado (padrão: 08:00 no fuso de São Paulo).
     * Consulta alunos ativos, quantifica cards pendentes/vencidos e publica na fila de saída.
     */
    @Scheduled(cron = "${mmeebb.scheduler.cron:0 0 8 * * *}", zone = "America/Sao_Paulo")
    public void sendDailyReviewNotifications() {
        log.info("[DailyScheduler] Iniciando rotina diária de notificações ativas do método MMEEBB.");

        List<Student> activeStudents = studentRepository.findByActiveTrue();
        if (activeStudents == null || activeStudents.isEmpty()) {
            log.info("[DailyScheduler] Nenhum estudante ativo encontrado para notificações diárias.");
            return;
        }

        LocalDate today = LocalDate.now();
        int notificationsSent = 0;

        for (Student student : activeStudents) {
            try {
                long pendingCount = repetitionScheduleRepository
                        .countByStudentIdAndNextReviewDateLessThanEqualAndIsActiveTrue(student.getId(), today);

                if (pendingCount > 0) {
                    String studentName = student.getFullName() != null && !student.getFullName().isBlank()
                            ? student.getFullName().trim()
                            : "Estudante";

                    String messageText = buildNotificationMessage(studentName, pendingCount);
                    OutgoingMessageDto outgoingDto = new OutgoingMessageDto(student.getPhoneNumber(), messageText);

                    rabbitTemplate.convertAndSend(
                            RabbitMQConfig.EXCHANGE_NAME,
                            RabbitMQConfig.OUTGOING_ROUTING_KEY,
                            outgoingDto
                    );

                    notificationsSent++;
                    log.info("[DailyScheduler] Notificação push enfileirada para [{}] ({} pendências)",
                            student.getPhoneNumber(), pendingCount);
                } else {
                    log.debug("[DailyScheduler] Nenhuma revisão pendente para estudante [{}] hoje.",
                            student.getPhoneNumber());
                }
            } catch (Exception e) {
                log.error("[DailyScheduler] Falha ao processar notificação diária para o estudante [{}]: {}",
                        student.getPhoneNumber(), e.getMessage(), e);
            }
        }

        log.info("[DailyScheduler] Rotina diária concluída. Total de notificações enfileiradas: {} de {} estudantes ativos.",
                notificationsSent, activeStudents.size());
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
