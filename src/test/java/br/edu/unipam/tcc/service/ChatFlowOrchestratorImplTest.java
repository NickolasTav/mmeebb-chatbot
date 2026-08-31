package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.dto.UazapiWebhookDto;
import br.edu.unipam.tcc.entity.*;
import br.edu.unipam.tcc.entity.enums.ChatState;
import br.edu.unipam.tcc.entity.enums.DifficultyLevel;
import br.edu.unipam.tcc.entity.enums.QuestionType;
import br.edu.unipam.tcc.entity.enums.ScheduleStatus;
import br.edu.unipam.tcc.repository.*;
import br.edu.unipam.tcc.service.impl.ChatFlowOrchestratorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatFlowOrchestratorImplTest {

    @Mock
    private ChatSessionRepository chatSessionRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private FlashcardRepository flashcardRepository;

    @Mock
    private RepetitionScheduleRepository repetitionScheduleRepository;

    @Mock
    private MmeebbService mmeebbService;

    @Mock
    private UazapiClientService uazapiClientService;

    @InjectMocks
    private ChatFlowOrchestratorImpl orchestrator;

    private Student mockStudent;
    private ChatSession mockSession;
    private Flashcard mockFlashcard;
    private RepetitionSchedule mockSchedule;
    private final String phone = "5534999998888";

    @BeforeEach
    void setUp() {
        mockStudent = Student.builder()
                .id(UUID.randomUUID())
                .phoneNumber(phone)
                .fullName("Estudante Teste")
                .active(true)
                .build();

        mockSession = ChatSession.builder()
                .id(UUID.randomUUID())
                .student(mockStudent)
                .phoneNumber(phone)
                .currentState(ChatState.MAIN_MENU)
                .lastInteractionAt(LocalDateTime.now())
                .build();

        Subject mockSubject = Subject.builder()
                .id(1L)
                .code("CLINICA_MEDICA")
                .name("Clínica Médica")
                .build();

        mockFlashcard = Flashcard.builder()
                .id(10L)
                .subject(mockSubject)
                .topic("Cardiologia")
                .questionType(QuestionType.FLASHCARD)
                .question("Qual a principal causa de ICC?")
                .answer("Hipertensão Arterial Sistêmica")
                .explanation("HAS e DAC são as principais causas.")
                .difficulty(DifficultyLevel.MEDIUM)
                .build();

        mockSchedule = RepetitionSchedule.builder()
                .id(100L)
                .student(mockStudent)
                .flashcard(mockFlashcard)
                .nIndex(0)
                .intervalDays(1)
                .repetitionCount(0)
                .consecutiveCorrect(0)
                .status(ScheduleStatus.PENDING)
                .nextReviewDate(LocalDate.now())
                .build();
    }

    @Test
    @DisplayName("Deve ignorar mensagem quando payload for nulo ou remetente for a própria instância (fromMe)")
    void shouldIgnoreNullOrFromMeMessages() {
        orchestrator.processIncomingMessage(null);
        orchestrator.processIncomingMessage(new UazapiWebhookDto("5534999998888@s.whatsapp.net", true, "Oi", "instancia", "msg-1"));

        verifyNoInteractions(chatSessionRepository, uazapiClientService);
    }

    @Test
    @DisplayName("Deve criar Student e ChatSession para novo contato e transicionar para MAIN_MENU enviando boas-vindas")
    void shouldHandleNewContactCreateSessionAndSendWelcomeMenu() {
        UazapiWebhookDto webhookDto = new UazapiWebhookDto(phone + "@s.whatsapp.net", false, "Olá", "instancia", "msg-1");

        when(chatSessionRepository.findByPhoneNumber(phone)).thenReturn(Optional.empty());
        when(studentRepository.findByPhoneNumber(phone)).thenReturn(Optional.empty());
        when(studentRepository.save(any(Student.class))).thenReturn(mockStudent);
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orchestrator.processIncomingMessage(webhookDto);

        verify(studentRepository).save(any(Student.class));
        verify(chatSessionRepository, atLeastOnce()).save(any(ChatSession.class));

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(uazapiClientService).sendTextMessage(eq(phone), messageCaptor.capture());
        assertTrue(messageCaptor.getValue().contains("Bem-vindo ao Chatbot MMEEBB"));
        assertTrue(messageCaptor.getValue().contains("Modo Revisão MMEEBB"));
    }

    @Test
    @DisplayName("Deve resetar para MAIN_MENU quando receber comando global como 'menu' ou 'sair'")
    void shouldResetToMainMenuWhenGlobalResetCommandReceived() {
        mockSession.setCurrentState(ChatState.REVIEW_MODE);
        mockSession.setCurrentFlashcard(mockFlashcard);

        UazapiWebhookDto webhookDto = new UazapiWebhookDto(phone + "@s.whatsapp.net", false, "menu", "instancia", "msg-2");

        when(chatSessionRepository.findByPhoneNumber(phone)).thenReturn(Optional.of(mockSession));
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orchestrator.processIncomingMessage(webhookDto);

        assertEquals(ChatState.MAIN_MENU, mockSession.getCurrentState());
        assertNull(mockSession.getCurrentFlashcard());
        verify(chatSessionRepository).save(mockSession);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(uazapiClientService).sendTextMessage(eq(phone), messageCaptor.capture());
        assertTrue(messageCaptor.getValue().contains("Menu Principal"));
    }

    @Test
    @DisplayName("Deve iniciar MODO_REVISAO_MMEEBB e enviar primeiro flashcard ao selecionar opção 1 no Menu")
    void shouldTransitionToReviewModeAndSendFirstFlashcardWhenOption1Selected() {
        mockSession.setCurrentState(ChatState.MAIN_MENU);

        UazapiWebhookDto webhookDto = new UazapiWebhookDto(phone + "@s.whatsapp.net", false, "1", "instancia", "msg-3");

        when(chatSessionRepository.findByPhoneNumber(phone)).thenReturn(Optional.of(mockSession));
        when(repetitionScheduleRepository.findPendingReviewsByStudent(eq(mockStudent.getId()), any(LocalDate.class), eq(ScheduleStatus.PENDING)))
                .thenReturn(List.of(mockSchedule));
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orchestrator.processIncomingMessage(webhookDto);

        assertEquals(ChatState.REVIEW_MODE, mockSession.getCurrentState());
        assertEquals(mockFlashcard, mockSession.getCurrentFlashcard());

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(uazapiClientService).sendTextMessage(eq(phone), messageCaptor.capture());
        assertTrue(messageCaptor.getValue().contains("Revisão MMEEBB"));
        assertTrue(messageCaptor.getValue().contains("Cardiologia"));
        assertTrue(messageCaptor.getValue().contains("Qual a principal causa de ICC?"));
    }

    @Test
    @DisplayName("Deve avisar quando não houver flashcards pendentes no momento ao selecionar opção 1")
    void shouldNotifyNoPendingCardsWhenOption1SelectedAndDeckIsEmpty() {
        mockSession.setCurrentState(ChatState.MAIN_MENU);

        UazapiWebhookDto webhookDto = new UazapiWebhookDto(phone + "@s.whatsapp.net", false, "1", "instancia", "msg-4");

        when(chatSessionRepository.findByPhoneNumber(phone)).thenReturn(Optional.of(mockSession));
        when(repetitionScheduleRepository.findPendingReviewsByStudent(eq(mockStudent.getId()), any(LocalDate.class), eq(ScheduleStatus.PENDING)))
                .thenReturn(Collections.emptyList());

        orchestrator.processIncomingMessage(webhookDto);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(uazapiClientService).sendTextMessage(eq(phone), messageCaptor.capture());
        assertTrue(messageCaptor.getValue().contains("não possui flashcards pendentes"));
    }

    @Test
    @DisplayName("Deve processar resposta correta no MODO_REVISAO_MMEEBB, duplicar intervalo e concluir quando sem mais cards")
    void shouldProcessCorrectAnswerAndCompleteReviewWhenNoMoreCards() {
        mockSession.setCurrentState(ChatState.REVIEW_MODE);
        mockSession.setCurrentFlashcard(mockFlashcard);

        UazapiWebhookDto webhookDto = new UazapiWebhookDto(phone + "@s.whatsapp.net", false, "Hipertensão Arterial Sistêmica", "instancia", "msg-5");

        RepetitionSchedule updatedSchedule = RepetitionSchedule.builder()
                .id(100L)
                .student(mockStudent)
                .flashcard(mockFlashcard)
                .nIndex(1)
                .intervalDays(2)
                .consecutiveCorrect(1)
                .repetitionCount(1)
                .status(ScheduleStatus.PENDING)
                .nextReviewDate(LocalDate.now().plusDays(2))
                .build();

        when(chatSessionRepository.findByPhoneNumber(phone)).thenReturn(Optional.of(mockSession));
        when(repetitionScheduleRepository.findByStudentIdAndFlashcardId(mockStudent.getId(), mockFlashcard.getId()))
                .thenReturn(Optional.of(mockSchedule));
        when(mmeebbService.processAnswer(eq(mockSchedule), eq(true), any(LocalDateTime.class)))
                .thenReturn(updatedSchedule);
        when(repetitionScheduleRepository.findPendingReviewsByStudent(eq(mockStudent.getId()), any(LocalDate.class), eq(ScheduleStatus.PENDING)))
                .thenReturn(Collections.emptyList());
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orchestrator.processIncomingMessage(webhookDto);

        verify(repetitionScheduleRepository).save(updatedSchedule);
        assertEquals(ChatState.MAIN_MENU, mockSession.getCurrentState());
        assertNull(mockSession.getCurrentFlashcard());

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(uazapiClientService).sendTextMessage(eq(phone), messageCaptor.capture());
        assertTrue(messageCaptor.getValue().contains("Resposta Correta"));
        assertTrue(messageCaptor.getValue().contains("2 dias"));
        assertTrue(messageCaptor.getValue().contains("concluídas"));
    }

    @Test
    @DisplayName("Deve processar resposta incorreta no MODO_REVISAO_MMEEBB, resetar intervalo para 1 dia e enviar feedback")
    void shouldProcessIncorrectAnswerAndResetIntervalToOneDay() {
        mockSession.setCurrentState(ChatState.REVIEW_MODE);
        mockSession.setCurrentFlashcard(mockFlashcard);

        UazapiWebhookDto webhookDto = new UazapiWebhookDto(phone + "@s.whatsapp.net", false, "Diabetes Mellitus", "instancia", "msg-6");

        RepetitionSchedule updatedSchedule = RepetitionSchedule.builder()
                .id(100L)
                .student(mockStudent)
                .flashcard(mockFlashcard)
                .nIndex(0)
                .intervalDays(1)
                .consecutiveCorrect(0)
                .repetitionCount(1)
                .status(ScheduleStatus.PENDING)
                .nextReviewDate(LocalDate.now().plusDays(1))
                .build();

        when(chatSessionRepository.findByPhoneNumber(phone)).thenReturn(Optional.of(mockSession));
        when(repetitionScheduleRepository.findByStudentIdAndFlashcardId(mockStudent.getId(), mockFlashcard.getId()))
                .thenReturn(Optional.of(mockSchedule));
        when(mmeebbService.processAnswer(eq(mockSchedule), eq(false), any(LocalDateTime.class)))
                .thenReturn(updatedSchedule);
        when(repetitionScheduleRepository.findPendingReviewsByStudent(eq(mockStudent.getId()), any(LocalDate.class), eq(ScheduleStatus.PENDING)))
                .thenReturn(Collections.emptyList());
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orchestrator.processIncomingMessage(webhookDto);

        verify(repetitionScheduleRepository).save(updatedSchedule);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(uazapiClientService).sendTextMessage(eq(phone), messageCaptor.capture());
        assertTrue(messageCaptor.getValue().contains("Resposta Incorreta"));
        assertTrue(messageCaptor.getValue().contains("Hipertensão Arterial Sistêmica"));
        assertTrue(messageCaptor.getValue().contains("1 dia"));
    }

    @Test
    @DisplayName("Deve transicionar para MODO_RAG_DUVIDAS ao selecionar opção 2 no Menu")
    void shouldTransitionToRagDoubtModeWhenOption2Selected() {
        mockSession.setCurrentState(ChatState.MAIN_MENU);

        UazapiWebhookDto webhookDto = new UazapiWebhookDto(phone + "@s.whatsapp.net", false, "2", "instancia", "msg-7");

        when(chatSessionRepository.findByPhoneNumber(phone)).thenReturn(Optional.of(mockSession));
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orchestrator.processIncomingMessage(webhookDto);

        assertEquals(ChatState.RAG_DOUBT_MODE, mockSession.getCurrentState());

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(uazapiClientService).sendTextMessage(eq(phone), messageCaptor.capture());
        assertTrue(messageCaptor.getValue().contains("Modo Dúvidas e RAG"));
    }

    @Test
    @DisplayName("Deve transicionar para SELECTING_COURSE e listar cursos ativos ao selecionar opção 3 no Menu")
    void shouldTransitionToSelectingCourseWhenOption3Selected() {
        mockSession.setCurrentState(ChatState.MAIN_MENU);

        Course course1 = Course.builder().id(1L).name("Medicina").code("MED").build();
        Course course2 = Course.builder().id(2L).name("Sistemas de Informação").code("SI").build();

        UazapiWebhookDto webhookDto = new UazapiWebhookDto(phone + "@s.whatsapp.net", false, "3", "instancia", "msg-8");

        when(chatSessionRepository.findByPhoneNumber(phone)).thenReturn(Optional.of(mockSession));
        when(courseRepository.findByActiveTrue()).thenReturn(List.of(course1, course2));
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orchestrator.processIncomingMessage(webhookDto);

        assertEquals(ChatState.SELECTING_COURSE, mockSession.getCurrentState());

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(uazapiClientService).sendTextMessage(eq(phone), messageCaptor.capture());
        assertTrue(messageCaptor.getValue().contains("Selecione seu Curso"));
        assertTrue(messageCaptor.getValue().contains("Medicina"));
        assertTrue(messageCaptor.getValue().contains("Sistemas de Informação"));
    }

    @Test
    @DisplayName("Deve processar seleção de curso válida e listar disciplinas no estado SELECTING_COURSE")
    void shouldHandleValidCourseSelectionAndListSubjects() {
        mockSession.setCurrentState(ChatState.SELECTING_COURSE);

        Course course1 = Course.builder().id(1L).name("Medicina").code("MED").build();
        Subject subject1 = Subject.builder().id(10L).name("Semiologia Médica").code("SEM").build();

        UazapiWebhookDto webhookDto = new UazapiWebhookDto(phone + "@s.whatsapp.net", false, "1", "instancia", "msg-9");

        when(chatSessionRepository.findByPhoneNumber(phone)).thenReturn(Optional.of(mockSession));
        when(courseRepository.findByActiveTrue()).thenReturn(List.of(course1));
        when(subjectRepository.findByCourseIdAndActiveTrue(1L)).thenReturn(List.of(subject1));
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orchestrator.processIncomingMessage(webhookDto);

        assertEquals(course1, mockSession.getSelectedCourse());
        assertEquals(ChatState.SELECTING_SUBJECT, mockSession.getCurrentState());

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(uazapiClientService).sendTextMessage(eq(phone), messageCaptor.capture());
        assertTrue(messageCaptor.getValue().contains("Selecione a Disciplina"));
        assertTrue(messageCaptor.getValue().contains("Semiologia Médica"));
    }

    @Test
    @DisplayName("Deve processar seleção de disciplina válida e retornar ao MAIN_MENU")
    void shouldHandleValidSubjectSelectionAndReturnToMainMenu() {
        Course course1 = Course.builder().id(1L).name("Medicina").code("MED").build();
        Subject subject1 = Subject.builder().id(10L).name("Semiologia Médica").code("SEM").build();

        mockSession.setCurrentState(ChatState.SELECTING_SUBJECT);
        mockSession.setSelectedCourse(course1);

        UazapiWebhookDto webhookDto = new UazapiWebhookDto(phone + "@s.whatsapp.net", false, "1", "instancia", "msg-10");

        when(chatSessionRepository.findByPhoneNumber(phone)).thenReturn(Optional.of(mockSession));
        when(subjectRepository.findByCourseIdAndActiveTrue(1L)).thenReturn(List.of(subject1));
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orchestrator.processIncomingMessage(webhookDto);

        assertEquals(subject1, mockSession.getSelectedSubject());
        assertEquals(ChatState.MAIN_MENU, mockSession.getCurrentState());

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(uazapiClientService).sendTextMessage(eq(phone), messageCaptor.capture());
        assertTrue(messageCaptor.getValue().contains("Semiologia Médica"));
        assertTrue(messageCaptor.getValue().contains("Menu Principal"));
    }

    @Test
    @DisplayName("Deve enviar mensagem de opção inválida quando texto não reconhecido for enviado no MAIN_MENU")
    void shouldSendInvalidOptionMessageWhenUnknownOptionSentInMainMenu() {
        mockSession.setCurrentState(ChatState.MAIN_MENU);

        UazapiWebhookDto webhookDto = new UazapiWebhookDto(phone + "@s.whatsapp.net", false, "opção desconhecida", "instancia", "msg-11");

        when(chatSessionRepository.findByPhoneNumber(phone)).thenReturn(Optional.of(mockSession));

        orchestrator.processIncomingMessage(webhookDto);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(uazapiClientService).sendTextMessage(eq(phone), messageCaptor.capture());
        assertTrue(messageCaptor.getValue().contains("Opção não reconhecida"));
    }
}
