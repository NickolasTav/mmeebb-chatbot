package br.edu.unipam.tcc.service.impl;

import br.edu.unipam.tcc.dto.IntentResultDto;
import br.edu.unipam.tcc.entity.enums.ChatIntent;
import br.edu.unipam.tcc.service.IntentRouterService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Classificador de intenção baseado no Google Gemini. Permite que o estudante escreva
 * livremente em vez de depender dos números do menu.
 */
@Slf4j
@Service
public class IntentRouterServiceImpl implements IntentRouterService {

    private static final String SYSTEM_PROMPT = """
            Você é o roteador de intenções de um chatbot de estudos via WhatsApp para estudantes universitários.
            Classifique a mensagem do estudante em EXATAMENTE uma das intenções abaixo:

            - START_REVIEW: quer estudar, revisar, praticar, responder questões ou flashcards pendentes.
            - ASK_DOUBT: fez uma pergunta de conteúdo acadêmico/clínico, ou pediu explicação sobre um tema.
            - CHANGE_SUBJECT: quer trocar, escolher ou mudar o curso ou a disciplina em foco.
            - SHOW_MENU: cumprimentou, pediu ajuda, pediu o menu ou perguntou como o sistema funciona.
            - EXIT: quer encerrar, sair ou se despedir.

            Responda SOMENTE com um objeto JSON válido, sem markdown e sem comentários, no formato:
            {"intent": "<INTENÇÃO>", "subject": "<disciplina citada ou null>"}

            O campo "subject" deve conter o nome da disciplina APENAS se o estudante citou uma
            explicitamente (ex.: "tenho dúvida em cardiologia" -> "cardiologia"). Caso contrário, use null.""";

    private final ChatLanguageModel chatLanguageModel;
    private final ObjectMapper objectMapper;

    public IntentRouterServiceImpl(ChatLanguageModel chatLanguageModel, ObjectMapper objectMapper) {
        this.chatLanguageModel = chatLanguageModel;
        this.objectMapper = objectMapper;
    }

    @Override
    public IntentResultDto classify(String message) {
        if (message == null || message.isBlank()) {
            return IntentResultDto.of(ChatIntent.SHOW_MENU);
        }

        try {
            Response<AiMessage> response = chatLanguageModel.generate(List.of(
                    SystemMessage.from(SYSTEM_PROMPT),
                    UserMessage.from("Mensagem do estudante: " + message.trim())
            ));

            String raw = response != null && response.content() != null ? response.content().text() : "";
            IntentResultDto parsed = parse(raw);

            log.info("[IntentRouter] \"{}\" -> {} (disciplina: {})",
                    message.trim(), parsed.intent(), parsed.subjectHint());
            return parsed;

        } catch (Exception e) {
            log.error("[IntentRouter] Falha ao classificar intenção, assumindo dúvida (RAG): {}", e.getMessage());
            return IntentResultDto.of(ChatIntent.ASK_DOUBT);
        }
    }

    private IntentResultDto parse(String raw) throws Exception {
        String cleaned = raw.trim()
                .replaceAll("(?s)^```(?:json)?\\s*", "")
                .replaceAll("(?s)\\s*```$", "")
                .trim();

        JsonNode node = objectMapper.readTree(cleaned);

        ChatIntent intent;
        try {
            intent = ChatIntent.valueOf(node.path("intent").asText("").trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("[IntentRouter] Intenção desconhecida retornada pelo modelo: \"{}\"", node.path("intent").asText());
            intent = ChatIntent.ASK_DOUBT;
        }

        JsonNode subjectNode = node.path("subject");
        String subject = subjectNode.isNull() || subjectNode.asText("").isBlank()
                ? null
                : subjectNode.asText().trim();

        return new IntentResultDto(intent, subject);
    }
}
