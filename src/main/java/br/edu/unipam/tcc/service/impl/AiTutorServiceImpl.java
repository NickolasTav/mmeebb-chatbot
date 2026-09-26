package br.edu.unipam.tcc.service.impl;

import br.edu.unipam.tcc.dto.TutorContextDto;
import br.edu.unipam.tcc.observability.MmeebbMetrics;
import br.edu.unipam.tcc.service.AiTutorService;
import br.edu.unipam.tcc.session.ConversationHistoryStore;
import br.edu.unipam.tcc.session.ConversationTurn;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Responde às dúvidas do estudante direto pelo Gemini, sem base de conhecimento: o que orienta a
 * resposta é o perfil acadêmico do aluno, a questão em revisão e o histórico curto da conversa
 * (spec-09). A segurança conceitual, que antes vinha da ancoragem no acervo, agora vem das
 * diretrizes do prompt.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiTutorServiceImpl implements AiTutorService {

    private static final String UNAVAILABLE_MESSAGE = """
            ⚠️ *Desculpe, tive uma instabilidade momentânea para responder.*

            _Envie sua dúvida novamente em instantes ou digite *menu* para voltar ao menu principal._""";

    private static final String SYSTEM_PROMPT = """
            Você é o Tutor Acadêmico do Chatbot MMEEBB do UNIPAM, conversando com um estudante pelo WhatsApp.
            Sua função é esclarecer dúvidas de conteúdo com rigor conceitual, clareza pedagógica e objetividade.

            DIRETRIZES OBRIGATÓRIAS:
            1. Responda com base no conhecimento acadêmico consolidado, no nível de graduação do curso do estudante.
            2. Se não tiver segurança sobre um ponto, diga isso claramente. Nunca invente referências, estudos, números, doses ou condutas.
            3. Em condutas clínicas, doses de medicamentos e protocolos, oriente o estudante a confirmar no protocolo institucional ou com o preceptor.
            4. Use o contexto do estudante e o histórico da conversa para entender perguntas de continuação ("explica melhor", "e o tratamento?").
            5. Se a mensagem não for uma dúvida de estudo, responda em uma frase e convide o estudante a enviar uma dúvida de conteúdo.
            6. Use a formatação do WhatsApp: *negrito*, _itálico_ e listas curtas. Seja conciso: a leitura é no celular.""";

    private static final String REVIEW_RULE = """
            REGRA DA REVISÃO: o estudante ainda vai responder esta questão. NÃO revele qual alternativa é a \
            correta nem a resposta, nem de forma indireta (por eliminação ou confirmando um palpite), mesmo \
            que ele peça. Explique os conceitos necessários para que ele consiga raciocinar sozinho.""";

    private final ChatLanguageModel chatLanguageModel;
    private final ConversationHistoryStore conversationHistoryStore;
    private final MmeebbMetrics mmeebbMetrics;

    @Override
    public String answer(TutorContextDto context, String question) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("A dúvida do estudante não pode ser nula ou vazia.");
        }

        String trimmedQuestion = question.trim();
        String phoneNumber = context.phoneNumber();
        log.info("[AiTutor] Dúvida de [{}] (disciplina: {}, em revisão: {}): \"{}\"",
                phoneNumber, context.subjectName(), context.reviewQuestion() != null, trimmedQuestion);

        try {
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(SystemMessage.from(buildSystemPrompt(context)));
            for (ConversationTurn turn : conversationHistoryStore.recent(phoneNumber)) {
                // Uma troca pela metade não vira mensagem: o Gemini rejeitaria o texto vazio.
                if (isBlank(turn.question()) || isBlank(turn.answer())) {
                    continue;
                }
                messages.add(UserMessage.from(turn.question()));
                messages.add(AiMessage.from(turn.answer()));
            }
            messages.add(UserMessage.from(trimmedQuestion));

            Response<AiMessage> response = chatLanguageModel.generate(messages);
            String text = response != null && response.content() != null ? response.content().text() : null;
            if (isBlank(text)) {
                throw new IllegalStateException("O Gemini devolveu uma resposta vazia.");
            }

            String answer = text.trim();
            conversationHistoryStore.append(phoneNumber, trimmedQuestion, answer);
            mmeebbMetrics.recordAiInteraction("tutor", "gemini");
            return answer;

        } catch (Exception e) {
            mmeebbMetrics.recordAiInteraction("tutor", "fallback");
            log.error("[AiTutor] Falha ao responder a dúvida de [{}]: {}", phoneNumber, e.getMessage(), e);
            return UNAVAILABLE_MESSAGE;
        }
    }

    @Override
    public void clearConversation(String phoneNumber) {
        conversationHistoryStore.clear(phoneNumber);
    }

    private String buildSystemPrompt(TutorContextDto context) {
        StringBuilder prompt = new StringBuilder(SYSTEM_PROMPT);

        List<String> profile = new ArrayList<>();
        addLine(profile, "Nome", context.studentName());
        if (!isBlank(context.courseName())) {
            String period = context.academicPeriod() != null ? " (" + context.academicPeriod() + "º período)" : "";
            profile.add("- Curso: " + context.courseName().trim() + period);
        }
        addLine(profile, "Disciplina em foco", context.subjectName());
        if (!profile.isEmpty()) {
            prompt.append("\n\nCONTEXTO DO ESTUDANTE:\n").append(String.join("\n", profile));
        }

        TutorContextDto.ReviewQuestion review = context.reviewQuestion();
        if (review != null) {
            List<String> lines = new ArrayList<>();
            addLine(lines, "Tópico", review.topic());
            addLine(lines, "Enunciado", review.question());
            addLine(lines, "Alternativas", review.options());
            addLine(lines, "Gabarito (CONFIDENCIAL)", review.answer());
            addLine(lines, "Explicação (CONFIDENCIAL)", review.explanation());
            prompt.append("\n\nQUESTÃO QUE O ESTUDANTE ESTÁ REVISANDO AGORA:\n")
                    .append(String.join("\n", lines))
                    .append("\n\n")
                    .append(REVIEW_RULE);
        }

        return prompt.toString();
    }

    private void addLine(List<String> lines, String label, String value) {
        if (!isBlank(value)) {
            lines.add("- " + label + ": " + value.trim());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
