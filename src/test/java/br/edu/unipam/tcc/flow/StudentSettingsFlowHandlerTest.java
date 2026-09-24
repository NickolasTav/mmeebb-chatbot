package br.edu.unipam.tcc.flow;

import br.edu.unipam.tcc.dto.StudentProfileDto;
import br.edu.unipam.tcc.entity.Course;
import br.edu.unipam.tcc.entity.enums.ChatState;
import br.edu.unipam.tcc.exception.ResourceNotFoundException;
import br.edu.unipam.tcc.messaging.OutgoingMessagePublisher;
import br.edu.unipam.tcc.repository.CourseRepository;
import br.edu.unipam.tcc.service.StudentSettingsService;
import br.edu.unipam.tcc.session.ChatSessionState;
import br.edu.unipam.tcc.session.ChatSessionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StudentSettingsFlowHandlerTest {

    private static final String PHONE = "5534999998888";
    private static final UUID STUDENT_ID = UUID.randomUUID();

    @Mock private ChatSessionStore chatSessionStore;
    @Mock private OutgoingMessagePublisher outgoingMessagePublisher;
    @Mock private StudentSettingsService studentSettingsService;
    @Mock private CourseRepository courseRepository;

    @InjectMocks private StudentSettingsFlowHandler handler;

    private Course medicina;
    private Course sistemas;

    @BeforeEach
    void setUp() {
        medicina = Course.builder().code("MED").name("Medicina").active(true).build();
        medicina.setId(1L);
        sistemas = Course.builder().code("SI").name("Sistemas de Informação").active(true).build();
        sistemas.setId(2L);

        when(courseRepository.findByActiveTrue()).thenReturn(List.of(medicina, sistemas));
        stubProfile("Mari", "23000388", 1L, "Medicina", 8, true);
    }

    private void stubProfile(String preferredName, String ra, Long courseId, String courseName,
                             Integer period, boolean enabled) {
        when(studentSettingsService.getProfile(STUDENT_ID)).thenReturn(new StudentProfileDto(
                "Maria Silva Andrade",
                preferredName,
                preferredName != null ? preferredName : "Maria",
                ra, courseId, courseName, period, LocalTime.of(12, 15), enabled));
    }

    private ChatSessionState session(ChatState state) {
        return ChatSessionState.builder()
                .phoneNumber(PHONE).studentId(STUDENT_ID).selectedCourseId(1L).currentState(state).build();
    }

    private String lastSentMessage() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(outgoingMessagePublisher, atLeastOnce()).publish(eq(PHONE), captor.capture());
        return captor.getValue();
    }

    // ============================================================ menu

    @Test
    @DisplayName("Deve abrir o menu com os dados do aluno e descartar rascunhos antigos")
    void deveAbrirMenuComDadosDoAluno() {
        ChatSessionState session = session(ChatState.MAIN_MENU);
        session.setDraftCourseId(2L);

        handler.open(session);

        assertEquals(ChatState.SETTINGS_MENU, session.getCurrentState());
        assertNull(session.getDraftCourseId());
        verify(chatSessionStore).save(session);

        String sent = lastSentMessage();
        assertTrue(sent.contains("Configurações"));
        assertTrue(sent.contains("Maria Silva Andrade"));
        assertTrue(sent.contains("chamo você de *Mari*"));
        assertTrue(sent.contains("23000388"));
        assertTrue(sent.contains("Medicina — 8º período"));
        assertTrue(sent.contains("*12:15* (ativo)"));
        assertTrue(sent.contains("Pausar lembretes"));
    }

    @Test
    @DisplayName("Deve oferecer 'Ativar lembretes' quando estiverem pausados")
    void deveOferecerAtivacaoQuandoPausado() {
        stubProfile("Mari", "23000388", 1L, "Medicina", 8, false);

        handler.open(session(ChatState.MAIN_MENU));

        String sent = lastSentMessage();
        assertTrue(sent.contains("(pausado)"));
        assertTrue(sent.contains("Ativar lembretes"));
        assertFalse(sent.contains("Pausar lembretes"));
    }

    @Test
    @DisplayName("Deve exibir textos padrão quando faltarem apelido, RA e matrícula")
    void deveExibirTextosPadraoQuandoFaltaremDados() {
        stubProfile(null, null, null, null, null, true);

        handler.open(session(ChatState.MAIN_MENU));

        String sent = lastSentMessage();
        assertFalse(sent.contains("chamo você de"));
        assertTrue(sent.contains("não informado"));
        assertTrue(sent.contains("sem matrícula ativa"));
    }

    @Test
    @DisplayName("Deve avisar opção inválida e reenviar o menu")
    void deveRecusarOpcaoInvalidaDoMenu() {
        ChatSessionState session = session(ChatState.SETTINGS_MENU);

        handler.handle(session, "9");

        assertEquals(ChatState.SETTINGS_MENU, session.getCurrentState());
        String sent = lastSentMessage();
        assertTrue(sent.contains("Opção inválida"));
        assertTrue(sent.contains("Configurações"));
    }

    // ============================================================ apelido

    @Test
    @DisplayName("Opção 1 deve pedir o nome a ser chamado")
    void devePedirNomeDeTratamento() {
        ChatSessionState session = session(ChatState.SETTINGS_MENU);

        handler.handle(session, "1");

        assertEquals(ChatState.SETTINGS_AWAITING_NAME, session.getCurrentState());
        assertTrue(lastSentMessage().contains("Como você quer ser chamado"));
    }

    @Test
    @DisplayName("Deve salvar apelido normalizado e voltar ao menu de Configurações")
    void deveSalvarApelidoNormalizado() {
        ChatSessionState session = session(ChatState.SETTINGS_AWAITING_NAME);

        handler.handle(session, "  Mari    Andrade ");

        verify(studentSettingsService).updatePreferredName(STUDENT_ID, "Mari Andrade");
        assertEquals(ChatState.SETTINGS_MENU, session.getCurrentState());
        String sent = lastSentMessage();
        assertTrue(sent.contains("vou chamar você de *Mari Andrade*"));
        assertTrue(sent.contains("Configurações"));
    }

    @ParameterizedTest(name = "apelido inválido: \"{0}\"")
    @DisplayName("Deve recusar apelido curto, longo ou sem letras")
    @ValueSource(strings = {"A", "1234", "!!", "Maria Silva Andrade de Souza Lima"})
    void deveRecusarApelidoInvalido(String input) {
        ChatSessionState session = session(ChatState.SETTINGS_AWAITING_NAME);

        handler.handle(session, input);

        verify(studentSettingsService, never()).updatePreferredName(any(), any());
        assertEquals(ChatState.SETTINGS_AWAITING_NAME, session.getCurrentState());
        assertTrue(lastSentMessage().contains("entre *2* e *30* caracteres"));
    }

    @Test
    @DisplayName("'remover' deve apagar o apelido")
    void deveRemoverApelido() {
        ChatSessionState session = session(ChatState.SETTINGS_AWAITING_NAME);

        handler.handle(session, "Remover");

        verify(studentSettingsService).updatePreferredName(STUDENT_ID, null);
        assertEquals(ChatState.SETTINGS_MENU, session.getCurrentState());
        assertTrue(lastSentMessage().contains("primeiro nome"));
    }

    // ============================================================ horário

    @Test
    @DisplayName("Opção 2 deve pedir o horário com exemplos")
    void devePedirHorarioComExemplos() {
        ChatSessionState session = session(ChatState.SETTINGS_MENU);

        handler.handle(session, "2");

        assertEquals(ChatState.SETTINGS_AWAITING_TIME, session.getCurrentState());
        assertTrue(lastSentMessage().contains("12:15"));
    }

    @Test
    @DisplayName("Deve salvar horário flexível e confirmar sem aviso de amanhã quando ainda sai hoje")
    void deveSalvarHorarioQueAindaSaiHoje() {
        ChatSessionState session = session(ChatState.SETTINGS_AWAITING_TIME);
        when(studentSettingsService.updateStudyTime(STUDENT_ID, LocalTime.of(12, 15))).thenReturn(true);

        handler.handle(session, "12h15");

        verify(studentSettingsService).updateStudyTime(STUDENT_ID, LocalTime.of(12, 15));
        assertEquals(ChatState.SETTINGS_MENU, session.getCurrentState());
        String sent = lastSentMessage();
        assertTrue(sent.contains("a partir das *12:15*"));
        assertFalse(sent.contains("amanhã"));
    }

    @Test
    @DisplayName("Deve avisar que o lembrete começa amanhã quando o horário já passou")
    void deveAvisarQueLembreteComecaAmanha() {
        ChatSessionState session = session(ChatState.SETTINGS_AWAITING_TIME);
        when(studentSettingsService.updateStudyTime(STUDENT_ID, LocalTime.of(7, 5))).thenReturn(false);

        handler.handle(session, "7:05");

        assertTrue(lastSentMessage().contains("amanhã"));
    }

    @ParameterizedTest(name = "horário inválido: \"{0}\"")
    @DisplayName("Deve recusar horário inválido e continuar aguardando")
    @ValueSource(strings = {"25:00", "12:60", "7:5", "amanhã cedo"})
    void deveRecusarHorarioInvalido(String input) {
        ChatSessionState session = session(ChatState.SETTINGS_AWAITING_TIME);

        handler.handle(session, input);

        verify(studentSettingsService, never()).updateStudyTime(any(), any());
        assertEquals(ChatState.SETTINGS_AWAITING_TIME, session.getCurrentState());
        assertTrue(lastSentMessage().contains("Não entendi esse horário"));
    }

    @Test
    @DisplayName("'voltar' durante uma edição deve retornar ao menu sem salvar")
    void deveVoltarSemSalvar() {
        ChatSessionState session = session(ChatState.SETTINGS_AWAITING_TIME);

        handler.handle(session, "voltar");

        verify(studentSettingsService, never()).updateStudyTime(any(), any());
        assertEquals(ChatState.SETTINGS_MENU, session.getCurrentState());
        assertTrue(lastSentMessage().contains("Configurações"));
    }

    // ============================================================ pausa

    @Test
    @DisplayName("Opção 3 deve pausar lembretes e permanecer no menu")
    void devePausarLembretes() {
        ChatSessionState session = session(ChatState.SETTINGS_MENU);
        when(studentSettingsService.toggleReviewNotifications(STUDENT_ID)).thenReturn(false);

        handler.handle(session, "3");

        assertEquals(ChatState.SETTINGS_MENU, session.getCurrentState());
        assertTrue(lastSentMessage().contains("Lembretes pausados"));
    }

    @Test
    @DisplayName("Opção 3 deve reativar lembretes pausados")
    void deveReativarLembretes() {
        when(studentSettingsService.toggleReviewNotifications(STUDENT_ID)).thenReturn(true);

        handler.handle(session(ChatState.SETTINGS_MENU), "3");

        assertTrue(lastSentMessage().contains("Lembretes ativados"));
    }

    // ============================================================ curso e período

    @Test
    @DisplayName("Opção 4 deve listar cursos marcando o atual")
    void deveListarCursosMarcandoOAtual() {
        ChatSessionState session = session(ChatState.SETTINGS_MENU);

        handler.handle(session, "4");

        assertEquals(ChatState.SETTINGS_AWAITING_COURSE, session.getCurrentState());
        String sent = lastSentMessage();
        assertTrue(sent.contains("*1* - Medicina _(atual)_"));
        assertTrue(sent.contains("*2* - Sistemas de Informação"));
        assertTrue(sent.contains("*manter*"));
    }

    @Test
    @DisplayName("Deve guardar o curso escolhido como rascunho e pedir o período")
    void deveGuardarCursoEscolhidoComoRascunho() {
        ChatSessionState session = session(ChatState.SETTINGS_AWAITING_COURSE);

        handler.handle(session, "2");

        assertEquals(2L, session.getDraftCourseId());
        assertEquals(ChatState.SETTINGS_AWAITING_PERIOD, session.getCurrentState());
        assertTrue(lastSentMessage().contains("Sistemas de Informação"));
        verify(studentSettingsService, never()).changeEnrollment(any(), any(), anyInt());
    }

    @Test
    @DisplayName("'manter' deve seguir com o curso atual")
    void deveManterCursoAtual() {
        ChatSessionState session = session(ChatState.SETTINGS_AWAITING_COURSE);

        handler.handle(session, "manter");

        assertEquals(1L, session.getDraftCourseId());
        assertEquals(ChatState.SETTINGS_AWAITING_PERIOD, session.getCurrentState());
    }

    @Test
    @DisplayName("Deve recusar número de curso fora da lista")
    void deveRecusarCursoInvalido() {
        ChatSessionState session = session(ChatState.SETTINGS_AWAITING_COURSE);

        handler.handle(session, "9");

        assertEquals(ChatState.SETTINGS_AWAITING_COURSE, session.getCurrentState());
        assertTrue(lastSentMessage().contains("Opção inválida"));
    }

    @Test
    @DisplayName("Deve gravar a matrícula ao receber o período e atualizar o foco da sessão")
    void deveGravarMatriculaAoReceberPeriodo() {
        ChatSessionState session = session(ChatState.SETTINGS_AWAITING_PERIOD);
        session.setDraftCourseId(2L);
        session.setSelectedSubjectId(10L);

        handler.handle(session, "6");

        verify(studentSettingsService).changeEnrollment(STUDENT_ID, 2L, 6);
        assertEquals(2L, session.getSelectedCourseId());
        assertNull(session.getSelectedSubjectId());
        assertNull(session.getDraftCourseId());
        assertEquals(ChatState.SETTINGS_MENU, session.getCurrentState());
        assertTrue(lastSentMessage().contains("6º período"));
    }

    @ParameterizedTest(name = "período inválido: \"{0}\"")
    @DisplayName("Deve recusar período fora de 1 a 20")
    @ValueSource(strings = {"0", "21", "oitavo"})
    void deveRecusarPeriodoInvalido(String input) {
        ChatSessionState session = session(ChatState.SETTINGS_AWAITING_PERIOD);
        session.setDraftCourseId(2L);

        handler.handle(session, input);

        verify(studentSettingsService, never()).changeEnrollment(any(), any(), anyInt());
        assertEquals(ChatState.SETTINGS_AWAITING_PERIOD, session.getCurrentState());
        assertTrue(lastSentMessage().contains("Período inválido"));
    }

    @Test
    @DisplayName("Deve avisar quando o curso deixou de estar disponível e não alterar nada")
    void deveAvisarCursoIndisponivel() {
        ChatSessionState session = session(ChatState.SETTINGS_AWAITING_PERIOD);
        session.setDraftCourseId(2L);
        doThrow(new ResourceNotFoundException("inativo"))
                .when(studentSettingsService).changeEnrollment(STUDENT_ID, 2L, 6);

        handler.handle(session, "6");

        assertEquals(1L, session.getSelectedCourseId());
        assertNull(session.getDraftCourseId());
        assertEquals(ChatState.SETTINGS_MENU, session.getCurrentState());
        assertTrue(lastSentMessage().contains("não está mais disponível"));
    }

    @Test
    @DisplayName("Deve avisar quando não houver curso disponível para troca")
    void deveAvisarQuandoNaoHouverCursos() {
        when(courseRepository.findByActiveTrue()).thenReturn(List.of());
        ChatSessionState session = session(ChatState.SETTINGS_MENU);

        handler.handle(session, "4");

        assertEquals(ChatState.SETTINGS_MENU, session.getCurrentState());
        assertTrue(lastSentMessage().contains("Não há cursos disponíveis"));
        verify(outgoingMessagePublisher, atLeastOnce()).publish(eq(PHONE), anyString());
    }
}
