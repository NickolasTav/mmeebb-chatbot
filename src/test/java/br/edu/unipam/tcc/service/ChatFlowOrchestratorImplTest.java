package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.dto.AnswerEvaluationDto;
import br.edu.unipam.tcc.dto.IntentResultDto;
import br.edu.unipam.tcc.dto.UazapiWebhookDto;
import br.edu.unipam.tcc.entity.Course;
import br.edu.unipam.tcc.entity.Flashcard;
import br.edu.unipam.tcc.entity.RepetitionSchedule;
import br.edu.unipam.tcc.entity.Student;
import br.edu.unipam.tcc.entity.StudentCourse;
import br.edu.unipam.tcc.entity.Subject;
import br.edu.unipam.tcc.entity.enums.ChatIntent;
import br.edu.unipam.tcc.entity.enums.ChatState;
import br.edu.unipam.tcc.entity.enums.ScheduleStatus;
import br.edu.unipam.tcc.repository.CourseRepository;
import br.edu.unipam.tcc.repository.FlashcardRepository;
import br.edu.unipam.tcc.repository.RepetitionScheduleRepository;
import br.edu.unipam.tcc.repository.StudentCourseRepository;
import br.edu.unipam.tcc.repository.StudentRepository;
import br.edu.unipam.tcc.repository.SubjectRepository;
import br.edu.unipam.tcc.service.impl.ChatFlowOrchestratorImpl;
import br.edu.unipam.tcc.session.ChatSessionState;
import br.edu.unipam.tcc.session.ChatSessionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatFlowOrchestratorImplTest {

    private static final String PHONE = "5534999998888";
    private static final String JID = PHONE + "@s.whatsapp.net";

    @Mock private ChatSessionStore chatSessionStore;
    @Mock private StudentRepository studentRepository;
    @Mock private StudentCourseRepository studentCourseRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private FlashcardRepository flashcardRepository;
    @Mock private RepetitionScheduleRepository repetitionScheduleRepository;
    @Mock private MmeebbService mmeebbService;
    @Mock private UazapiClientService uazapiClientService;
    @Mock private SubjectRagService subjectRagService;
    @Mock private IntentRouterService intentRouterService;
    @Mock private AnswerEvaluationService answerEvaluationService;
    @Mock private StudentOnboardingService studentOnboardingService;

    @InjectMocks private ChatFlowOrchestratorImpl orchestrator;

    private Student student;
    private Course course;
    private Subject subject;
    private Flashcard flashcard;

    @BeforeEach
    void setUp() {
        course = Course.builder().code("MED").name("Medicina").active(true).build();
        course.setId(1L);

        subject = Subject.builder().course(course).code("CARD").name("Cardiologia").active(true).build();
        subject.setId(10L);

        student = Student.builder()
                .phoneNumber(PHONE)
                .fullName("Maria Silva")
                .active(true)
                .build();
        student.setId(UUID.randomUUID());

        flashcard = Flashcard.builder()
                .subject(subject)
                .topic("Arritmias")
                .question("Qual a primeira conduta na FA instável?")
                .answer("Cardioversão elétrica sincronizada")
                .active(true)
                .build();
        flashcard.setId(100L);
    }

    private UazapiWebhookDto message(String text) {
        return new UazapiWebhookDto(null, JID, false, text, null, null, "msg-id");
    }

    private ChatSessionState registeredSession(ChatState state) {
        return ChatSessionState.builder()
                .phoneNumber(PHONE)
                .studentId(student.getId())
                .selectedCourseId(course.getId())
                .currentState(state)
                .build();
    }

    private String lastSentMessage() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(uazapiClientService, org.mockito.Mockito.atLeastOnce()).sendTextMessage(eq(PHONE), captor.capture());
        return captor.getValue();
    }

    // =========================================================================
    // Onboarding
    // =========================================================================

    @Test
    @DisplayName("Deve iniciar o formulário de cadastro pedindo o nome completo no primeiro contato")
    void shouldStartOnboardingOnFirstContact() {
        when(chatSessionStore.find(PHONE)).thenReturn(Optional.empty());
        when(studentRepository.findByPhoneNumber(PHONE)).thenReturn(Optional.empty());

        orchestrator.processIncomingMessage(message("oi"));

        ArgumentCaptor<ChatSessionState> captor = ArgumentCaptor.forClass(ChatSessionState.class);
        verify(chatSessionStore).save(captor.capture());

        assertEquals(ChatState.AWAITING_FULL_NAME, captor.getValue().getCurrentState());
        assertTrue(lastSentMessage().contains("nome completo"));
    }

    @Test
    @DisplayName("Deve recusar nome sem sobrenome e permanecer aguardando o nome completo")
    void shouldRejectSingleWordName() {
        ChatSessionState session = ChatSessionState.builder()
                .phoneNumber(PHONE)
                .currentState(ChatState.AWAITING_FULL_NAME)
                .build();
        when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));

        orchestrator.processIncomingMessage(message("Maria"));

        verify(chatSessionStore, never()).save(any());
        assertEquals(ChatState.AWAITING_FULL_NAME, session.getCurrentState());
        assertTrue(lastSentMessage().contains("nome completo"));
    }

    @Test
    @DisplayName("Deve pular o RA e listar os cursos quando o estudante enviar 'pular'")
    void shouldSkipRaAndListCourses() {
        ChatSessionState session = ChatSessionState.builder()
                .phoneNumber(PHONE)
                .currentState(ChatState.AWAITING_RA)
                .draftFullName("Maria Silva")
                .build();
        when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));
        when(courseRepository.findByActiveTrue()).thenReturn(List.of(course));

        orchestrator.processIncomingMessage(message("pular"));

        assertEquals(ChatState.AWAITING_COURSE, session.getCurrentState());
        assertTrue(lastSentMessage().contains("Medicina"));
    }

    @Test
    @DisplayName("Deve recusar RA já vinculado a outro número de WhatsApp")
    void shouldRejectRaOwnedByAnotherPhone() {
        Student other = Student.builder().phoneNumber("5511111111111").fullName("Outro").build();
        other.setId(UUID.randomUUID());

        ChatSessionState session = ChatSessionState.builder()
                .phoneNumber(PHONE)
                .currentState(ChatState.AWAITING_RA)
                .draftFullName("Maria Silva")
                .build();
        when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));
        when(studentRepository.findByRa("12345")).thenReturn(Optional.of(other));

        orchestrator.processIncomingMessage(message("12345"));

        assertEquals(ChatState.AWAITING_RA, session.getCurrentState());
        assertTrue(lastSentMessage().contains("já está vinculado"));
    }

    @Test
    @DisplayName("Deve concluir o cadastro ao receber o período e transicionar para o MAIN_MENU")
    void shouldCompleteRegistrationOnPeriodInput() {
        ChatSessionState session = ChatSessionState.builder()
                .phoneNumber(PHONE)
                .currentState(ChatState.AWAITING_ACADEMIC_PERIOD)
                .draftFullName("Maria Silva")
                .draftRa("12345")
                .draftCourseId(course.getId())
                .build();
        when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));
        when(studentOnboardingService.register(PHONE, "Maria Silva", "12345", course.getId(), 8))
                .thenReturn(student);
        when(repetitionScheduleRepository
                .countByStudentIdAndNextReviewDateLessThanEqualAndIsActiveTrue(eq(student.getId()), any()))
                .thenReturn(12L);

        orchestrator.processIncomingMessage(message("8"));

        verify(studentOnboardingService).register(PHONE, "Maria Silva", "12345", course.getId(), 8);
        assertEquals(ChatState.MAIN_MENU, session.getCurrentState());
        assertEquals(student.getId(), session.getStudentId());

        String sent = lastSentMessage();
        assertTrue(sent.contains("Cadastro concluído"));
        assertTrue(sent.contains("12"));
    }

    @Test
    @DisplayName("Deve recusar período fora da faixa permitida")
    void shouldRejectInvalidAcademicPeriod() {
        ChatSessionState session = ChatSessionState.builder()
                .phoneNumber(PHONE)
                .currentState(ChatState.AWAITING_ACADEMIC_PERIOD)
                .draftCourseId(course.getId())
                .build();
        when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));

        orchestrator.processIncomingMessage(message("99"));

        verify(studentOnboardingService, never()).register(anyString(), anyString(), anyString(), any(), any());
        assertTrue(lastSentMessage().contains("Período inválido"));
    }

    @Test
    @DisplayName("Não deve refazer o formulário quando o telefone já estiver cadastrado e a sessão tiver expirado")
    void shouldNotRepeatOnboardingForRegisteredPhone() {
        StudentCourse link = StudentCourse.builder().student(student).course(course).active(true).build();

        when(chatSessionStore.find(PHONE)).thenReturn(Optional.empty());
        when(studentRepository.findByPhoneNumber(PHONE)).thenReturn(Optional.of(student));
        when(studentCourseRepository.findByStudentIdAndActiveTrue(student.getId())).thenReturn(List.of(link));
        when(intentRouterService.classify("bom dia")).thenReturn(IntentResultDto.of(ChatIntent.SHOW_MENU));

        orchestrator.processIncomingMessage(message("bom dia"));

        verify(studentOnboardingService, never()).register(anyString(), anyString(), anyString(), any(), any());
        assertTrue(lastSentMessage().contains("Menu Principal"));
    }

    // =========================================================================
    // Roteamento por intenção
    // =========================================================================

    @Test
    @DisplayName("Deve iniciar a revisão quando a intenção classificada for START_REVIEW")
    void shouldStartReviewWhenIntentIsStartReview() {
        ChatSessionState session = registeredSession(ChatState.MAIN_MENU);
        when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));
        when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(intentRouterService.classify("quero estudar um pouco"))
                .thenReturn(IntentResultDto.of(ChatIntent.START_REVIEW));

        RepetitionSchedule schedule = RepetitionSchedule.builder().student(student).flashcard(flashcard).build();
        when(repetitionScheduleRepository.findPendingReviewsByStudent(
                eq(student.getId()), any(LocalDate.class), eq(ScheduleStatus.PENDING)))
                .thenReturn(List.of(schedule));

        orchestrator.processIncomingMessage(message("quero estudar um pouco"));

        assertEquals(ChatState.REVIEW_MODE, session.getCurrentState());
        assertEquals(flashcard.getId(), session.getCurrentFlashcardId());
        assertTrue(lastSentMessage().contains("Arritmias"));
    }

    @Test
    @DisplayName("Deve responder dúvida em texto livre pelo RAG e restringir a busca à disciplina citada")
    void shouldAnswerFreeTextDoubtScopedToMentionedSubject() {
        ChatSessionState session = registeredSession(ChatState.MAIN_MENU);
        when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));
        when(intentRouterService.classify(anyString()))
                .thenReturn(new IntentResultDto(ChatIntent.ASK_DOUBT, "cardiologia"));
        when(subjectRepository.findByCourseIdAndActiveTrueAndNameContainingIgnoreCase(course.getId(), "cardiologia"))
                .thenReturn(List.of(subject));
        when(subjectRepository.findById(subject.getId())).thenReturn(Optional.of(subject));
        when(subjectRagService.answerDoubt(anyString(), eq(subject.getId())))
                .thenReturn("A cardioversão é indicada na instabilidade.");

        orchestrator.processIncomingMessage(message("tenho uma dúvida de cardiologia sobre fibrilação atrial"));

        verify(subjectRagService).answerDoubt(
                "tenho uma dúvida de cardiologia sobre fibrilação atrial", subject.getId());
        assertEquals(ChatState.RAG_DOUBT_MODE, session.getCurrentState());
        assertEquals(subject.getId(), session.getSelectedSubjectId());
        assertTrue(lastSentMessage().contains("Cardiologia"));
    }

    @Test
    @DisplayName("Deve consultar o acervo geral quando não houver disciplina selecionada na sessão")
    void shouldQueryGlobalRagWhenNoSubjectSelected() {
        ChatSessionState session = registeredSession(ChatState.RAG_DOUBT_MODE);
        when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));
        when(subjectRagService.answerDoubt(anyString(), eq(null))).thenReturn("Resposta geral.");

        orchestrator.processIncomingMessage(message("o que é sepse?"));

        verify(subjectRagService).answerDoubt("o que é sepse?", null);
        assertTrue(lastSentMessage().contains("Acervo geral"));
    }

    @Test
    @DisplayName("Não deve classificar intenção quando o estudante enviar um número do menu")
    void shouldSkipIntentClassificationForNumericMenuOption() {
        ChatSessionState session = registeredSession(ChatState.MAIN_MENU);
        when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));

        orchestrator.processIncomingMessage(message("2"));

        verify(intentRouterService, never()).classify(anyString());
        assertEquals(ChatState.RAG_DOUBT_MODE, session.getCurrentState());
    }

    // =========================================================================
    // Modo revisão / motor MMEEBB
    // =========================================================================

    @Test
    @DisplayName("Deve aceitar resposta dissertativa aprovada na correção semântica e dobrar o intervalo")
    void shouldAcceptSemanticallyCorrectAnswerAndDoubleInterval() {
        ChatSessionState session = registeredSession(ChatState.REVIEW_MODE);
        session.setCurrentFlashcardId(flashcard.getId());

        when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));
        when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(flashcardRepository.findById(flashcard.getId())).thenReturn(Optional.of(flashcard));
        when(answerEvaluationService.evaluate(anyString(), eq(flashcard)))
                .thenReturn(new AnswerEvaluationDto(true, "Boa! É exatamente a conduta indicada."));

        RepetitionSchedule schedule = RepetitionSchedule.builder()
                .student(student).flashcard(flashcard).nIndex(1).intervalDays(2).build();
        when(repetitionScheduleRepository.findByStudentIdAndFlashcardId(student.getId(), flashcard.getId()))
                .thenReturn(Optional.of(schedule));

        RepetitionSchedule updated = RepetitionSchedule.builder()
                .student(student).flashcard(flashcard).nIndex(2).intervalDays(4)
                .status(ScheduleStatus.PENDING).build();
        when(mmeebbService.processAnswer(eq(schedule), eq(true), any())).thenReturn(updated);
        when(repetitionScheduleRepository.findPendingReviewsByStudent(
                eq(student.getId()), any(LocalDate.class), eq(ScheduleStatus.PENDING)))
                .thenReturn(List.of());

        orchestrator.processIncomingMessage(message("choque elétrico sincronizado"));

        verify(mmeebbService).processAnswer(eq(schedule), eq(true), any());
        verify(repetitionScheduleRepository).save(updated);

        String sent = lastSentMessage();
        assertTrue(sent.contains("Resposta correta"));
        assertTrue(sent.contains("4 dia(s)"));
        assertTrue(sent.contains("É exatamente a conduta indicada."));
        assertEquals(ChatState.MAIN_MENU, session.getCurrentState());
    }

    @Test
    @DisplayName("Deve reiniciar o intervalo e exibir o gabarito quando a resposta for reprovada")
    void shouldResetIntervalOnIncorrectAnswer() {
        ChatSessionState session = registeredSession(ChatState.REVIEW_MODE);
        session.setCurrentFlashcardId(flashcard.getId());

        when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));
        when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(flashcardRepository.findById(flashcard.getId())).thenReturn(Optional.of(flashcard));
        when(answerEvaluationService.evaluate(anyString(), eq(flashcard)))
                .thenReturn(AnswerEvaluationDto.rejected());

        RepetitionSchedule schedule = RepetitionSchedule.builder()
                .student(student).flashcard(flashcard).nIndex(3).intervalDays(8).build();
        when(repetitionScheduleRepository.findByStudentIdAndFlashcardId(student.getId(), flashcard.getId()))
                .thenReturn(Optional.of(schedule));

        RepetitionSchedule updated = RepetitionSchedule.builder()
                .student(student).flashcard(flashcard).nIndex(0).intervalDays(1)
                .status(ScheduleStatus.PENDING).build();
        when(mmeebbService.processAnswer(eq(schedule), eq(false), any())).thenReturn(updated);
        when(repetitionScheduleRepository.findPendingReviewsByStudent(
                eq(student.getId()), any(LocalDate.class), eq(ScheduleStatus.PENDING)))
                .thenReturn(List.of());

        orchestrator.processIncomingMessage(message("massagem cardíaca"));

        String sent = lastSentMessage();
        assertTrue(sent.contains("Resposta incorreta"));
        assertTrue(sent.contains("Cardioversão elétrica sincronizada"));
    }

    @Test
    @DisplayName("Deve avisar que não há revisões pendentes e voltar ao menu")
    void shouldReportNoPendingReviews() {
        ChatSessionState session = registeredSession(ChatState.MAIN_MENU);
        when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));
        when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(repetitionScheduleRepository.findPendingReviewsByStudent(
                eq(student.getId()), any(LocalDate.class), eq(ScheduleStatus.PENDING)))
                .thenReturn(List.of());

        orchestrator.processIncomingMessage(message("1"));

        assertEquals(ChatState.MAIN_MENU, session.getCurrentState());
        assertTrue(lastSentMessage().contains("Nenhuma revisão pendente"));
    }

    // =========================================================================
    // Comandos globais
    // =========================================================================

    @Test
    @DisplayName("Deve encerrar a sessão e limpar o flashcard em curso ao receber 'sair'")
    void shouldHandleExitCommand() {
        ChatSessionState session = registeredSession(ChatState.REVIEW_MODE);
        session.setCurrentFlashcardId(flashcard.getId());
        when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));

        orchestrator.processIncomingMessage(message("sair"));

        assertEquals(ChatState.MAIN_MENU, session.getCurrentState());
        assertEquals(null, session.getCurrentFlashcardId());
        assertTrue(lastSentMessage().contains("Até logo"));
    }

    @Test
    @DisplayName("Não deve tratar 'menu' como comando global durante o formulário de cadastro")
    void shouldNotTreatResetAsGlobalCommandDuringOnboarding() {
        ChatSessionState session = ChatSessionState.builder()
                .phoneNumber(PHONE)
                .currentState(ChatState.AWAITING_FULL_NAME)
                .build();
        when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));

        orchestrator.processIncomingMessage(message("menu"));

        assertEquals(ChatState.AWAITING_FULL_NAME, session.getCurrentState());
        assertTrue(lastSentMessage().contains("nome completo"));
    }

    @Test
    @DisplayName("Deve ignorar payload nulo e mensagens enviadas pelo próprio bot")
    void shouldIgnoreNullAndFromMeMessages() {
        orchestrator.processIncomingMessage(null);
        orchestrator.processIncomingMessage(new UazapiWebhookDto(null, JID, true, "oi", null, null, "msg"));

        verify(uazapiClientService, never()).sendTextMessage(anyString(), anyString());
        verify(chatSessionStore, never()).save(any());
    }
}
