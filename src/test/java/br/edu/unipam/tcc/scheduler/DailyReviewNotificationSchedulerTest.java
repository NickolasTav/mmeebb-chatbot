package br.edu.unipam.tcc.scheduler;

import br.edu.unipam.tcc.config.RabbitMQConfig;
import br.edu.unipam.tcc.dto.OutgoingMessageDto;
import br.edu.unipam.tcc.entity.Student;
import br.edu.unipam.tcc.repository.RepetitionScheduleRepository;
import br.edu.unipam.tcc.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyReviewNotificationSchedulerTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private RepetitionScheduleRepository repetitionScheduleRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private DailyReviewNotificationScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new DailyReviewNotificationScheduler(
                studentRepository,
                repetitionScheduleRepository,
                rabbitTemplate
        );
    }

    @Test
    @DisplayName("Smoke Test: Deve executar ciclo completo de agendamento diário com sucesso")
    void deveExecutarCicloDeAgendamentoDiarioComSucesso() {
        Student student = Student.builder()
                .id(UUID.randomUUID())
                .fullName("Dr. João Silva")
                .phoneNumber("5534999991111")
                .active(true)
                .build();

        when(studentRepository.findByActiveTrue()).thenReturn(List.of(student));
        when(repetitionScheduleRepository.countByStudentIdAndNextReviewDateLessThanEqualAndIsActiveTrue(eq(student.getId()), any(LocalDate.class)))
                .thenReturn(3L);

        scheduler.sendDailyReviewNotifications();

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.OUTGOING_ROUTING_KEY),
                any(OutgoingMessageDto.class)
        );
    }

    @Test
    @DisplayName("Deve enfileirar mensagem formatada para estudante com pendências MMEEBB")
    void deveEnfileirarMensagemFormatadaParaEstudanteComPendencias() {
        UUID studentId = UUID.randomUUID();
        Student student = Student.builder()
                .id(studentId)
                .fullName("Maria Souza")
                .phoneNumber("5534988882222")
                .active(true)
                .build();

        when(studentRepository.findByActiveTrue()).thenReturn(List.of(student));
        when(repetitionScheduleRepository.countByStudentIdAndNextReviewDateLessThanEqualAndIsActiveTrue(eq(studentId), any(LocalDate.class)))
                .thenReturn(5L);

        scheduler.sendDailyReviewNotifications();

        ArgumentCaptor<OutgoingMessageDto> messageCaptor = ArgumentCaptor.forClass(OutgoingMessageDto.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.OUTGOING_ROUTING_KEY),
                messageCaptor.capture()
        );

        OutgoingMessageDto captured = messageCaptor.getValue();
        assertEquals("5534988882222", captured.phoneNumber());
        assertTrue(captured.messageText().contains("Maria"));
        assertTrue(captured.messageText().contains("5"));
        assertTrue(captured.messageText().contains("MMEEBB"));
        assertTrue(captured.messageText().contains("*revisar*"));
    }

    @Test
    @DisplayName("Não deve enfileirar notificação quando estudante não possui revisões pendentes")
    void naoDeveEnfileirarNotificacaoQuandoEstudanteNaoPossuiPendencias() {
        UUID studentId = UUID.randomUUID();
        Student student = Student.builder()
                .id(studentId)
                .fullName("Carlos Pereira")
                .phoneNumber("5534977773333")
                .active(true)
                .build();

        when(studentRepository.findByActiveTrue()).thenReturn(List.of(student));
        when(repetitionScheduleRepository.countByStudentIdAndNextReviewDateLessThanEqualAndIsActiveTrue(eq(studentId), any(LocalDate.class)))
                .thenReturn(0L);

        scheduler.sendDailyReviewNotifications();

        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    @DisplayName("Deve processar múltiplos estudantes e notificar apenas os que possuem pendências")
    void deveProcessarMultiplosEstudantesNotificandoApenasPendentes() {
        Student student1 = Student.builder()
                .id(UUID.randomUUID())
                .fullName("Aluno Com Revisao")
                .phoneNumber("5534911110001")
                .active(true)
                .build();

        Student student2 = Student.builder()
                .id(UUID.randomUUID())
                .fullName("Aluno Sem Revisao")
                .phoneNumber("5534911110002")
                .active(true)
                .build();

        Student student3 = Student.builder()
                .id(UUID.randomUUID())
                .fullName("Aluno Outra Revisao")
                .phoneNumber("5534911110003")
                .active(true)
                .build();

        when(studentRepository.findByActiveTrue()).thenReturn(List.of(student1, student2, student3));
        when(repetitionScheduleRepository.countByStudentIdAndNextReviewDateLessThanEqualAndIsActiveTrue(eq(student1.getId()), any(LocalDate.class)))
                .thenReturn(2L);
        when(repetitionScheduleRepository.countByStudentIdAndNextReviewDateLessThanEqualAndIsActiveTrue(eq(student2.getId()), any(LocalDate.class)))
                .thenReturn(0L);
        when(repetitionScheduleRepository.countByStudentIdAndNextReviewDateLessThanEqualAndIsActiveTrue(eq(student3.getId()), any(LocalDate.class)))
                .thenReturn(7L);

        scheduler.sendDailyReviewNotifications();

        verify(rabbitTemplate, times(2)).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.OUTGOING_ROUTING_KEY),
                any(OutgoingMessageDto.class)
        );
    }

    @Test
    @DisplayName("Deve isolar falhas de envio por estudante sem interromper o loop dos demais")
    void deveIsolarFalhasPorEstudanteSemInterromperOutros() {
        Student studentFail = Student.builder()
                .id(UUID.randomUUID())
                .fullName("Aluno Falha")
                .phoneNumber("5534999990001")
                .active(true)
                .build();

        Student studentOk = Student.builder()
                .id(UUID.randomUUID())
                .fullName("Aluno Sucesso")
                .phoneNumber("5534999990002")
                .active(true)
                .build();

        when(studentRepository.findByActiveTrue()).thenReturn(List.of(studentFail, studentOk));
        when(repetitionScheduleRepository.countByStudentIdAndNextReviewDateLessThanEqualAndIsActiveTrue(eq(studentFail.getId()), any(LocalDate.class)))
                .thenThrow(new RuntimeException("Falha temporária de banco"));
        when(repetitionScheduleRepository.countByStudentIdAndNextReviewDateLessThanEqualAndIsActiveTrue(eq(studentOk.getId()), any(LocalDate.class)))
                .thenReturn(1L);

        assertDoesNotThrow(() -> scheduler.sendDailyReviewNotifications());

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.OUTGOING_ROUTING_KEY),
                any(OutgoingMessageDto.class)
        );
    }
}
