package br.edu.unipam.tcc.service.impl;

import br.edu.unipam.tcc.dto.UazapiWebhookDto;
import br.edu.unipam.tcc.entity.*;
import br.edu.unipam.tcc.entity.enums.ChatState;
import br.edu.unipam.tcc.entity.enums.QuestionType;
import br.edu.unipam.tcc.entity.enums.ScheduleStatus;
import br.edu.unipam.tcc.repository.*;
import br.edu.unipam.tcc.service.ChatFlowOrchestrator;
import br.edu.unipam.tcc.service.MmeebbService;
import br.edu.unipam.tcc.service.SubjectRagService;
import br.edu.unipam.tcc.service.UazapiClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Implementação do orquestrador de fluxo conversacional do Chatbot MMEEBB.
 * Gerencia a Máquina de Estados Finita (FSM), sessões de usuários, comandos globais de reset,
 * revisões de flashcards via motor MMEEBB, consultas RAG multidisciplinares e mensageria WhatsApp.
 */
@Slf4j
@Service
public class ChatFlowOrchestratorImpl implements ChatFlowOrchestrator {

    private static final Set<String> RESET_COMMANDS = Set.of(
            "menu", "sair", "inicio", "início", "começo", "comeco", "reset", "reiniciar",
            "/menu", "/sair", "/inicio", "/start", "/reset"
    );

    private static final Set<String> GREETING_OR_HELP_COMMANDS = Set.of(
            "ola", "olá", "oi", "oii", "oiii", "bom dia", "boa tarde", "boa noite",
            "eai", "eae", "fala", "salve", "alo", "alô", "hello", "hi", "hey",
            "ajuda", "help", "socorro", "como funciona", "opcoes", "opções", "comandos", "info",
            "/help", "/ajuda"
    );

    private final ChatSessionRepository chatSessionRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final SubjectRepository subjectRepository;
    private final FlashcardRepository flashcardRepository;
    private final RepetitionScheduleRepository repetitionScheduleRepository;
    private final MmeebbService mmeebbService;
    private final UazapiClientService uazapiClientService;
    private final SubjectRagService subjectRagService;

    public ChatFlowOrchestratorImpl(
            ChatSessionRepository chatSessionRepository,
            StudentRepository studentRepository,
            CourseRepository courseRepository,
            SubjectRepository subjectRepository,
            FlashcardRepository flashcardRepository,
            RepetitionScheduleRepository repetitionScheduleRepository,
            MmeebbService mmeebbService,
            UazapiClientService uazapiClientService,
            SubjectRagService subjectRagService
    ) {
        this.chatSessionRepository = chatSessionRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.subjectRepository = subjectRepository;
        this.flashcardRepository = flashcardRepository;
        this.repetitionScheduleRepository = repetitionScheduleRepository;
        this.mmeebbService = mmeebbService;
        this.uazapiClientService = uazapiClientService;
        this.subjectRagService = subjectRagService;
    }

    @Override
    @Transactional
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

        // 1. Resolução de Sessão e Estudante
        ChatSession session = resolveSession(phoneNumber);
        session.setLastInteractionAt(LocalDateTime.now());

        // 2. Interceptação de Comandos Globais de Reset
        if (isGlobalResetCommand(lowerText)) {
            handleGlobalReset(session, phoneNumber);
            return;
        }

        // 3. Execução da FSM por Estado
        switch (session.getCurrentState()) {
            case NEW -> handleNewState(session, phoneNumber);
            case MAIN_MENU -> handleMainMenuState(session, phoneNumber, rawText, lowerText);
            case REVIEW_MODE -> handleReviewModeState(session, phoneNumber, rawText);
            case RAG_DOUBT_MODE -> handleRagDoubtModeState(session, phoneNumber, rawText);
            case SELECTING_COURSE -> handleSelectingCourseState(session, phoneNumber, rawText);
            case SELECTING_SUBJECT -> handleSelectingSubjectState(session, phoneNumber, rawText);
        }
    }

    private ChatSession resolveSession(String phoneNumber) {
        return chatSessionRepository.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> {
                    Student student = studentRepository.findByPhoneNumber(phoneNumber)
                            .orElseGet(() -> studentRepository.save(
                                    Student.builder()
                                            .phoneNumber(phoneNumber)
                                            .fullName("Estudante")
                                            .active(true)
                                            .build()
                            ));

                    ChatSession newSession = ChatSession.builder()
                            .phoneNumber(phoneNumber)
                            .student(student)
                            .currentState(ChatState.NEW)
                            .lastInteractionAt(LocalDateTime.now())
                            .build();

                    return chatSessionRepository.save(newSession);
                });
    }

    private boolean isGlobalResetCommand(String text) {
        if (text == null || text.isBlank()) return false;
        return RESET_COMMANDS.contains(text.toLowerCase().trim());
    }

    private boolean isGreetingOrHelpCommand(String text) {
        if (text == null || text.isBlank()) return false;
        String clean = text.toLowerCase().trim();
        if (GREETING_OR_HELP_COMMANDS.contains(clean)) {
            return true;
        }
        return clean.startsWith("olá") || clean.startsWith("ola") || clean.startsWith("oi ")
                || clean.startsWith("bom dia") || clean.startsWith("boa tarde") || clean.startsWith("boa noite")
                || clean.startsWith("ajuda") || clean.startsWith("help");
    }

    private void handleGlobalReset(ChatSession session, String phoneNumber) {
        log.info("[Orchestrator] Comando global de reset recebido de [{}]", phoneNumber);
        session.setCurrentState(ChatState.MAIN_MENU);
        session.setCurrentFlashcard(null);
        chatSessionRepository.save(session);
        sendMainMenuMessage(phoneNumber);
    }

    private void handleNewState(ChatSession session, String phoneNumber) {
        log.info("[Orchestrator] Novo contato detectado: [{}]", phoneNumber);
        session.setCurrentState(ChatState.MAIN_MENU);
        chatSessionRepository.save(session);

        String welcomeMsg = """
                👋 *Olá! Bem-vindo ao Chatbot MMEEBB UNIPAM!*
                Seu assistente inteligente de repetição espaçada e estudos médicos.

                📋 *Menu Principal*
                *1* - 📚 Modo Revisão MMEEBB
                *2* - 💡 Modo Dúvidas (RAG)
                *3* - 🔄 Trocar Disciplina/Curso

                _Digite o número da opção desejada para começar._""";

        uazapiClientService.sendTextMessage(phoneNumber, welcomeMsg);
    }

    private void handleMainMenuState(ChatSession session, String phoneNumber, String rawText, String lowerText) {
        if (isGreetingOrHelpCommand(lowerText)) {
            sendMainMenuMessage(phoneNumber);
        } else if ("1".equals(rawText) || lowerText.contains("revis")) {
            startReviewMode(session, phoneNumber);
        } else if ("2".equals(rawText) || lowerText.contains("duvid") || lowerText.contains("rag")) {
            session.setCurrentState(ChatState.RAG_DOUBT_MODE);
            chatSessionRepository.save(session);

            String ragMsg = """
                    💡 *Modo Dúvidas e RAG Ativado*
                    
                    Envie sua pergunta ou dúvida clínica/acadêmica.
                    Nosso tutor inteligente consultará a base de conhecimento para te auxiliar!
                    
                    _A qualquer momento, digite *menu* para voltar ao menu principal._""";
            uazapiClientService.sendTextMessage(phoneNumber, ragMsg);
        } else if ("3".equals(rawText) || lowerText.contains("trocar") || lowerText.contains("curso")) {
            startCourseSelection(session, phoneNumber);
        } else {
            String invalidMsg = """
                    ⚠️ *Opção não reconhecida.*
                    
                    Por favor, escolha uma das opções válidas:
                    *1* - 📚 Modo Revisão MMEEBB
                    *2* - 💡 Modo Dúvidas (RAG)
                    *3* - 🔄 Trocar Disciplina/Curso
                    
                    _Ou digite *menu* para reiniciar o fluxo._""";
            uazapiClientService.sendTextMessage(phoneNumber, invalidMsg);
        }
    }

    private void startReviewMode(ChatSession session, String phoneNumber) {
        Student student = session.getStudent();
        List<RepetitionSchedule> pendingList = repetitionScheduleRepository.findPendingReviewsByStudent(
                student.getId(),
                LocalDate.now(),
                ScheduleStatus.PENDING
        );

        if (pendingList.isEmpty()) {
            String emptyMsg = """
                    🎉 *Parabéns!* Você não possui flashcards pendentes para revisão no momento.
                    
                    Digite *menu* para ver outras opções de estudo.""";
            uazapiClientService.sendTextMessage(phoneNumber, emptyMsg);
            return;
        }

        RepetitionSchedule firstSchedule = pendingList.get(0);
        Flashcard firstCard = firstSchedule.getFlashcard();

        session.setCurrentState(ChatState.REVIEW_MODE);
        session.setCurrentFlashcard(firstCard);
        chatSessionRepository.save(session);

        sendFlashcardQuestion(phoneNumber, firstCard);
    }

    private void handleReviewModeState(ChatSession session, String phoneNumber, String studentAnswer) {
        Flashcard currentCard = session.getCurrentFlashcard();
        Student student = session.getStudent();

        if (currentCard == null) {
            log.warn("[Orchestrator] Modo de revisão sem flashcard ativo para [{}]. Resetando ao menu.", phoneNumber);
            session.setCurrentState(ChatState.MAIN_MENU);
            chatSessionRepository.save(session);
            sendMainMenuMessage(phoneNumber);
            return;
        }

        // 1. Avalia a resposta do aluno
        boolean isCorrect = evaluateAnswer(studentAnswer, currentCard);

        RepetitionSchedule schedule = repetitionScheduleRepository
                .findByStudentIdAndFlashcardId(student.getId(), currentCard.getId())
                .orElseGet(() -> mmeebbService.initializeSchedule(student, currentCard, LocalDate.now()));

        RepetitionSchedule updatedSchedule = mmeebbService.processAnswer(schedule, isCorrect, LocalDateTime.now());
        repetitionScheduleRepository.save(updatedSchedule);

        // 2. Constrói feedback do MMEEBB
        StringBuilder feedback = new StringBuilder();
        if (isCorrect) {
            feedback.append("✅ *Resposta Correta!*\n")
                    .append("Intervalo aumentado para *").append(updatedSchedule.getIntervalDays())
                    .append(" dias* (N=").append(updatedSchedule.getNIndex()).append(").\n\n");
        } else {
            feedback.append("❌ *Resposta Incorreta!*\n")
                    .append("Resposta correta: *").append(currentCard.getAnswer()).append("*\n")
                    .append("Intervalo reiniciado para *1 dia* (N=0) para consolidação.\n\n");
        }

        if (currentCard.getExplanation() != null && !currentCard.getExplanation().isBlank()) {
            feedback.append("💡 *Explicação:* ").append(currentCard.getExplanation()).append("\n\n");
        }

        // 3. Busca próximo flashcard pendente para hoje
        List<RepetitionSchedule> remainingList = repetitionScheduleRepository.findPendingReviewsByStudent(
                student.getId(),
                LocalDate.now(),
                ScheduleStatus.PENDING
        );

        // Filtra o card recém-processado que agora tem data futura
        List<RepetitionSchedule> nextCandidates = remainingList.stream()
                .filter(s -> !s.getFlashcard().getId().equals(currentCard.getId())
                        && !s.getNextReviewDate().isAfter(LocalDate.now()))
                .toList();

        if (!nextCandidates.isEmpty()) {
            Flashcard nextCard = nextCandidates.get(0).getFlashcard();
            session.setCurrentFlashcard(nextCard);
            chatSessionRepository.save(session);

            feedback.append("------------------------------------\n\n");
            feedback.append(formatFlashcardText(nextCard));
            uazapiClientService.sendTextMessage(phoneNumber, feedback.toString());
        } else {
            session.setCurrentFlashcard(null);
            session.setCurrentState(ChatState.MAIN_MENU);
            chatSessionRepository.save(session);

            feedback.append("🎉 *Parabéns! Todas as revisões de hoje foram concluídas!*\n\n")
                    .append("Digite *menu* para retornar ao menu principal.");
            uazapiClientService.sendTextMessage(phoneNumber, feedback.toString());
        }
    }

    private boolean evaluateAnswer(String studentAnswer, Flashcard card) {
        if (studentAnswer == null || studentAnswer.isBlank()) {
            return false;
        }

        String cleanedStudent = studentAnswer.trim().replaceAll("\\s+", " ");
        String cleanedExpected = card.getAnswer().trim().replaceAll("\\s+", " ");

        // Comparação direta sem case-sensitivity
        if (cleanedStudent.equalsIgnoreCase(cleanedExpected)) {
            return true;
        }

        // Comparação de letra em questões de múltipla escolha (ex: "A", "B", "Opção A")
        if (card.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
            String firstLetterStudent = cleanedStudent.replaceAll("(?i)^(letra|opcao|opção)\\s*", "");
            return firstLetterStudent.equalsIgnoreCase(cleanedExpected);
        }

        return false;
    }

    private void handleRagDoubtModeState(ChatSession session, String phoneNumber, String questionText) {
        log.info("[Orchestrator] Dúvida RAG recebida de [{}]: \"{}\"", phoneNumber, questionText);

        Subject currentSubject = session.getSelectedSubject();
        if (currentSubject == null) {
            log.info("[Orchestrator] Aluno [{}] tentou consultar RAG sem disciplina ativa selecionada.", phoneNumber);
            String noSubjectMsg = """
                    ⚠️ *Você ainda não selecionou uma disciplina ativa.*

                    Para que o tutor inteligente possa consultar a base de conhecimento correta:
                    1. Digite *menu* para voltar ao Menu Principal
                    2. Escolha a opção *3* (*Trocar Disciplina/Curso*)
                    3. Retorne ao *Modo Dúvidas (RAG)*.

                    _Digite *menu* para continuar._""";

            uazapiClientService.sendTextMessage(phoneNumber, noSubjectMsg);
            return;
        }

        String ragAnswer = subjectRagService.answerDoubt(questionText, currentSubject.getId());

        String formattedMessage = String.format("""
                🤖 *Tutor Virtual UNIPAM* (📖 *%s*)

                %s

                ------------------------------------
                _Envie outra dúvida sobre a disciplina ou digite *menu* para voltar ao menu principal._""",
                currentSubject.getName(), ragAnswer);

        uazapiClientService.sendTextMessage(phoneNumber, formattedMessage);
    }

    private void startCourseSelection(ChatSession session, String phoneNumber) {
        List<Course> activeCourses = courseRepository.findByActiveTrue();
        if (activeCourses.isEmpty()) {
            String emptyCoursesMsg = "⚠️ Não há cursos cadastrados no momento.\n\nDigite *menu* para voltar.";
            uazapiClientService.sendTextMessage(phoneNumber, emptyCoursesMsg);
            return;
        }

        session.setCurrentState(ChatState.SELECTING_COURSE);
        chatSessionRepository.save(session);

        StringBuilder sb = new StringBuilder("🎓 *Selecione seu Curso:*\n\n");
        for (int i = 0; i < activeCourses.size(); i++) {
            sb.append("*").append(i + 1).append("* - ").append(activeCourses.get(i).getName()).append("\n");
        }
        sb.append("\n_Digite o número da opção desejada ou *menu* para cancelar._");

        uazapiClientService.sendTextMessage(phoneNumber, sb.toString());
    }

    private void handleSelectingCourseState(ChatSession session, String phoneNumber, String input) {
        List<Course> activeCourses = courseRepository.findByActiveTrue();
        try {
            int selectedIndex = Integer.parseInt(input.trim()) - 1;
            if (selectedIndex >= 0 && selectedIndex < activeCourses.size()) {
                Course chosenCourse = activeCourses.get(selectedIndex);
                session.setSelectedCourse(chosenCourse);

                List<Subject> activeSubjects = subjectRepository.findByCourseIdAndActiveTrue(chosenCourse.getId());
                if (activeSubjects.isEmpty()) {
                    session.setCurrentState(ChatState.MAIN_MENU);
                    chatSessionRepository.save(session);
                    uazapiClientService.sendTextMessage(phoneNumber, "✅ Curso *" + chosenCourse.getName() + "* selecionado!\n(Nenhuma disciplina vinculada encontrada).\n\nRetornando ao Menu Principal...");
                    sendMainMenuMessage(phoneNumber);
                    return;
                }

                session.setCurrentState(ChatState.SELECTING_SUBJECT);
                chatSessionRepository.save(session);

                StringBuilder sb = new StringBuilder("📖 *Selecione a Disciplina do Curso " + chosenCourse.getName() + ":*\n\n");
                for (int i = 0; i < activeSubjects.size(); i++) {
                    sb.append("*").append(i + 1).append("* - ").append(activeSubjects.get(i).getName()).append("\n");
                }
                sb.append("\n_Digite o número da disciplina desejada ou *menu* para cancelar._");
                uazapiClientService.sendTextMessage(phoneNumber, sb.toString());
                return;
            }
        } catch (NumberFormatException ignored) {
            // Entrada não numérica
        }

        uazapiClientService.sendTextMessage(phoneNumber, "⚠️ Número de curso inválido. Digite um número da lista ou envie *menu* para voltar.");
    }

    private void handleSelectingSubjectState(ChatSession session, String phoneNumber, String input) {
        Course currentCourse = session.getSelectedCourse();
        if (currentCourse == null) {
            session.setCurrentState(ChatState.MAIN_MENU);
            chatSessionRepository.save(session);
            sendMainMenuMessage(phoneNumber);
            return;
        }

        List<Subject> activeSubjects = subjectRepository.findByCourseIdAndActiveTrue(currentCourse.getId());
        try {
            int selectedIndex = Integer.parseInt(input.trim()) - 1;
            if (selectedIndex >= 0 && selectedIndex < activeSubjects.size()) {
                Subject chosenSubject = activeSubjects.get(selectedIndex);
                session.setSelectedSubject(chosenSubject);
                session.setCurrentState(ChatState.MAIN_MENU);
                chatSessionRepository.save(session);

                String successMsg = "✅ Disciplina *" + chosenSubject.getName() + "* selecionada com sucesso!\n\n" +
                        "📋 *Menu Principal - Chatbot MMEEBB*\n\n" +
                        "Escolha uma das opções abaixo:\n" +
                        "*1* - 📚 Modo Revisão MMEEBB\n" +
                        "*2* - 💡 Modo Dúvidas (RAG)\n" +
                        "*3* - 🔄 Trocar Disciplina/Curso\n\n" +
                        "_Digite o número da opção desejada ou *sair* para reiniciar._";
                uazapiClientService.sendTextMessage(phoneNumber, successMsg);
                return;
            }
        } catch (NumberFormatException ignored) {
            // Entrada não numérica
        }

        uazapiClientService.sendTextMessage(phoneNumber, "⚠️ Número de disciplina inválido. Digite um número da lista ou envie *menu* para voltar.");
    }

    private void sendFlashcardQuestion(String phoneNumber, Flashcard card) {
        String message = formatFlashcardText(card);
        uazapiClientService.sendTextMessage(phoneNumber, message);
    }

    private String formatFlashcardText(Flashcard card) {
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

    private void sendMainMenuMessage(String phoneNumber) {
        String menuMsg = """
                📋 *Menu Principal - Chatbot MMEEBB*
                
                Escolha uma das opções abaixo:
                *1* - 📚 Modo Revisão MMEEBB
                *2* - 💡 Modo Dúvidas (RAG)
                *3* - 🔄 Trocar Disciplina/Curso
                
                _Digite o número da opção desejada ou *sair* para reiniciar._""";
        uazapiClientService.sendTextMessage(phoneNumber, menuMsg);
    }
}
