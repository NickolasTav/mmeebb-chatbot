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
import br.edu.unipam.tcc.util.StudyTimeParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static br.edu.unipam.tcc.util.ChatInputUtils.MAX_ACADEMIC_PERIOD;
import static br.edu.unipam.tcc.util.ChatInputUtils.numberedList;
import static br.edu.unipam.tcc.util.ChatInputUtils.parsePositiveInt;
import static br.edu.unipam.tcc.util.ChatInputUtils.parseSelection;

/**
 * Fluxo conversacional do menu de Configurações. Valida a entrada vinda do WhatsApp e conduz as
 * transições dos estados {@code SETTINGS_*}; regras e persistência ficam no
 * {@link StudentSettingsService}. Depois de cada alteração o menu volta na mesma mensagem, para o
 * aluno ajustar outra preferência sem gastar um envio a mais na fila anti-ban.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StudentSettingsFlowHandler {

    private static final String BACK_COMMAND = "voltar";
    private static final String REMOVE_NAME_COMMAND = "remover";
    private static final String KEEP_COURSE_COMMAND = "manter";
    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_NAME_LENGTH = 30;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final ChatSessionStore chatSessionStore;
    private final OutgoingMessagePublisher outgoingMessagePublisher;
    private final StudentSettingsService studentSettingsService;
    private final CourseRepository courseRepository;

    /**
     * Abre o menu de Configurações. Rascunhos de uma edição abandonada (por exemplo, o aluno saiu
     * com "menu" no meio da troca de curso) são descartados para nunca serem gravados por engano.
     */
    public void open(ChatSessionState session) {
        session.clearOnboardingDraft();
        transitionTo(session, ChatState.SETTINGS_MENU);
        send(session, settingsMenu(session));
    }

    public void handle(ChatSessionState session, String rawText) {
        String text = rawText == null ? "" : rawText.trim();

        if (session.getCurrentState() != ChatState.SETTINGS_MENU && BACK_COMMAND.equalsIgnoreCase(text)) {
            open(session);
            return;
        }

        switch (session.getCurrentState()) {
            case SETTINGS_MENU -> handleMenuOption(session, text);
            case SETTINGS_AWAITING_NAME -> handleNameInput(session, text);
            case SETTINGS_AWAITING_TIME -> handleTimeInput(session, text);
            case SETTINGS_AWAITING_COURSE -> handleCourseInput(session, text);
            case SETTINGS_AWAITING_PERIOD -> handlePeriodInput(session, text);
            default -> throw new IllegalStateException(
                    "Estado fora do fluxo de configurações: " + session.getCurrentState());
        }
    }

    // =========================================================================
    // Menu
    // =========================================================================

    private void handleMenuOption(ChatSessionState session, String text) {
        switch (text) {
            case "1" -> {
                transitionTo(session, ChatState.SETTINGS_AWAITING_NAME);
                send(session, """
                        ✏️ *Como você quer ser chamado?*

                        Envie o nome ou apelido que eu devo usar nas mensagens (até 30 caracteres).

                        _Exemplo: Mari_
                        _Envie *remover* para eu voltar a usar seu primeiro nome, ou *voltar* para cancelar._""");
            }
            case "2" -> {
                transitionTo(session, ChatState.SETTINGS_AWAITING_TIME);
                send(session, """
                        ⏰ *Que horas você quer receber o lembrete de revisões?*

                        Envie o horário no formato 24h. _Exemplos: *07:30*, *12:15*, *18h*, *21h45*_

                        _Envie *voltar* para cancelar._""");
            }
            case "3" -> {
                boolean enabled = studentSettingsService.toggleReviewNotifications(session.getStudentId());
                String confirmation = enabled
                        ? "🔔 *Lembretes ativados!* Você volta a receber o aviso diário de revisões."
                        : "🔕 *Lembretes pausados.* Você não receberá o aviso diário até ativá-los de novo.";
                send(session, confirmation + "\n\n" + settingsMenu(session));
            }
            case "4" -> startCourseChange(session);
            default -> send(session, "⚠️ Opção inválida.\n\n" + settingsMenu(session));
        }
    }

    // =========================================================================
    // Nome a ser chamado
    // =========================================================================

    private void handleNameInput(ChatSessionState session, String text) {
        if (REMOVE_NAME_COMMAND.equalsIgnoreCase(text)) {
            studentSettingsService.updatePreferredName(session.getStudentId(), null);
            backToMenu(session, "✅ Pronto! Vou voltar a chamar você pelo primeiro nome.");
            return;
        }

        String name = text.replaceAll("\\s+", " ");
        boolean validLength = name.length() >= MIN_NAME_LENGTH && name.length() <= MAX_NAME_LENGTH;
        if (!validLength || !name.matches(".*\\p{L}.*")) {
            send(session, """
                    ⚠️ O nome precisa ter entre *2* e *30* caracteres e conter letras.

                    _Tente de novo ou envie *voltar* para cancelar._""");
            return;
        }

        studentSettingsService.updatePreferredName(session.getStudentId(), name);
        backToMenu(session, "✅ Pronto! A partir de agora vou chamar você de *" + name + "*.");
    }

    // =========================================================================
    // Horário do lembrete
    // =========================================================================

    private void handleTimeInput(ChatSessionState session, String text) {
        Optional<LocalTime> parsed = StudyTimeParser.parse(text);
        if (parsed.isEmpty()) {
            send(session, """
                    ⚠️ Não entendi esse horário. Envie no formato 24h, entre *00:00* e *23:59*.

                    _Exemplos: *07:30*, *12:15*, *18h*, *21h45*. Ou envie *voltar* para cancelar._""");
            return;
        }

        LocalTime studyTime = parsed.get();
        boolean firesToday = studentSettingsService.updateStudyTime(session.getStudentId(), studyTime);

        StringBuilder confirmation = new StringBuilder("✅ Lembrete diário ajustado! Você vai recebê-lo a partir das *")
                .append(studyTime.format(TIME_FORMAT))
                .append("*.");
        if (!firesToday) {
            confirmation.append("\n_Como esse horário já passou hoje, o próximo lembrete chega amanhã._");
        }

        backToMenu(session, confirmation.toString());
    }

    // =========================================================================
    // Curso e período
    // =========================================================================

    private void startCourseChange(ChatSessionState session) {
        List<Course> courses = courseRepository.findByActiveTrue();
        if (courses.isEmpty()) {
            send(session, "⚠️ Não há cursos disponíveis no momento.\n\n" + settingsMenu(session));
            return;
        }

        StudentProfileDto profile = studentSettingsService.getProfile(session.getStudentId());
        List<String> labels = courses.stream()
                .map(course -> course.getId().equals(profile.courseId())
                        ? course.getName() + " _(atual)_"
                        : course.getName())
                .toList();

        String hint = profile.courseId() != null
                ? "_Envie o número, *manter* para continuar no curso atual e só mudar o período, ou *voltar* para cancelar._"
                : "_Envie o número do curso ou *voltar* para cancelar._";

        transitionTo(session, ChatState.SETTINGS_AWAITING_COURSE);
        send(session, "🎓 *Selecione o seu curso:*\n\n" + numberedList(labels) + "\n" + hint);
    }

    private void handleCourseInput(ChatSessionState session, String text) {
        Long courseId;
        String courseName;

        if (KEEP_COURSE_COMMAND.equalsIgnoreCase(text)) {
            StudentProfileDto profile = studentSettingsService.getProfile(session.getStudentId());
            if (profile.courseId() == null) {
                send(session, "⚠️ Você ainda não tem curso ativo. Envie o *número* do curso da lista ou *voltar* para cancelar.");
                return;
            }
            courseId = profile.courseId();
            courseName = profile.courseName();
        } else {
            Optional<Course> chosen = parseSelection(text, courseRepository.findByActiveTrue());
            if (chosen.isEmpty()) {
                send(session, "⚠️ Opção inválida. Envie o *número* do curso da lista ou *voltar* para cancelar.");
                return;
            }
            courseId = chosen.get().getId();
            courseName = chosen.get().getName();
        }

        session.setDraftCourseId(courseId);
        transitionTo(session, ChatState.SETTINGS_AWAITING_PERIOD);
        send(session, "Curso *" + courseName + "* ✅\n\nEm qual *período* você está? "
                + "_(envie apenas o número, entre 1 e " + MAX_ACADEMIC_PERIOD + ")_");
    }

    private void handlePeriodInput(ChatSessionState session, String text) {
        Integer period = parsePositiveInt(text);
        if (period == null || period > MAX_ACADEMIC_PERIOD) {
            send(session, "⚠️ Período inválido. Envie apenas o número do período, entre *1* e *"
                    + MAX_ACADEMIC_PERIOD + "*.");
            return;
        }

        Long courseId = session.getDraftCourseId();
        if (courseId == null) {
            open(session);
            return;
        }

        try {
            studentSettingsService.changeEnrollment(session.getStudentId(), courseId, period);
        } catch (ResourceNotFoundException e) {
            log.warn("[SettingsFlow] Curso {} indisponível na troca de matrícula de [{}]: {}",
                    courseId, session.getPhoneNumber(), e.getMessage());
            session.clearOnboardingDraft();
            backToMenu(session, "⚠️ Esse curso não está mais disponível. Nada foi alterado.");
            return;
        }

        session.setSelectedCourseId(courseId);
        session.setSelectedSubjectId(null);
        session.clearOnboardingDraft();
        backToMenu(session, "✅ Matrícula atualizada para o *" + period + "º período*.");
    }

    // =========================================================================
    // Auxiliares
    // =========================================================================

    private void backToMenu(ChatSessionState session, String confirmation) {
        transitionTo(session, ChatState.SETTINGS_MENU);
        send(session, confirmation + "\n\n" + settingsMenu(session));
    }

    private String settingsMenu(ChatSessionState session) {
        StudentProfileDto profile = studentSettingsService.getProfile(session.getStudentId());

        String nameLine = profile.preferredName() != null
                ? profile.fullName() + " _(chamo você de *" + profile.preferredName() + "*)_"
                : profile.fullName();
        String courseLine = profile.courseName() == null
                ? "sem matrícula ativa"
                : profile.courseName()
                        + (profile.academicPeriod() != null ? " — " + profile.academicPeriod() + "º período" : "");
        String reminderLine = "*" + profile.preferredStudyTime().format(TIME_FORMAT) + "* "
                + (profile.reviewNotificationsEnabled() ? "(ativo)" : "(pausado)");
        String toggleOption = profile.reviewNotificationsEnabled() ? "🔕 Pausar lembretes" : "🔔 Ativar lembretes";

        return String.format("""
                ⚙️ *Configurações*

                👤 *Nome:* %s
                🎓 *RA:* %s
                📚 *Curso:* %s
                ⏰ *Lembrete diário:* %s

                *1* - ✏️ Nome a ser chamado
                *2* - ⏰ Horário do lembrete
                *3* - %s
                *4* - 🎓 Curso e período

                _Digite o número ou *menu* para voltar._""",
                nameLine,
                profile.ra() != null ? profile.ra() : "não informado",
                courseLine,
                reminderLine,
                toggleOption);
    }

    private void transitionTo(ChatSessionState session, ChatState newState) {
        session.setCurrentState(newState);
        chatSessionStore.save(session);
    }

    private void send(ChatSessionState session, String message) {
        outgoingMessagePublisher.publish(session.getPhoneNumber(), message);
    }
}
