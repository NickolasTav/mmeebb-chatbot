package br.edu.unipam.tcc.scheduler;

import br.edu.unipam.tcc.dto.DailyReviewPlanDto;
import br.edu.unipam.tcc.entity.Student;
import br.edu.unipam.tcc.messaging.OutgoingMessagePublisher;
import br.edu.unipam.tcc.repository.StudentRepository;
import br.edu.unipam.tcc.service.DailyReviewQueueService;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyReviewNotificationSchedulerTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 17);

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private DailyReviewQueueService dailyReviewQueueService;

    @Mock
    private OutgoingMessagePublisher outgoingMessagePublisher;

    @Mock
    private Tracer tracer;

    private DailyReviewNotificationScheduler schedulerAt(LocalTime time) {
        Clock clock = Clock.fixed(LocalDateTime.of(TODAY, time).atZone(ZONE).toInstant(), ZONE);
        return new DailyReviewNotificationScheduler(
                studentRepository, dailyReviewQueueService, outgoingMessagePublisher, tracer, clock);
    }

    private static DailyReviewPlanDto plan(int reviews, int newQuestions, long beyondLimit) {
        return new DailyReviewPlanDto(List.of(), reviews, newQuestions, 20, 0, beyondLimit);
    }

    private Student student(String fullName, String phone) {
        return Student.builder()
                .id(UUID.randomUUID())
                .fullName(fullName)
                .phoneNumber(phone)
                .preferredStudyTime(LocalTime.of(12, 15))
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Deve consultar alunos devidos com o minuto da rodada (sem segundos) e a data de São Paulo")
    void deveConsultarAlunosDevidosComMinutoTruncado() {
        when(studentRepository.findDueForReviewNotification(any(), any())).thenReturn(List.of());

        schedulerAt(LocalTime.of(12, 20, 0, 350_000_000)).sendDailyReviewNotifications();

        verify(studentRepository).findDueForReviewNotification(LocalTime.of(12, 20), TODAY);
    }

    @Test
    @DisplayName("Não deve contar pendências nem publicar quando nenhum aluno estiver devido")
    void naoDeveFazerNadaQuandoNenhumAlunoEstiverDevido() {
        when(studentRepository.findDueForReviewNotification(any(), any())).thenReturn(List.of());

        schedulerAt(LocalTime.of(12, 20)).sendDailyReviewNotifications();

        verifyNoInteractions(dailyReviewQueueService, outgoingMessagePublisher);
    }

    @Test
    @DisplayName("Deve enviar lembrete com o nome de exibição e marcar o dia como avaliado")
    void deveEnviarLembreteComNomeDeExibicaoEMarcarODia() {
        Student maria = student("Maria Souza", "5534988882222");
        maria.setPreferredName("Mari");
        when(studentRepository.findDueForReviewNotification(any(), any())).thenReturn(List.of(maria));
        when(dailyReviewQueueService.summarizeToday(maria)).thenReturn(plan(5, 0, 0));

        schedulerAt(LocalTime.of(12, 20)).sendDailyReviewNotifications();

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(outgoingMessagePublisher).publish(eq("5534988882222"), message.capture());
        assertTrue(message.getValue().contains("*Mari*"));
        assertTrue(message.getValue().contains("*5*"));
        assertTrue(message.getValue().contains("MMEEBB"));
        assertTrue(message.getValue().contains("*revisar*"));

        verify(studentRepository).save(maria);
        assertEquals(TODAY, maria.getLastReviewNotificationOn());
    }

    @Test
    @DisplayName("Deve marcar o dia como avaliado mesmo sem pendências, sem publicar mensagem")
    void deveMarcarODiaMesmoSemPendencias() {
        Student carlos = student("Carlos Pereira", "5534977773333");
        when(studentRepository.findDueForReviewNotification(any(), any())).thenReturn(List.of(carlos));
        when(dailyReviewQueueService.summarizeToday(carlos)).thenReturn(plan(0, 0, 0));

        schedulerAt(LocalTime.of(12, 20)).sendDailyReviewNotifications();

        verifyNoInteractions(outgoingMessagePublisher);
        verify(studentRepository).save(carlos);
        assertEquals(TODAY, carlos.getLastReviewNotificationOn());
    }

    @Test
    @DisplayName("Não deve marcar o dia quando a publicação falhar, para tentar de novo na próxima rodada")
    void naoDeveMarcarODiaQuandoAPublicacaoFalhar() {
        Student joao = student("João Silva", "5534999991111");
        when(studentRepository.findDueForReviewNotification(any(), any())).thenReturn(List.of(joao));
        when(dailyReviewQueueService.summarizeToday(joao)).thenReturn(plan(3, 0, 0));
        doThrow(new RuntimeException("RabbitMQ indisponível"))
                .when(outgoingMessagePublisher).publish(anyString(), anyString());

        assertDoesNotThrow(() -> schedulerAt(LocalTime.of(12, 20)).sendDailyReviewNotifications());

        verify(studentRepository, never()).save(any());
        assertNull(joao.getLastReviewNotificationOn());
    }

    @Test
    @DisplayName("Deve isolar falhas por estudante sem interromper os demais")
    void deveIsolarFalhasPorEstudante() {
        Student falha = student("Aluno Falha", "5534999990001");
        Student ok = student("Aluno Sucesso", "5534999990002");
        when(studentRepository.findDueForReviewNotification(any(), any())).thenReturn(List.of(falha, ok));
        when(dailyReviewQueueService.summarizeToday(falha))
                .thenThrow(new RuntimeException("Falha temporária de banco"));
        when(dailyReviewQueueService.summarizeToday(ok)).thenReturn(plan(1, 0, 0));

        assertDoesNotThrow(() -> schedulerAt(LocalTime.of(12, 20)).sendDailyReviewNotifications());

        verify(outgoingMessagePublisher, times(1)).publish(eq("5534999990002"), anyString());
        verify(studentRepository, never()).save(falha);
        verify(studentRepository).save(ok);
    }

    @Test
    @DisplayName("Lembrete deve anunciar só o que cabe no limite, detalhando revisões e novas")
    void lembreteDeveDetalharRevisoesENovasDentroDoLimite() {
        Student ana = student("Ana Lima", "5534966664444");
        when(studentRepository.findDueForReviewNotification(any(), any())).thenReturn(List.of(ana));
        when(dailyReviewQueueService.summarizeToday(ana)).thenReturn(plan(12, 8, 361));

        schedulerAt(LocalTime.of(12, 20)).sendDailyReviewNotifications();

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(outgoingMessagePublisher).publish(eq("5534966664444"), message.capture());
        assertTrue(message.getValue().contains("📚 *Revisão MMEEBB* · faltam *20* hoje (12 revisões + 8 novas)"),
                message.getValue());
        assertFalse(message.getValue().contains("381"), message.getValue());
    }

    @Test
    @DisplayName("Não deve lembrar quem já cumpriu a meta do dia, mesmo com questões além do limite")
    void naoDeveLembrarQuemJaCumpriuAMeta() {
        Student bia = student("Bia Rocha", "5534955556666");
        when(studentRepository.findDueForReviewNotification(any(), any())).thenReturn(List.of(bia));
        when(dailyReviewQueueService.summarizeToday(bia)).thenReturn(plan(0, 0, 361));

        schedulerAt(LocalTime.of(12, 20)).sendDailyReviewNotifications();

        verifyNoInteractions(outgoingMessagePublisher);
        // Avaliado hoje: continua no máximo um lembrete por dia.
        verify(studentRepository).save(bia);
        assertEquals(TODAY, bia.getLastReviewNotificationOn());
    }
}
