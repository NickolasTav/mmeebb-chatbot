package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.dto.AdaptiveReviewOutcomeDto;
import br.edu.unipam.tcc.dto.AnswerEvaluationDto;
import br.edu.unipam.tcc.dto.IntentResultDto;
import br.edu.unipam.tcc.dto.TutorContextDto;
import br.edu.unipam.tcc.dto.TopicPerformanceDto;
import br.edu.unipam.tcc.dto.UazapiWebhookDto;
import br.edu.unipam.tcc.entity.Course;
import br.edu.unipam.tcc.entity.Flashcard;
import br.edu.unipam.tcc.entity.RepetitionSchedule;
import br.edu.unipam.tcc.entity.Student;
import br.edu.unipam.tcc.entity.StudentCourse;
import br.edu.unipam.tcc.entity.Subject;
import br.edu.unipam.tcc.entity.enums.ChatIntent;
import br.edu.unipam.tcc.entity.enums.ChatState;
import br.edu.unipam.tcc.entity.enums.QuestionType;
import br.edu.unipam.tcc.entity.enums.ScheduleStatus;
import br.edu.unipam.tcc.entity.enums.TopicMastery;
import br.edu.unipam.tcc.flow.StudentPerformanceFlowHandler;
import br.edu.unipam.tcc.flow.StudentSettingsFlowHandler;
import br.edu.unipam.tcc.repository.CourseRepository;
import br.edu.unipam.tcc.repository.FlashcardRepository;
import br.edu.unipam.tcc.repository.RepetitionScheduleRepository;
import br.edu.unipam.tcc.repository.StudentCourseRepository;
import br.edu.unipam.tcc.repository.StudentRepository;
import br.edu.unipam.tcc.messaging.OutgoingMessagePublisher;
import br.edu.unipam.tcc.observability.MmeebbMetrics;
import br.edu.unipam.tcc.repository.SubjectRepository;
import br.edu.unipam.tcc.service.impl.ChatFlowOrchestratorImpl;
import br.edu.unipam.tcc.session.ChatSessionState;
import br.edu.unipam.tcc.session.ChatSessionStore;
import br.edu.unipam.tcc.util.OptionShuffler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
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
    @Mock private AdaptiveReviewService adaptiveReviewService;
    @Mock private AiTutorService aiTutorService;
    @Mock private IntentRouterService intentRouterService;
    @Mock private AnswerEvaluationService answerEvaluationService;
    @Mock private StudentOnboardingService studentOnboardingService;
    @Mock private MmeebbMetrics mmeebbMetrics;
    @Mock private StudentSettingsFlowHandler studentSettingsFlowHandler;
    @Mock private StudentPerformanceFlowHandler studentPerformanceFlowHandler;
    // 02:30 UTC do dia 18 equivale a 23:30 do dia 17 em Brasília: prova que a data vem do fuso de São Paulo.
    @Spy private Clock clock = Clock.fixed(Instant.parse("2026-09-18T02:30:00Z"), ZoneId.of("America/Sao_Paulo"));
    @Mock private OutgoingMessagePublisher outgoingMessagePublisher;
    // Embaralhador real com semente fixa: os testes conferem a regra, não um mock dela.
    @Spy private OptionShuffler optionShuffler = new OptionShuffler(new Random(42));

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

        // Sem estímulo adaptativo, a fila chega ao aluno na mesma ordem em que veio do banco.
        when(adaptiveReviewService.prioritize(any(), anyList()))
                .thenAnswer(invocation -> invocation.getArgument(1));
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
        verify(outgoingMessagePublisher, org.mockito.Mockito.atLeastOnce()).publish(eq(PHONE), captor.capture());
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

        String sent = lastSentMessage();
        assertTrue(sent.contains("Seja bem-vindo ao Chatbot MMEEBB UNIPAM"));
        assertTrue(sent.contains("Por favor"));
        assertTrue(sent.contains("*nome completo*"));
        assertTrue(sent.contains("Maria Silva Andrade"));
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
        assertTrue(sent.contains("Cadastro concluído, Maria"));
        assertTrue(sent.contains("12"));
        assertTrue(sent.contains("*08:00*"));
        assertTrue(sent.contains("*configurações*"));
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
        String menu = lastSentMessage();
        assertTrue(menu.contains("Menu Principal"));
        assertTrue(menu.contains("Configurações"));
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
    @DisplayName("Deve responder dúvida em texto livre pelo tutor com a disciplina citada no contexto")
    void shouldAnswerFreeTextDoubtWithMentionedSubjectInContext() {
        ChatSessionState session = registeredSession(ChatState.MAIN_MENU);
        when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));
        when(intentRouterService.classify(anyString()))
                .thenReturn(new IntentResultDto(ChatIntent.ASK_DOUBT, "cardiologia"));
        when(subjectRepository.findByCourseIdAndActiveTrueAndNameContainingIgnoreCase(course.getId(), "cardiologia"))
                .thenReturn(List.of(subject));
        when(subjectRepository.findById(subject.getId())).thenReturn(Optional.of(subject));
        when(aiTutorService.answer(any(TutorContextDto.class), anyString()))
                .thenReturn("A cardioversão é indicada na instabilidade.");

        orchestrator.processIncomingMessage(message("tenho uma dúvida de cardiologia sobre fibrilação atrial"));

        ArgumentCaptor<TutorContextDto> context = ArgumentCaptor.forClass(TutorContextDto.class);
        verify(aiTutorService).answer(context.capture(),
                eq("tenho uma dúvida de cardiologia sobre fibrilação atrial"));
        assertEquals("Cardiologia", context.getValue().subjectName());
        assertNull(context.getValue().reviewQuestion());
        assertEquals(ChatState.DOUBT_MODE, session.getCurrentState());
        assertEquals(subject.getId(), session.getSelectedSubjectId());
        assertTrue(lastSentMessage().contains("Cardiologia"));
    }

    @Test
    @DisplayName("Deve enviar ao tutor o perfil acadêmico do aluno como contexto")
    void shouldSendStudentProfileAsTutorContext() {
        ChatSessionState session = registeredSession(ChatState.DOUBT_MODE);
        when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));
        when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        when(studentCourseRepository.findByStudentIdAndCourseId(student.getId(), course.getId()))
                .thenReturn(Optional.of(StudentCourse.builder()
                        .student(student).course(course).academicPeriod(5).build()));
        when(aiTutorService.answer(any(TutorContextDto.class), anyString())).thenReturn("Resposta.");

        orchestrator.processIncomingMessage(message("o que é sepse?"));

        ArgumentCaptor<TutorContextDto> context = ArgumentCaptor.forClass(TutorContextDto.class);
        verify(aiTutorService).answer(context.capture(), eq("o que é sepse?"));
        TutorContextDto sent = context.getValue();
        assertEquals(PHONE, sent.phoneNumber());
        assertEquals(student.displayName(), sent.studentName());
        assertEquals("Medicina", sent.courseName());
        assertEquals(5, sent.academicPeriod());
        assertNull(sent.subjectName());
        assertNull(sent.reviewQuestion());
    }

    @Test
    @DisplayName("Deve responder a dúvida mesmo sem cadastro, curso ou matrícula encontrados")
    void shouldAnswerDoubtEvenWhenProfileIsUnknown() {
        ChatSessionState session = registeredSession(ChatState.DOUBT_MODE);
        when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));
        when(aiTutorService.answer(any(TutorContextDto.class), anyString())).thenReturn("Resposta.");

        orchestrator.processIncomingMessage(message("o que é sepse?"));

        ArgumentCaptor<TutorContextDto> context = ArgumentCaptor.forClass(TutorContextDto.class);
        verify(aiTutorService).answer(context.capture(), eq("o que é sepse?"));
        assertNull(context.getValue().studentName());
        assertNull(context.getValue().courseName());
        assertNull(context.getValue().academicPeriod());
        assertTrue(lastSentMessage().contains("Resposta."));
    }

    @Test
    @DisplayName("Deve rotular a resposta como tema livre quando não houver disciplina em foco")
    void shouldLabelAnswerAsFreeTopicWhenNoSubjectSelected() {
        ChatSessionState session = registeredSession(ChatState.DOUBT_MODE);
        when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));
        when(aiTutorService.answer(any(TutorContextDto.class), anyString())).thenReturn("Resposta geral.");

        orchestrator.processIncomingMessage(message("o que é sepse?"));

        String sent = lastSentMessage();
        assertTrue(sent.contains("Tema livre"));
        assertTrue(sent.contains("Resposta geral."));
        assertFalse(sent.contains("Acervo"));
    }

    @Test
    @DisplayName("Não deve classificar intenção quando o estudante enviar um número do menu")
    void shouldSkipIntentClassificationForNumericMenuOption() {
        ChatSessionState session = registeredSession(ChatState.MAIN_MENU);
        when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));

        orchestrator.processIncomingMessage(message("2"));

        verify(intentRouterService, never()).classify(anyString());
        assertEquals(ChatState.DOUBT_MODE, session.getCurrentState());
        String sent = lastSentMessage();
        assertTrue(sent.contains("Modo Dúvidas"));
        assertFalse(sent.contains("acervo"));
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
                .thenReturn(new AnswerEvaluationDto(true, "Boa! É exatamente a conduta indicada.", false));

        RepetitionSchedule updated = RepetitionSchedule.builder()
                .student(student).flashcard(flashcard).nIndex(2).intervalDays(4)
                .status(ScheduleStatus.PENDING).build();
        when(adaptiveReviewService.processAnswer(eq(student), eq(flashcard), eq(true), any()))
                .thenReturn(outcome(updated, TopicMastery.SEM_DADOS, 0.0, false, false, 0));
        when(repetitionScheduleRepository.findPendingReviewsByStudent(
                eq(student.getId()), any(LocalDate.class), eq(ScheduleStatus.PENDING)))
                .thenReturn(List.of());

        orchestrator.processIncomingMessage(message("choque elétrico sincronizado"));

        verify(adaptiveReviewService).processAnswer(eq(student), eq(flashcard), eq(true), any());

        String sent = lastSentMessage();
        assertTrue(sent.startsWith("✅ *Correto!*\n📅 Próximo reforço em *4 dias* (2^2)"), sent);
        assertTrue(sent.contains("É exatamente a conduta indicada."));
        assertFalse(sent.contains("Ajuste personalizado"));
        assertEquals(ChatState.MAIN_MENU, session.getCurrentState());
    }

    @Test
    @DisplayName("Deve explicar ao aluno quando o teto do tópico frágil segurar o intervalo")
    void shouldExplainAdaptiveCapToStudent() {
        ChatSessionState session = registeredSession(ChatState.REVIEW_MODE);
        session.setCurrentFlashcardId(flashcard.getId());

        when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));
        when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(flashcardRepository.findById(flashcard.getId())).thenReturn(Optional.of(flashcard));
        when(answerEvaluationService.evaluate(anyString(), eq(flashcard)))
                .thenReturn(new AnswerEvaluationDto(true, null, false));

        RepetitionSchedule updated = RepetitionSchedule.builder()
                .student(student).flashcard(flashcard).nIndex(3).intervalDays(8)
                .status(ScheduleStatus.PENDING).build();
        when(adaptiveReviewService.processAnswer(eq(student), eq(flashcard), eq(true), any()))
                .thenReturn(outcome(updated, TopicMastery.FRAGIL, 0.67, true, false, 0));
        when(repetitionScheduleRepository.findPendingReviewsByStudent(
                eq(student.getId()), any(LocalDate.class), eq(ScheduleStatus.PENDING)))
                .thenReturn(List.of());

        orchestrator.processIncomingMessage(message("cardioversão"));

        String sent = lastSentMessage();
        assertTrue(sent.contains("Ajuste personalizado"));
        assertTrue(sent.contains("Arritmias"));
        assertTrue(sent.contains("67% de erro"));
        assertTrue(sent.contains("8 dia(s)"));
    }

    @Test
    @DisplayName("Deve anunciar o reforço dirigido e o lapso amortecido quando houver erro")
    void shouldAnnounceReinforcementAndSoftenedLapse() {
        ChatSessionState session = registeredSession(ChatState.REVIEW_MODE);
        session.setCurrentFlashcardId(flashcard.getId());

        when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));
        when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(flashcardRepository.findById(flashcard.getId())).thenReturn(Optional.of(flashcard));
        when(answerEvaluationService.evaluate(anyString(), eq(flashcard)))
                .thenReturn(AnswerEvaluationDto.rejected());

        RepetitionSchedule updated = RepetitionSchedule.builder()
                .student(student).flashcard(flashcard).nIndex(4).intervalDays(16)
                .status(ScheduleStatus.PENDING).build();
        when(adaptiveReviewService.processAnswer(eq(student), eq(flashcard), eq(false), any()))
                .thenReturn(outcome(updated, TopicMastery.DOMINADO, 0.08, false, true, 2));
        when(repetitionScheduleRepository.findPendingReviewsByStudent(
                eq(student.getId()), any(LocalDate.class), eq(ScheduleStatus.PENDING)))
                .thenReturn(List.of());

        orchestrator.processIncomingMessage(message("não sei"));

        String sent = lastSentMessage();
        assertTrue(sent.contains("📅 Ciclo recuado, sem voltar ao zero: próximo reforço em *16 dias* (2^4)."), sent);
        assertFalse(sent.contains("Ciclo reiniciado"));
        assertTrue(sent.contains("tratei isto como um lapso"));
        assertTrue(sent.contains("Reforço dirigido"));
        assertTrue(sent.contains("*2* questão(ões)"));
    }

    @Test
    @DisplayName("Deve entregar a fila de revisões já priorizada pela camada adaptativa")
    void shouldDeliverPrioritizedReviewQueue() {
        ChatSessionState session = registeredSession(ChatState.MAIN_MENU);

        when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));
        when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));

        Flashcard fraco = Flashcard.builder().subject(subject).topic("Insuficiência cardíaca")
                .question("Qual a classe funcional NYHA III?").answer("Sintomas aos pequenos esforços")
                .active(true).build();
        fraco.setId(200L);

        RepetitionSchedule agendamentoArritmia = RepetitionSchedule.builder()
                .student(student).flashcard(flashcard).status(ScheduleStatus.PENDING).build();
        RepetitionSchedule agendamentoFraco = RepetitionSchedule.builder()
                .student(student).flashcard(fraco).status(ScheduleStatus.PENDING).build();

        when(repetitionScheduleRepository.findPendingReviewsByStudent(
                eq(student.getId()), any(LocalDate.class), eq(ScheduleStatus.PENDING)))
                .thenReturn(List.of(agendamentoArritmia, agendamentoFraco));
        when(adaptiveReviewService.prioritize(eq(student.getId()), anyList()))
                .thenReturn(List.of(agendamentoFraco, agendamentoArritmia));

        orchestrator.processIncomingMessage(message("1"));

        verify(adaptiveReviewService).prioritize(eq(student.getId()), anyList());
        assertEquals(fraco.getId(), session.getCurrentFlashcardId());
        assertTrue(lastSentMessage().contains("Insuficiência cardíaca"));
    }

    private AdaptiveReviewOutcomeDto outcome(RepetitionSchedule schedule, TopicMastery mastery, double errorRate,
                                             boolean intervalCapped, boolean lapseSoftened, int reinforcedCards) {
        TopicPerformanceDto performance = new TopicPerformanceDto(
                subject.getId(), subject.getName(), schedule.getFlashcard().getTopic(),
                10L, Math.round(errorRate * 10), errorRate, mastery);
        return new AdaptiveReviewOutcomeDto(schedule, performance, intervalCapped, lapseSoftened, reinforcedCards);
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

        RepetitionSchedule updated = RepetitionSchedule.builder()
                .student(student).flashcard(flashcard).nIndex(0).intervalDays(1)
                .status(ScheduleStatus.PENDING).build();
        when(adaptiveReviewService.processAnswer(eq(student), eq(flashcard), eq(false), any()))
                .thenReturn(outcome(updated, TopicMastery.SEM_DADOS, 0.0, false, false, 0));
        when(repetitionScheduleRepository.findPendingReviewsByStudent(
                eq(student.getId()), any(LocalDate.class), eq(ScheduleStatus.PENDING)))
                .thenReturn(List.of());

        orchestrator.processIncomingMessage(message("massagem cardíaca"));

        String sent = lastSentMessage();
        assertTrue(sent.startsWith("❌ *Não foi dessa vez.*\n✔️ Correta: *Cardioversão elétrica sincronizada*\n"
                + "📅 Ciclo reiniciado: próximo reforço em *1 dia* (2^0), para consolidar."), sent);
    }

    @Test
    @DisplayName("Deve pedir ajuda ao tutor com a questão em revisão e reenviá-la sem tocar no MMEEBB")
    void shouldAskTutorWithReviewQuestionAndResendItWhenDoubtDuringReview() {
        ChatSessionState session = registeredSession(ChatState.REVIEW_MODE);
        session.setCurrentFlashcardId(flashcard.getId());

        when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));
        when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(flashcardRepository.findById(flashcard.getId())).thenReturn(Optional.of(flashcard));
        when(flashcardRepository.findSubjectIdById(flashcard.getId())).thenReturn(Optional.of(subject.getId()));
        when(subjectRepository.findById(subject.getId())).thenReturn(Optional.of(subject));
        when(answerEvaluationService.evaluate(anyString(), eq(flashcard)))
                .thenReturn(AnswerEvaluationDto.doubt());
        when(aiTutorService.answer(any(TutorContextDto.class), anyString()))
                .thenReturn("Pense no que define instabilidade hemodinâmica.");

        orchestrator.processIncomingMessage(message("não entendi, pode explicar essa questão?"));

        ArgumentCaptor<TutorContextDto> context = ArgumentCaptor.forClass(TutorContextDto.class);
        verify(aiTutorService).answer(context.capture(), eq("não entendi, pode explicar essa questão?"));
        TutorContextDto.ReviewQuestion review = context.getValue().reviewQuestion();
        assertEquals("Arritmias", review.topic());
        assertEquals("Qual a primeira conduta na FA instável?", review.question());
        assertEquals("Cardioversão elétrica sincronizada", review.answer());
        assertEquals("Cardiologia", context.getValue().subjectName());
        verify(adaptiveReviewService, never()).processAnswer(any(), any(), anyBoolean(), any());

        String sent = lastSentMessage();
        assertTrue(sent.contains("Pense no que define instabilidade hemodinâmica."));
        assertTrue(sent.contains("Qual a primeira conduta na FA instável?"));
    }

    @Test
    @DisplayName("Deve manter o flashcard ativo e o estado de revisão após uma dúvida")
    void shouldKeepActiveFlashcardAfterDoubtDuringReview() {
        ChatSessionState session = registeredSession(ChatState.REVIEW_MODE);
        session.setCurrentFlashcardId(flashcard.getId());

        when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));
        when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(flashcardRepository.findById(flashcard.getId())).thenReturn(Optional.of(flashcard));
        when(flashcardRepository.findSubjectIdById(flashcard.getId())).thenReturn(Optional.of(subject.getId()));
        when(answerEvaluationService.evaluate(anyString(), eq(flashcard)))
                .thenReturn(AnswerEvaluationDto.doubt());
        when(aiTutorService.answer(any(TutorContextDto.class), anyString())).thenReturn("Explicação.");

        orchestrator.processIncomingMessage(message("por que não é outra conduta?"));

        assertEquals(ChatState.REVIEW_MODE, session.getCurrentState());
        assertEquals(flashcard.getId(), session.getCurrentFlashcardId());
    }

    // =========================================================================
    // Alternativas embaralhadas (D11) e formato da questão no WhatsApp (D12)
    // =========================================================================

    private static final List<String> MC_OPTIONS = List.of(
            "A) Cardioversão elétrica sincronizada", "B) Amiodarona venosa",
            "C) Adenosina em bolus", "D) Observação clínica");
    // Ordem exibida [2, 0, 3, 1]: A=Adenosina, B=Cardioversão (correta), C=Observação, D=Amiodarona.
    private static final List<Integer> SHOWN_ORDER = List.of(2, 0, 3, 1);
    private static final String SHOWN_OPTIONS = "*A)* Adenosina em bolus\n*B)* Cardioversão elétrica sincronizada\n"
            + "*C)* Observação clínica\n*D)* Amiodarona venosa";

    private Flashcard multipleChoice(long id) {
        Flashcard card = Flashcard.builder()
                .subject(subject)
                .topic("Cardiologia - Arritmias")
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .question("Qual a primeira conduta na FA com instabilidade hemodinâmica?")
                .answer("A")
                .optionsJson("[\"A) Cardioversão elétrica sincronizada\", \"B) Amiodarona venosa\", "
                        + "\"C) Adenosina em bolus\", \"D) Observação clínica\"]")
                .explanation("Instabilidade hemodinâmica pede cardioversão elétrica sincronizada imediata.")
                .active(true)
                .build();
        card.setId(id);
        return card;
    }

    private RepetitionSchedule scheduleOf(Flashcard card, long id, Integer lastCorrectPosition) {
        RepetitionSchedule schedule = RepetitionSchedule.builder()
                .student(student).flashcard(card).status(ScheduleStatus.PENDING)
                .lastCorrectPosition(lastCorrectPosition == null ? null : lastCorrectPosition.shortValue())
                .build();
        schedule.setId(id);
        return schedule;
    }

    private ChatSessionState reviewingShownOrder(Flashcard card) {
        ChatSessionState session = registeredSession(ChatState.REVIEW_MODE);
        session.setCurrentFlashcardId(card.getId());
        session.setCurrentOptionOrder(SHOWN_ORDER);
        when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));
        when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(flashcardRepository.findById(card.getId())).thenReturn(Optional.of(card));
        return session;
    }

    private void stubAnswerOutcome(Flashcard card, boolean correct, int nIndex, int intervalDays, boolean lapse) {
        RepetitionSchedule updated = RepetitionSchedule.builder()
                .student(student).flashcard(card).nIndex(nIndex).intervalDays(intervalDays)
                .status(ScheduleStatus.PENDING).build();
        when(adaptiveReviewService.processAnswer(eq(student), any(Flashcard.class), eq(correct), any()))
                .thenReturn(outcome(updated, TopicMastery.SEM_DADOS, 0.0, false, lapse, 0));
    }

    private void stubPending(RepetitionSchedule... schedules) {
        when(repetitionScheduleRepository.findPendingReviewsByStudent(
                eq(student.getId()), any(LocalDate.class), eq(ScheduleStatus.PENDING)))
                .thenReturn(List.of(schedules));
    }

    @Test
    @DisplayName("Deve apresentar a questão no formato novo, com contador e alternativas legíveis, nunca o JSON cru")
    void shouldPresentQuestionInNewFormat() {
        ChatSessionState session = registeredSession(ChatState.MAIN_MENU);
        when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));
        when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        Flashcard card = multipleChoice(300L);
        stubPending(scheduleOf(card, 30L, null), scheduleOf(multipleChoice(301L), 31L, null));

        orchestrator.processIncomingMessage(message("1"));

        String sent = lastSentMessage();
        assertTrue(sent.startsWith("📚 *Revisão MMEEBB* · faltam *2* hoje\n🏷️ _Cardiologia - Arritmias_\n\n"
                + "❓ Qual a primeira conduta na FA com instabilidade hemodinâmica?\n\n*A)* "), sent);
        assertTrue(sent.endsWith("\n\n✍️ _Responda com a letra (ex.: *B*)._\n_Dúvida? Pergunte — não conta como erro._"), sent);
        for (String letter : List.of("A", "B", "C", "D")) {
            assertTrue(sent.contains("\n*" + letter + ")* "), sent);
        }
        assertFalse(sent.contains("[\""), sent);
        assertFalse(sent.contains("Opções:"), sent);
        assertFalse(sent.contains("%n"), sent);
    }

    @Test
    @DisplayName("Ao apresentar, deve salvar a ordem na sessão e gravar no agendamento uma posição correta diferente da última")
    void shouldSaveOrderAndRecordNewCorrectPosition() {
        for (int last = 0; last < MC_OPTIONS.size(); last++) {
            org.mockito.Mockito.clearInvocations(repetitionScheduleRepository, outgoingMessagePublisher);
            ChatSessionState session = registeredSession(ChatState.MAIN_MENU);
            when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));
            when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
            Flashcard card = multipleChoice(300L);
            stubPending(scheduleOf(card, 30L, last));

            orchestrator.processIncomingMessage(message("1"));

            List<Integer> order = session.getCurrentOptionOrder();
            assertEquals(4, order.size());
            int correctPosition = order.indexOf(0);
            assertTrue(correctPosition != last, "a correta repetiu a posição " + last);
            verify(repetitionScheduleRepository).updateLastCorrectPosition(30L, (short) correctPosition);
            String letter = String.valueOf((char) ('A' + correctPosition));
            assertTrue(lastSentMessage().contains("*" + letter + ")* Cardioversão elétrica sincronizada"));
        }
    }

    @Test
    @DisplayName("Depois de uma dúvida, deve reenviar a questão na mesma ordem, sem sortear nem gravar posição")
    void shouldResendSameOrderAfterDoubt() {
        Flashcard card = multipleChoice(300L);
        ChatSessionState session = reviewingShownOrder(card);
        when(flashcardRepository.findSubjectIdById(card.getId())).thenReturn(Optional.of(subject.getId()));
        when(answerEvaluationService.evaluate(anyString(), any(Flashcard.class))).thenReturn(AnswerEvaluationDto.doubt());
        when(aiTutorService.answer(any(TutorContextDto.class), anyString())).thenReturn("Pense na instabilidade.");
        stubPending(scheduleOf(card, 30L, 1), scheduleOf(multipleChoice(301L), 31L, null));

        orchestrator.processIncomingMessage(message("por que não é a adenosina?"));

        String sent = lastSentMessage();
        assertTrue(sent.contains(SHOWN_OPTIONS), sent);
        assertTrue(sent.contains("faltam *2* hoje"), sent);
        assertEquals(SHOWN_ORDER, session.getCurrentOptionOrder());
        verify(repetitionScheduleRepository, never()).updateLastCorrectPosition(any(), any());
    }

    @Test
    @DisplayName("O tutor deve receber alternativas e gabarito na ordem exibida ao aluno")
    void tutorShouldReceiveDisplayedOptionsAndAnswer() {
        Flashcard card = multipleChoice(300L);
        reviewingShownOrder(card);
        when(answerEvaluationService.evaluate(anyString(), any(Flashcard.class))).thenReturn(AnswerEvaluationDto.doubt());
        when(aiTutorService.answer(any(TutorContextDto.class), anyString())).thenReturn("Explicação.");

        orchestrator.processIncomingMessage(message("não entendi"));

        ArgumentCaptor<TutorContextDto> context = ArgumentCaptor.forClass(TutorContextDto.class);
        verify(aiTutorService).answer(context.capture(), eq("não entendi"));
        TutorContextDto.ReviewQuestion review = context.getValue().reviewQuestion();
        assertEquals("B", review.answer());
        assertEquals(List.of("A) Adenosina em bolus", "B) Cardioversão elétrica sincronizada",
                "C) Observação clínica", "D) Amiodarona venosa"), OptionShuffler.parseOptions(review.options()));
    }

    @Test
    @DisplayName("Deve corrigir pela letra exibida e mandar ao MMEEBB a questão original")
    void shouldGradeByDisplayedLetter() {
        Flashcard card = multipleChoice(300L);
        reviewingShownOrder(card);
        when(answerEvaluationService.evaluate(anyString(), any(Flashcard.class))).thenReturn(AnswerEvaluationDto.accepted());
        stubAnswerOutcome(card, true, 2, 4, false);
        stubPending();

        orchestrator.processIncomingMessage(message("B"));

        ArgumentCaptor<Flashcard> graded = ArgumentCaptor.forClass(Flashcard.class);
        verify(answerEvaluationService).evaluate(eq("B"), graded.capture());
        assertEquals("B", graded.getValue().getAnswer());
        assertEquals(List.of("A) Adenosina em bolus", "B) Cardioversão elétrica sincronizada",
                "C) Observação clínica", "D) Amiodarona venosa"), OptionShuffler.parseOptions(graded.getValue().getOptionsJson()));

        ArgumentCaptor<Flashcard> scheduled = ArgumentCaptor.forClass(Flashcard.class);
        verify(adaptiveReviewService).processAnswer(eq(student), scheduled.capture(), eq(true), any());
        assertEquals("A", scheduled.getValue().getAnswer());
        assertEquals(card.getOptionsJson(), scheduled.getValue().getOptionsJson());
    }

    @Test
    @DisplayName("Sessão antiga, sem a ordem salva, deve corrigir pela ordem original")
    void oldSessionWithoutOrderGradesByOriginalOrder() {
        Flashcard card = multipleChoice(300L);
        ChatSessionState session = reviewingShownOrder(card);
        session.setCurrentOptionOrder(null);
        when(answerEvaluationService.evaluate(anyString(), any(Flashcard.class))).thenReturn(AnswerEvaluationDto.accepted());
        stubAnswerOutcome(card, true, 1, 2, false);
        stubPending();

        orchestrator.processIncomingMessage(message("A"));

        ArgumentCaptor<Flashcard> graded = ArgumentCaptor.forClass(Flashcard.class);
        verify(answerEvaluationService).evaluate(eq("A"), graded.capture());
        assertEquals("A", graded.getValue().getAnswer());
        assertEquals(MC_OPTIONS, OptionShuffler.parseOptions(graded.getValue().getOptionsJson()));
        assertTrue(lastSentMessage().contains("✅ *Correto!* Alternativa *A*"));
    }

    @Test
    @DisplayName("Acerto: letra exibida, próximo reforço em dias e a explicação como 'Por quê'")
    void correctAnswerFeedbackFormat() {
        Flashcard card = multipleChoice(300L);
        ChatSessionState session = reviewingShownOrder(card);
        when(answerEvaluationService.evaluate(anyString(), any(Flashcard.class))).thenReturn(AnswerEvaluationDto.accepted());
        stubAnswerOutcome(card, true, 2, 4, false);
        stubPending();

        orchestrator.processIncomingMessage(message("b"));

        String sent = lastSentMessage();
        assertTrue(sent.startsWith("✅ *Correto!* Alternativa *B*\n📅 Próximo reforço em *4 dias* (2^2)\n\n"), sent);
        assertTrue(sent.contains("💡 *Por quê:* Instabilidade hemodinâmica pede cardioversão elétrica sincronizada imediata."), sent);
        assertTrue(sent.contains("Todas as revisões de hoje foram concluídas"), sent);
        assertNull(session.getCurrentOptionOrder());
        assertFalse(sent.contains("%n"));
    }

    @Test
    @DisplayName("Erro: letra marcada, correta com o texto, ciclo reiniciado, separador e a próxima questão")
    void wrongAnswerFeedbackFormatAndNextQuestion() {
        Flashcard card = multipleChoice(300L);
        ChatSessionState session = reviewingShownOrder(card);
        when(answerEvaluationService.evaluate(anyString(), any(Flashcard.class))).thenReturn(AnswerEvaluationDto.rejected());
        stubAnswerOutcome(card, false, 0, 1, false);
        Flashcard next = multipleChoice(301L);
        stubPending(scheduleOf(next, 31L, null));

        orchestrator.processIncomingMessage(message("C"));

        String sent = lastSentMessage();
        assertTrue(sent.startsWith("❌ *Não foi dessa vez.* Você marcou *C*.\n"
                + "✔️ Correta: *B)* Cardioversão elétrica sincronizada\n"
                + "📅 Ciclo reiniciado: próximo reforço em *1 dia* (2^0), para consolidar.\n\n"), sent);
        assertTrue(sent.contains("💡 *Por quê:* "), sent);
        assertTrue(sent.contains("\n\n━━━━━━━━━━\n\n📚 *Revisão MMEEBB* · faltam *1* hoje"), sent);
        assertEquals(next.getId(), session.getCurrentFlashcardId());
        int nextPosition = session.getCurrentOptionOrder().indexOf(0);
        verify(repetitionScheduleRepository).updateLastCorrectPosition(31L, (short) nextPosition);
    }

    @Test
    @DisplayName("Erro em texto livre não deve inventar a letra marcada")
    void wrongFreeTextAnswerDoesNotClaimALetter() {
        Flashcard card = multipleChoice(300L);
        reviewingShownOrder(card);
        when(answerEvaluationService.evaluate(anyString(), any(Flashcard.class))).thenReturn(AnswerEvaluationDto.rejected());
        stubAnswerOutcome(card, false, 0, 1, false);
        stubPending();

        orchestrator.processIncomingMessage(message("amiodarona"));

        String sent = lastSentMessage();
        assertTrue(sent.startsWith("❌ *Não foi dessa vez.*\n✔️ Correta: *B)* Cardioversão elétrica sincronizada\n"), sent);
        assertFalse(sent.contains("Você marcou"), sent);
    }

    @Test
    @DisplayName("Sem revisões pendentes, não deve sortear ordem nem gravar posição")
    void shouldNotShuffleWithoutPendingReviews() {
        ChatSessionState session = registeredSession(ChatState.MAIN_MENU);
        when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));
        when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        stubPending();

        orchestrator.processIncomingMessage(message("1"));

        assertNull(session.getCurrentOptionOrder());
        verify(repetitionScheduleRepository, never()).updateLastCorrectPosition(any(), any());
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
    @DisplayName("Deve encerrar a sessão, limpar o flashcard em curso e esquecer a conversa ao receber 'sair'")
    void shouldHandleExitCommand() {
        ChatSessionState session = registeredSession(ChatState.REVIEW_MODE);
        session.setCurrentFlashcardId(flashcard.getId());
        when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));

        orchestrator.processIncomingMessage(message("sair"));

        assertEquals(ChatState.MAIN_MENU, session.getCurrentState());
        assertEquals(null, session.getCurrentFlashcardId());
        assertTrue(lastSentMessage().contains("Até logo"));
        verify(aiTutorService).clearConversation(PHONE);
    }

    @Test
    @DisplayName("Voltar ao menu não deve apagar a conversa com o tutor")
    void shouldKeepTutorHistoryOnMenuReset() {
        ChatSessionState session = registeredSession(ChatState.DOUBT_MODE);
        when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));

        orchestrator.processIncomingMessage(message("menu"));

        assertEquals(ChatState.MAIN_MENU, session.getCurrentState());
        verify(aiTutorService, never()).clearConversation(anyString());
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

        verify(outgoingMessagePublisher, never()).publish(anyString(), anyString());
        verify(chatSessionStore, never()).save(any());
    }

    // =========================================================================
    // Configurações
    // =========================================================================

    @Test
    @DisplayName("Deve abrir Configurações pela opção 3 do menu sem classificar intenção")
    void shouldOpenSettingsFromMenuOptionThree() {
        ChatSessionState session = registeredSession(ChatState.MAIN_MENU);
        when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));

        orchestrator.processIncomingMessage(message("3"));

        verify(studentSettingsFlowHandler).open(session);
        verify(intentRouterService, never()).classify(anyString());
    }

    @ParameterizedTest(name = "palavra-chave \"{0}\"")
    @DisplayName("Deve abrir Configurações por palavra-chave global mesmo durante uma revisão")
    @ValueSource(strings = {"configurações", "Configuracoes", "config", "AJUSTES", "preferências", "/config"})
    void shouldOpenSettingsFromGlobalKeyword(String keyword) {
        ChatSessionState session = registeredSession(ChatState.REVIEW_MODE);
        session.setCurrentFlashcardId(flashcard.getId());
        when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));

        orchestrator.processIncomingMessage(message(keyword));

        verify(studentSettingsFlowHandler).open(session);
        assertNull(session.getCurrentFlashcardId());
        verify(answerEvaluationService, never()).evaluate(anyString(), any());
    }

    // =========================================================================
    // Relatório de desempenho
    // =========================================================================

    @Test
    @DisplayName("Deve abrir o relatório de desempenho pela opção 4 do menu")
    void shouldOpenPerformanceReportFromMenuOption() {
        ChatSessionState session = registeredSession(ChatState.MAIN_MENU);
        when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));
        when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));

        orchestrator.processIncomingMessage(message("4"));

        verify(studentPerformanceFlowHandler).send(session, student);
        verify(intentRouterService, never()).classify(anyString());
    }

    @ParameterizedTest(name = "\"{0}\"")
    @DisplayName("Deve abrir o desempenho por palavra-chave em qualquer estado após o cadastro")
    @ValueSource(strings = {"desempenho", "meu desempenho", "progresso", "estatísticas", "como estou"})
    void shouldOpenPerformanceReportFromGlobalKeyword(String keyword) {
        ChatSessionState session = registeredSession(ChatState.REVIEW_MODE);
        session.setCurrentFlashcardId(flashcard.getId());
        when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));
        when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));

        orchestrator.processIncomingMessage(message(keyword));

        verify(studentPerformanceFlowHandler).send(session, student);
        verify(answerEvaluationService, never()).evaluate(anyString(), any());
    }

    @Test
    @DisplayName("Deve abrir o desempenho quando a intenção classificada for SHOW_PERFORMANCE")
    void shouldOpenPerformanceReportFromIntent() {
        ChatSessionState session = registeredSession(ChatState.MAIN_MENU);
        when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));
        when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(intentRouterService.classify("será que estou melhorando?"))
                .thenReturn(IntentResultDto.of(ChatIntent.SHOW_PERFORMANCE));

        orchestrator.processIncomingMessage(message("será que estou melhorando?"));

        verify(studentPerformanceFlowHandler).send(session, student);
    }

    @Test
    @DisplayName("O menu principal deve anunciar a opção de desempenho")
    void mainMenuShouldAnnouncePerformanceOption() {
        ChatSessionState session = registeredSession(ChatState.MAIN_MENU);
        when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));

        orchestrator.processIncomingMessage(message("menu"));

        String sent = lastSentMessage();
        assertTrue(sent.contains("*3* - ⚙️ Configurações"));
        assertTrue(sent.contains("*4* - 📊 Meu desempenho"));
    }

    @ParameterizedTest(name = "estado {0}")
    @DisplayName("Deve delegar os estados de Configurações ao handler")
    @EnumSource(value = ChatState.class, mode = EnumSource.Mode.MATCH_ALL, names = "SETTINGS_.*")
    void shouldDelegateSettingsStatesToHandler(ChatState state) {
        ChatSessionState session = registeredSession(state);
        when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));

        orchestrator.processIncomingMessage(message("12:15"));

        verify(studentSettingsFlowHandler).handle(session, "12:15");
    }

    @Test
    @DisplayName("'menu' durante uma edição deve cancelar sem passar pelo handler")
    void shouldCancelSettingsEditWithMenuCommand() {
        ChatSessionState session = registeredSession(ChatState.SETTINGS_AWAITING_TIME);
        when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));

        orchestrator.processIncomingMessage(message("menu"));

        verify(studentSettingsFlowHandler, never()).handle(any(), anyString());
        assertEquals(ChatState.MAIN_MENU, session.getCurrentState());
        assertTrue(lastSentMessage().contains("Menu Principal"));
    }

    @Test
    @DisplayName("Deve abrir Configurações quando a intenção classificada for OPEN_SETTINGS")
    void shouldOpenSettingsFromIntent() {
        ChatSessionState session = registeredSession(ChatState.MAIN_MENU);
        when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));
        when(intentRouterService.classify("quero mudar o horário do lembrete"))
                .thenReturn(IntentResultDto.of(ChatIntent.OPEN_SETTINGS));

        orchestrator.processIncomingMessage(message("quero mudar o horário do lembrete"));

        verify(studentSettingsFlowHandler).open(session);
    }

    @Test
    @DisplayName("Não deve abrir Configurações antes de concluir o cadastro")
    void shouldNotOpenSettingsBeforeRegistration() {
        ChatSessionState session = ChatSessionState.builder()
                .phoneNumber(PHONE)
                .currentState(ChatState.AWAITING_FULL_NAME)
                .build();
        when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));

        orchestrator.processIncomingMessage(message("config"));

        verify(studentSettingsFlowHandler, never()).open(any());
        assertEquals(ChatState.AWAITING_FULL_NAME, session.getCurrentState());
    }

    @Test
    @DisplayName("Deve buscar pendências pela data de São Paulo, não pelo fuso da JVM")
    void shouldQueryPendingReviewsUsingSaoPauloDate() {
        ChatSessionState session = registeredSession(ChatState.MAIN_MENU);
        when(chatSessionStore.find(PHONE)).thenReturn(Optional.of(session));
        when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(repetitionScheduleRepository.findPendingReviewsByStudent(any(), any(), any())).thenReturn(List.of());

        orchestrator.processIncomingMessage(message("1"));

        verify(repetitionScheduleRepository).findPendingReviewsByStudent(
                student.getId(), LocalDate.of(2026, 9, 17), ScheduleStatus.PENDING);
    }
}
