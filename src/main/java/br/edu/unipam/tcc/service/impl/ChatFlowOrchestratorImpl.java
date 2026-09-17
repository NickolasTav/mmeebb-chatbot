package br.edu.unipam.tcc.service.impl;

import br.edu.unipam.tcc.dto.AnswerEvaluationDto;
import br.edu.unipam.tcc.dto.IntentResultDto;
import br.edu.unipam.tcc.dto.UazapiWebhookDto;
import br.edu.unipam.tcc.entity.Course;
import br.edu.unipam.tcc.entity.Flashcard;
import br.edu.unipam.tcc.entity.RepetitionSchedule;
import br.edu.unipam.tcc.entity.Student;
import br.edu.unipam.tcc.entity.StudentCourse;
import br.edu.unipam.tcc.entity.Subject;
import br.edu.unipam.tcc.entity.enums.ChatState;
import br.edu.unipam.tcc.entity.enums.ScheduleStatus;
import br.edu.unipam.tcc.repository.CourseRepository;
import br.edu.unipam.tcc.repository.FlashcardRepository;
import br.edu.unipam.tcc.repository.RepetitionScheduleRepository;
import br.edu.unipam.tcc.repository.StudentCourseRepository;
import br.edu.unipam.tcc.repository.StudentRepository;
import br.edu.unipam.tcc.repository.SubjectRepository;
import br.edu.unipam.tcc.service.AnswerEvaluationService;
import br.edu.unipam.tcc.service.ChatFlowOrchestrator;
import br.edu.unipam.tcc.service.IntentRouterService;
import br.edu.unipam.tcc.service.MmeebbService;
import br.edu.unipam.tcc.service.StudentOnboardingService;
import br.edu.unipam.tcc.service.SubjectRagService;
import br.edu.unipam.tcc.service.UazapiClientService;
import br.edu.unipam.tcc.session.ChatSessionState;
import br.edu.unipam.tcc.session.ChatSessionStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Orquestrador do fluxo conversacional do Chatbot MMEEBB.
 * O estado da conversa vive no Redis; o Postgres guarda apenas o que é durável
 * (estudante, matrícula e agendamentos de repetição espaçada).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatFlowOrchestratorImpl implements ChatFlowOrchestrator {

    private static final Set<String> EXIT_COMMANDS = Set.of(
            "sair", "/sair", "!sair", "tchau", "tchauu", "tchauzinho",
            "encerrar", "finalizar", "fim", "ate mais", "até mais",
            "flw", "valeu", "adeus", "fechar", "parar"
    );

    private static final Set<String> RESET_COMMANDS = Set.of(
            "menu", "inicio", "início", "começo", "comeco", "reset", "reiniciar",
            "/menu", "/inicio", "/start", "/reset"
    );

    private static final Set<String> SKIP_COMMANDS = Set.of(
            "pular", "nao tenho", "não tenho", "nao sei", "não sei", "-", "skip"
    );

    private static final int MAX_ACADEMIC_PERIOD = 20;

    private final ChatSessionStore chatSessionStore;
    private final StudentRepository studentRepository;
    private final StudentCourseRepository studentCourseRepository;
    private final CourseRepository courseRepository;
    private final SubjectRepository subjectRepository;
    private final FlashcardRepository flashcardRepository;
    private final RepetitionScheduleRepository repetitionScheduleRepository;
    private final MmeebbService mmeebbService;
    private final UazapiClientService uazapiClientService;
    private final SubjectRagService subjectRagService;
    private final IntentRouterService intentRouterService;
    private final AnswerEvaluationService answerEvaluationService;
    private final StudentOnboardingService studentOnboardingService;

    @Override
    public void processIncomingMessage(UazapiWebhookDto webhookDto) {
        if (webhookDto == null) {
            log.warn("[Orchestrator] Payload nulo recebido.");
            return;
        }

        if (Boolean.TRUE.equals(webhookDto.fromMe())) {
            log.debug("[Orchestrator] Mensagem enviada pelo próprio bot ignorada (fromMe = true).");
            return;
        }

        String phoneNumber = webhookDto.getCleanPhoneNumber();
        if (phoneNumber == null || phoneNumber.isBlank()) {
            log.warn("[Orchestrator] Telefone do remetente nulo ou vazio no payload.");
            return;
        }

        String rawText = webhookDto.text() != null ? webhookDto.text().trim() : "";
        String lowerText = rawText.toLowerCase();

        log.info("[Orchestrator] Processando mensagem de [{}]: \"{}\"", phoneNumber, rawText);

        ChatSessionState session = resolveSession(phoneNumber);

        // Comandos globais só valem depois do cadastro: durante o formulário, "menu"
        // é uma resposta possível do estudante e não deve abortar o fluxo.
        if (session.isRegistered()) {
            if (EXIT_COMMANDS.contains(lowerText)) {
                handleExitCommand(session);
                return;
            }
            if (RESET_COMMANDS.contains(lowerText)) {
                handleGlobalReset(session);
                return;
            }
        }

        switch (session.getCurrentState()) {
            case NEW -> startOnboarding(session);
            case AWAITING_FULL_NAME -> handleFullNameInput(session, rawText);
            case AWAITING_RA -> handleRaInput(session, rawText, lowerText);
            case AWAITING_COURSE -> handleCourseInput(session, rawText);
            case AWAITING_ACADEMIC_PERIOD -> handleAcademicPeriodInput(session, rawText);
            case MAIN_MENU -> handleMainMenuState(session, rawText);
            case REVIEW_MODE -> handleReviewModeState(session, rawText);
            case RAG_DOUBT_MODE -> handleRagDoubtModeState(session, rawText);
            case SELECTING_COURSE -> handleSelectingCourseState(session, rawText);
            case SELECTING_SUBJECT -> handleSelectingSubjectState(session, rawText);
        }
    }

    // =========================================================================
    // Resolução de sessão
    // =========================================================================

    /**
     * Recupera a sessão do Redis. Quando a sessão expirou ou é o primeiro contato,
     * reconstrói o estado a partir do cadastro no Postgres: o telefone é a chave única
     * do estudante, então um número já cadastrado nunca refaz o formulário.
     */
    private ChatSessionState resolveSession(String phoneNumber) {
        return chatSessionStore.find(phoneNumber)
                .orElseGet(() -> rebuildFromDatabase(phoneNumber));
    }

    private ChatSessionState rebuildFromDatabase(String phoneNumber) {
        ChatSessionState state = ChatSessionState.builder().phoneNumber(phoneNumber).build();

        studentRepository.findByPhoneNumber(phoneNumber).ifPresent(student -> {
            state.setStudentId(student.getId());
            state.setCurrentState(ChatState.MAIN_MENU);
            studentCourseRepository.findByStudentIdAndActiveTrue(student.getId()).stream()
                    .findFirst()
                    .map(StudentCourse::getCourse)
                    .ifPresent(course -> state.setSelectedCourseId(course.getId()));
            log.info("[Orchestrator] Sessão reconstruída para estudante já cadastrado [{}]", phoneNumber);
        });

        return state;
    }

    private Student loadStudent(ChatSessionState session) {
        return studentRepository.findById(session.getStudentId())
                .orElseThrow(() -> new IllegalStateException(
                        "Sessão aponta para estudante inexistente: " + session.getStudentId()));
    }

    // =========================================================================
    // Onboarding (formulário de primeiro contato)
    // =========================================================================

    private void startOnboarding(ChatSessionState session) {
        log.info("[Orchestrator] Primeiro contato detectado: [{}]. Iniciando formulário de cadastro.",
                session.getPhoneNumber());

        transitionTo(session, ChatState.AWAITING_FULL_NAME);

        send(session, """
                👋 *Bem-vindo ao Chatbot MMEEBB UNIPAM!*
                Seu assistente de repetição espaçada para os estudos.

                Como este é seu primeiro acesso, preciso de alguns dados rápidos para montar seu plano de revisões.

                📝 *1 de 3* — Qual é o seu *nome completo*?""");
    }

    private void handleFullNameInput(ChatSessionState session, String rawText) {
        String name = rawText.replaceAll("\\s+", " ").trim();

        if (name.length() < 3 || !name.contains(" ")) {
            send(session, """
                    ⚠️ Preciso do seu *nome completo* (nome e sobrenome).

                    _Exemplo: Maria Silva Andrade_""");
            return;
        }

        session.setDraftFullName(name);
        transitionTo(session, ChatState.AWAITING_RA);

        send(session, String.format("""
                Prazer, *%s*! 🙌

                📝 *2 de 3* — Qual é o seu *RA (registro acadêmico)*?

                _Se não souber agora, envie *pular*._""", firstName(name)));
    }

    private void handleRaInput(ChatSessionState session, String rawText, String lowerText) {
        if (!SKIP_COMMANDS.contains(lowerText)) {
            String ra = rawText.replaceAll("[^A-Za-z0-9]", "").trim();

            if (ra.isEmpty()) {
                send(session, "⚠️ RA inválido. Envie apenas números e letras, ou *pular* para informar depois.");
                return;
            }

            Optional<Student> owner = studentRepository.findByRa(ra);
            if (owner.isPresent() && !owner.get().getPhoneNumber().equals(session.getPhoneNumber())) {
                send(session, """
                        ⚠️ Este RA já está vinculado a outro número de WhatsApp.

                        _Confira o número digitado ou envie *pular* para seguir sem o RA._""");
                return;
            }

            session.setDraftRa(ra);
        }

        List<Course> courses = courseRepository.findByActiveTrue();
        if (courses.isEmpty()) {
            log.error("[Orchestrator] Cadastro interrompido: não há cursos ativos no banco.");
            send(session, "⚠️ Ainda não há cursos cadastrados no sistema. Procure a coordenação e tente novamente mais tarde.");
            return;
        }

        transitionTo(session, ChatState.AWAITING_COURSE);
        send(session, "📝 *3 de 3* — Selecione o seu *curso*:\n\n" + numberedList(courses.stream().map(Course::getName).toList()));
    }

    private void handleCourseInput(ChatSessionState session, String rawText) {
        List<Course> courses = courseRepository.findByActiveTrue();

        Optional<Course> chosen = parseSelection(rawText, courses);
        if (chosen.isEmpty()) {
            send(session, "⚠️ Opção inválida. Envie o *número* do seu curso:\n\n"
                    + numberedList(courses.stream().map(Course::getName).toList()));
            return;
        }

        session.setDraftCourseId(chosen.get().getId());
        transitionTo(session, ChatState.AWAITING_ACADEMIC_PERIOD);

        send(session, String.format("""
                Curso *%s* selecionado! ✅

                Por último: em qual *período* você está? _(envie apenas o número, ex.: 8)_""",
                chosen.get().getName()));
    }

    private void handleAcademicPeriodInput(ChatSessionState session, String rawText) {
        Integer period = parsePositiveInt(rawText);
        if (period == null || period > MAX_ACADEMIC_PERIOD) {
            send(session, "⚠️ Período inválido. Envie apenas o número do período, entre *1* e *"
                    + MAX_ACADEMIC_PERIOD + "*.");
            return;
        }

        Student student = studentOnboardingService.register(
                session.getPhoneNumber(),
                session.getDraftFullName(),
                session.getDraftRa(),
                session.getDraftCourseId(),
                period
        );

        session.setStudentId(student.getId());
        session.setSelectedCourseId(session.getDraftCourseId());
        session.clearOnboardingDraft();
        transitionTo(session, ChatState.MAIN_MENU);

        long pending = countPendingReviews(student.getId());

        send(session, String.format("""
                🎉 *Cadastro concluído, %s!*

                Preparei *%d* questão(ões) para sua primeira rodada de revisões.

                %s

                💬 _Você também pode escrever livremente: pergunte qualquer dúvida de conteúdo que eu consulto o acervo da sua disciplina._""",
                firstName(student.getFullName()), pending, menuBody()));
    }

    // =========================================================================
    // Menu principal e roteamento por intenção
    // =========================================================================

    private void handleMainMenuState(ChatSessionState session, String rawText) {
        switch (rawText) {
            case "1" -> {
                startReviewMode(session);
                return;
            }
            case "2" -> {
                enterDoubtMode(session);
                return;
            }
            case "3" -> {
                startCourseSelection(session);
                return;
            }
            default -> { /* texto livre: segue para a classificação de intenção */ }
        }

        IntentResultDto intent = intentRouterService.classify(rawText);
        applySubjectHint(session, intent.subjectHint());

        switch (intent.intent()) {
            case START_REVIEW -> startReviewMode(session);
            case CHANGE_SUBJECT -> startCourseSelection(session);
            case SHOW_MENU -> sendMainMenu(session);
            case EXIT -> handleExitCommand(session);
            case ASK_DOUBT -> {
                // Responde a dúvida imediatamente e mantém o aluno no modo de perguntas.
                transitionTo(session, ChatState.RAG_DOUBT_MODE);
                answerDoubt(session, rawText);
            }
        }
    }

    /**
     * Associa a disciplina citada em texto livre ("dúvida em cardiologia") ao escopo da sessão,
     * restringindo a busca vetorial a essa disciplina.
     */
    private void applySubjectHint(ChatSessionState session, String subjectHint) {
        if (subjectHint == null || subjectHint.isBlank() || session.getSelectedCourseId() == null) {
            return;
        }

        subjectRepository
                .findByCourseIdAndActiveTrueAndNameContainingIgnoreCase(session.getSelectedCourseId(), subjectHint.trim())
                .stream()
                .findFirst()
                .ifPresent(subject -> {
                    log.info("[Orchestrator] Disciplina \"{}\" inferida da mensagem e aplicada à sessão de [{}]",
                            subject.getName(), session.getPhoneNumber());
                    session.setSelectedSubjectId(subject.getId());
                });
    }

    private void enterDoubtMode(ChatSessionState session) {
        transitionTo(session, ChatState.RAG_DOUBT_MODE);
        send(session, """
                💡 *Modo Dúvidas ativado*

                Envie sua pergunta sobre qualquer conteúdo do acervo que eu consulto o material da sua disciplina.

                _Digite *menu* a qualquer momento para voltar._""");
    }

    // =========================================================================
    // Modo revisão (motor MMEEBB)
    // =========================================================================

    private void startReviewMode(ChatSessionState session) {
        Student student = loadStudent(session);
        List<RepetitionSchedule> pending = findPendingReviews(student.getId());

        if (pending.isEmpty()) {
            transitionTo(session, ChatState.MAIN_MENU);
            send(session, """
                    🎉 *Nenhuma revisão pendente por hoje!*

                    Seus próximos reforços já estão agendados pelo método MMEEBB. Digite *menu* para ver outras opções.""");
            return;
        }

        Flashcard first = pending.get(0).getFlashcard();
        session.setCurrentFlashcardId(first.getId());
        transitionTo(session, ChatState.REVIEW_MODE);

        send(session, formatFlashcard(first));
    }

    private void handleReviewModeState(ChatSessionState session, String studentAnswer) {
        Student student = loadStudent(session);

        Optional<Flashcard> current = session.getCurrentFlashcardId() != null
                ? flashcardRepository.findById(session.getCurrentFlashcardId())
                : Optional.empty();

        if (current.isEmpty()) {
            log.warn("[Orchestrator] Modo de revisão sem flashcard ativo para [{}]. Voltando ao menu.",
                    session.getPhoneNumber());
            session.clearReviewContext();
            transitionTo(session, ChatState.MAIN_MENU);
            sendMainMenu(session);
            return;
        }

        Flashcard card = current.get();
        AnswerEvaluationDto evaluation = answerEvaluationService.evaluate(studentAnswer, card);

        RepetitionSchedule schedule = repetitionScheduleRepository
                .findByStudentIdAndFlashcardId(student.getId(), card.getId())
                .orElseGet(() -> mmeebbService.initializeSchedule(student, card, LocalDate.now()));

        RepetitionSchedule updated = mmeebbService.processAnswer(schedule, evaluation.correct(), LocalDateTime.now());
        repetitionScheduleRepository.save(updated);

        StringBuilder feedback = new StringBuilder();
        if (evaluation.correct()) {
            feedback.append("✅ *Resposta correta!*\n")
                    .append("Próximo reforço em *").append(updated.getIntervalDays())
                    .append(" dia(s)* — IRA 2^").append(updated.getNIndex()).append(".\n\n");
        } else {
            feedback.append("❌ *Resposta incorreta.*\n")
                    .append("Gabarito: *").append(card.getAnswer()).append("*\n")
                    .append("Intervalo reiniciado para *1 dia* (N=0) para consolidação.\n\n");
        }

        if (evaluation.feedback() != null && !evaluation.feedback().isBlank()) {
            feedback.append("🧠 *Correção:* ").append(evaluation.feedback()).append("\n\n");
        }

        if (card.getExplanation() != null && !card.getExplanation().isBlank()) {
            feedback.append("💡 *Explicação:* ").append(card.getExplanation()).append("\n\n");
        }

        List<RepetitionSchedule> remaining = findPendingReviews(student.getId()).stream()
                .filter(s -> !s.getFlashcard().getId().equals(card.getId()))
                .toList();

        if (remaining.isEmpty()) {
            session.clearReviewContext();
            transitionTo(session, ChatState.MAIN_MENU);
            feedback.append("🎉 *Todas as revisões de hoje foram concluídas!*\n\nDigite *menu* para voltar.");
        } else {
            Flashcard next = remaining.get(0).getFlashcard();
            session.setCurrentFlashcardId(next.getId());
            chatSessionStore.save(session);
            feedback.append("------------------------------------\n\n").append(formatFlashcard(next));
        }

        send(session, feedback.toString());
    }

    private List<RepetitionSchedule> findPendingReviews(java.util.UUID studentId) {
        return repetitionScheduleRepository.findPendingReviewsByStudent(
                studentId, LocalDate.now(), ScheduleStatus.PENDING);
    }

    private long countPendingReviews(java.util.UUID studentId) {
        return repetitionScheduleRepository
                .countByStudentIdAndNextReviewDateLessThanEqualAndIsActiveTrue(studentId, LocalDate.now());
    }

    // =========================================================================
    // Modo dúvidas (RAG)
    // =========================================================================

    private void handleRagDoubtModeState(ChatSessionState session, String questionText) {
        answerDoubt(session, questionText);
    }

    private void answerDoubt(ChatSessionState session, String questionText) {
        log.info("[Orchestrator] Dúvida RAG de [{}] (disciplina {}): \"{}\"",
                session.getPhoneNumber(), session.getSelectedSubjectId(), questionText);

        String answer = subjectRagService.answerDoubt(questionText, session.getSelectedSubjectId());

        String header = Optional.ofNullable(session.getSelectedSubjectId())
                .flatMap(subjectRepository::findById)
                .map(subject -> "📖 *" + subject.getName() + "*")
                .orElse("🌐 *Acervo geral*");

        send(session, String.format("""
                🤖 *Tutor Virtual UNIPAM* (%s)

                %s

                ------------------------------------
                _Envie outra dúvida ou digite *menu* para voltar._""", header, answer));
    }

    // =========================================================================
    // Troca de curso / disciplina
    // =========================================================================

    private void startCourseSelection(ChatSessionState session) {
        List<Course> courses = courseRepository.findByActiveTrue();
        if (courses.isEmpty()) {
            send(session, "⚠️ Não há cursos cadastrados no momento.\n\nDigite *menu* para voltar.");
            return;
        }

        transitionTo(session, ChatState.SELECTING_COURSE);
        send(session, "🎓 *Selecione o curso:*\n\n"
                + numberedList(courses.stream().map(Course::getName).toList())
                + "\n_Ou digite *menu* para cancelar._");
    }

    private void handleSelectingCourseState(ChatSessionState session, String rawText) {
        List<Course> courses = courseRepository.findByActiveTrue();

        Optional<Course> chosen = parseSelection(rawText, courses);
        if (chosen.isEmpty()) {
            send(session, "⚠️ Número de curso inválido. Escolha um da lista ou digite *menu* para voltar.");
            return;
        }

        session.setSelectedCourseId(chosen.get().getId());
        session.setSelectedSubjectId(null);

        List<Subject> subjects = subjectRepository.findByCourseIdAndActiveTrue(chosen.get().getId());
        if (subjects.isEmpty()) {
            transitionTo(session, ChatState.MAIN_MENU);
            send(session, "✅ Curso *" + chosen.get().getName()
                    + "* selecionado.\n_(Nenhuma disciplina vinculada encontrada.)_\n\n" + menuBody());
            return;
        }

        transitionTo(session, ChatState.SELECTING_SUBJECT);
        send(session, "📖 *Selecione a disciplina de " + chosen.get().getName() + ":*\n\n"
                + numberedList(subjects.stream().map(Subject::getName).toList())
                + "\n_Ou digite *menu* para cancelar._");
    }

    private void handleSelectingSubjectState(ChatSessionState session, String rawText) {
        if (session.getSelectedCourseId() == null) {
            transitionTo(session, ChatState.MAIN_MENU);
            sendMainMenu(session);
            return;
        }

        List<Subject> subjects = subjectRepository.findByCourseIdAndActiveTrue(session.getSelectedCourseId());

        Optional<Subject> chosen = parseSelection(rawText, subjects);
        if (chosen.isEmpty()) {
            send(session, "⚠️ Número de disciplina inválido. Escolha um da lista ou digite *menu* para voltar.");
            return;
        }

        session.setSelectedSubjectId(chosen.get().getId());
        transitionTo(session, ChatState.MAIN_MENU);

        send(session, "✅ Disciplina *" + chosen.get().getName() + "* selecionada!\n\n" + menuBody());
    }

    // =========================================================================
    // Comandos globais
    // =========================================================================

    private void handleExitCommand(ChatSessionState session) {
        log.info("[Orchestrator] Comando de saída recebido de [{}]", session.getPhoneNumber());
        session.clearReviewContext();
        transitionTo(session, ChatState.MAIN_MENU);

        send(session, """
                👋 *Até logo!* Sua sessão de estudos foi finalizada.

                Quando quiser voltar a revisar ou tirar uma dúvida, é só mandar uma mensagem. 🚀📚""");
    }

    private void handleGlobalReset(ChatSessionState session) {
        log.info("[Orchestrator] Reset global recebido de [{}]", session.getPhoneNumber());
        session.clearReviewContext();
        transitionTo(session, ChatState.MAIN_MENU);
        sendMainMenu(session);
    }

    // =========================================================================
    // Auxiliares
    // =========================================================================

    private void transitionTo(ChatSessionState session, ChatState newState) {
        session.setCurrentState(newState);
        chatSessionStore.save(session);
    }

    private void send(ChatSessionState session, String message) {
        uazapiClientService.sendTextMessage(session.getPhoneNumber(), message);
    }

    private void sendMainMenu(ChatSessionState session) {
        send(session, menuBody());
    }

    private String menuBody() {
        return """
                📋 *Menu Principal — Chatbot MMEEBB*

                *1* - 📚 Revisar (método MMEEBB)
                *2* - 💡 Tirar uma dúvida
                *3* - 🔄 Trocar curso/disciplina

                _Digite o número, escreva o que precisa ou envie *sair* para encerrar._""";
    }

    private String formatFlashcard(Flashcard card) {
        StringBuilder sb = new StringBuilder();
        sb.append("📚 *Revisão MMEEBB*\n");
        sb.append("🏷️ *Tópico:* ").append(card.getTopic()).append("\n\n");
        sb.append("❓ *Pergunta:* ").append(card.getQuestion()).append("\n\n");

        if (card.getOptionsJson() != null && !card.getOptionsJson().isBlank()) {
            sb.append("Opções:\n").append(card.getOptionsJson()).append("\n\n");
        }

        sb.append("_Envie sua resposta ou digite *menu* para pausar._");
        return sb.toString();
    }

    private String numberedList(List<String> labels) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < labels.size(); i++) {
            sb.append("*").append(i + 1).append("* - ").append(labels.get(i)).append("\n");
        }
        return sb.toString();
    }

    private <T> Optional<T> parseSelection(String input, List<T> options) {
        Integer index = parsePositiveInt(input);
        if (index == null || index > options.size()) {
            return Optional.empty();
        }
        return Optional.of(options.get(index - 1));
    }

    private Integer parsePositiveInt(String input) {
        if (input == null) {
            return null;
        }
        try {
            int value = Integer.parseInt(input.trim());
            return value >= 1 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String firstName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "Estudante";
        }
        return fullName.trim().split("\\s+")[0];
    }
}
