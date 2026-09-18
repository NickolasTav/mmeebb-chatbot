package br.edu.unipam.tcc.flow;

import br.edu.unipam.tcc.dto.StudentPerformanceReportDto;
import br.edu.unipam.tcc.dto.TopicPerformanceDto;
import br.edu.unipam.tcc.entity.Student;
import br.edu.unipam.tcc.entity.enums.ChatState;
import br.edu.unipam.tcc.entity.enums.TopicMastery;
import br.edu.unipam.tcc.messaging.OutgoingMessagePublisher;
import br.edu.unipam.tcc.service.PerformanceAnalysisService;
import br.edu.unipam.tcc.service.PerformanceDiagnosisService;
import br.edu.unipam.tcc.session.ChatSessionState;
import br.edu.unipam.tcc.session.ChatSessionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentPerformanceFlowHandlerTest {

    private static final String PHONE = "5534999998888";
    private static final UUID ALUNO = UUID.randomUUID();

    private PerformanceAnalysisService performanceAnalysisService;
    private PerformanceDiagnosisService performanceDiagnosisService;
    private OutgoingMessagePublisher outgoingMessagePublisher;
    private ChatSessionStore chatSessionStore;
    private StudentPerformanceFlowHandler handler;

    private Student student;

    @BeforeEach
    void setUp() {
        performanceAnalysisService = mock(PerformanceAnalysisService.class);
        performanceDiagnosisService = mock(PerformanceDiagnosisService.class);
        outgoingMessagePublisher = mock(OutgoingMessagePublisher.class);
        chatSessionStore = mock(ChatSessionStore.class);
        handler = new StudentPerformanceFlowHandler(performanceAnalysisService, performanceDiagnosisService,
                outgoingMessagePublisher, chatSessionStore);

        student = Student.builder().phoneNumber(PHONE).fullName("Maria Silva").preferredName("Mari").build();
        student.setId(ALUNO);
    }

    private ChatSessionState session(ChatState state) {
        return ChatSessionState.builder().phoneNumber(PHONE).studentId(ALUNO).currentState(state).build();
    }

    private TopicPerformanceDto topic(String name, double errorRate, TopicMastery mastery) {
        return new TopicPerformanceDto(4L, "Farmacologia Clinica", name,
                10L, Math.round(errorRate * 10), errorRate, mastery);
    }

    private String mensagemEnviada() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(outgoingMessagePublisher).publish(eq(PHONE), captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("Smoke Test: deve enviar taxa de acerto, pontos frágeis e conteúdos consolidados")
    void deveEnviarOResumoDeDesempenho() {
        when(performanceAnalysisService.buildStudentReport(student)).thenReturn(
                new StudentPerformanceReportDto(ALUNO, "Mari", 90, 48L, 31L, 0.6458, true, List.of(
                        topic("Antibioticoterapia", 0.7, TopicMastery.FRAGIL),
                        topic("Arritmias", 0.3, TopicMastery.EM_CONSOLIDACAO),
                        topic("Anti-hipertensivos", 0.1, TopicMastery.DOMINADO))));
        when(performanceDiagnosisService.diagnose(any())).thenReturn(Optional.empty());

        handler.send(session(ChatState.MAIN_MENU), student);

        String mensagem = mensagemEnviada();
        assertThat(mensagem).contains("Mari")
                .contains("90 dias")
                .contains("*48*")
                .contains("65%")
                .contains("Antibioticoterapia")
                .contains("70% de erro")
                .contains("8 dias")
                .contains("Anti-hipertensivos");
    }

    @Test
    @DisplayName("A mensagem não pode conter CRLF: o WhatsApp espera apenas \\n como quebra de linha")
    void mensagemNaoPodeConterQuebraDeLinhaDoWindows() {
        when(performanceAnalysisService.buildStudentReport(student)).thenReturn(
                new StudentPerformanceReportDto(ALUNO, "Mari", 90, 48L, 31L, 0.6458, true, List.of(
                        topic("Antibioticoterapia", 0.8, TopicMastery.FRAGIL),
                        topic("Arritmias", 0.6, TopicMastery.FRAGIL),
                        topic("Anti-hipertensivos", 0.1, TopicMastery.DOMINADO),
                        topic("Semiologia", 0.05, TopicMastery.DOMINADO))));
        when(performanceDiagnosisService.diagnose(any())).thenReturn(Optional.empty());

        handler.send(session(ChatState.MAIN_MENU), student);

        assertThat(mensagemEnviada()).doesNotContain("\r");
    }

    @Test
    @DisplayName("Deve anexar o diagnóstico do preceptor virtual quando a IA responder")
    void deveAnexarODiagnosticoDaIa() {
        when(performanceAnalysisService.buildStudentReport(student)).thenReturn(
                new StudentPerformanceReportDto(ALUNO, "Mari", 90, 48L, 31L, 0.6458, true,
                        List.of(topic("Antibioticoterapia", 0.7, TopicMastery.FRAGIL))));
        when(performanceDiagnosisService.diagnose(any()))
                .thenReturn(Optional.of("Reserve 10 minutos por dia para antibioticoterapia."));

        handler.send(session(ChatState.MAIN_MENU), student);

        assertThat(mensagemEnviada()).contains("Reserve 10 minutos por dia para antibioticoterapia.");
    }

    @Test
    @DisplayName("Sem amostra suficiente, deve convidar a revisar em vez de exibir percentuais vazios")
    void semAmostraDeveConvidarARevisar() {
        when(performanceAnalysisService.buildStudentReport(student)).thenReturn(
                new StudentPerformanceReportDto(ALUNO, "Mari", 90, 2L, 1L, 0.5, false, List.of()));

        handler.send(session(ChatState.MAIN_MENU), student);

        String mensagem = mensagemEnviada();
        assertThat(mensagem).contains("Ainda não tenho respostas suficientes").contains("revisar");
        assertThat(mensagem).doesNotContain("50%");
        verify(performanceDiagnosisService, never()).diagnose(any());
    }

    @Test
    @DisplayName("Deve devolver o aluno ao menu principal e limpar a questão em aberto")
    void deveDevolverAoMenuPrincipal() {
        when(performanceAnalysisService.buildStudentReport(student)).thenReturn(
                new StudentPerformanceReportDto(ALUNO, "Mari", 90, 0L, 0L, 0.0, false, List.of()));

        ChatSessionState session = session(ChatState.REVIEW_MODE);
        session.setCurrentFlashcardId(100L);

        handler.send(session, student);

        assertThat(session.getCurrentState()).isEqualTo(ChatState.MAIN_MENU);
        assertThat(session.getCurrentFlashcardId()).isNull();
        verify(chatSessionStore).save(session);
    }
}
