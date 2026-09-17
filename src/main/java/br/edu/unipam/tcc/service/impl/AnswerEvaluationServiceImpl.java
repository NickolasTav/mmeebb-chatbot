package br.edu.unipam.tcc.service.impl;

import br.edu.unipam.tcc.dto.AnswerEvaluationDto;
import br.edu.unipam.tcc.entity.Flashcard;
import br.edu.unipam.tcc.entity.enums.QuestionType;
import br.edu.unipam.tcc.observability.MmeebbMetrics;
import br.edu.unipam.tcc.service.AnswerEvaluationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Avaliação de respostas com correção semântica via Google Gemini.
 * Substitui a comparação literal, que reprovava qualquer resposta escrita
 * com as palavras do próprio estudante.
 */
@Slf4j
@Service
public class AnswerEvaluationServiceImpl implements AnswerEvaluationService {

    private static final String SYSTEM_PROMPT = """
            Você é um preceptor acadêmico avaliando a interação de um estudante em uma revisão de flashcard.

            Primeiro decida se a mensagem do estudante é:
            (a) uma TENTATIVA DE RESPOSTA à pergunta, mesmo que incompleta, vaga ou com outras palavras; ou
            (b) um PEDIDO DE AJUDA/DÚVIDA sobre a questão (ex.: "não entendi", "pode explicar?", "por que não
            é a B?", "não sei", "me dá uma dica").

            Se for pedido de ajuda/dúvida, responda exatamente:
            {"correct": false, "feedback": null, "isDoubt": true}

            Se for tentativa de resposta, avalie comparando com o gabarito. Considere CORRETA quando ela
            expressa o mesmo conceito do gabarito, ainda que com outras palavras, sinônimos, abreviações
            clínicas usuais ou menos detalhes. Considere INCORRETA quando contradiz o gabarito, cita outro
            conceito ou é vaga a ponto de não demonstrar domínio. Responda:
            {"correct": true|false, "feedback": "<comentário de no máximo 200 caracteres>", "isDoubt": false}

            Responda SOMENTE com um objeto JSON válido, sem markdown, em um dos dois formatos acima. O
            feedback deve ser direto e pedagógico, em português do Brasil, dirigido ao estudante.""";

    private static final Pattern CHOICE_LETTER =
            Pattern.compile("^(?:letra|opcao|alternativa)?\\s*([a-z])(?:\\s|$)");

    private final ChatLanguageModel chatLanguageModel;
    private final ObjectMapper objectMapper;
    private final MmeebbMetrics mmeebbMetrics;

    public AnswerEvaluationServiceImpl(ChatLanguageModel chatLanguageModel, ObjectMapper objectMapper, MmeebbMetrics mmeebbMetrics) {
        this.chatLanguageModel = chatLanguageModel;
        this.objectMapper = objectMapper;
        this.mmeebbMetrics = mmeebbMetrics;
    }

    @Override
    public AnswerEvaluationDto evaluate(String studentAnswer, Flashcard flashcard) {
        if (flashcard == null) {
            throw new IllegalArgumentException("O flashcard em revisão é obrigatório para avaliar a resposta.");
        }
        if (studentAnswer == null || studentAnswer.isBlank()) {
            return AnswerEvaluationDto.rejected();
        }

        String student = studentAnswer.trim().replaceAll("\\s+", " ");
        String expected = flashcard.getAnswer().trim().replaceAll("\\s+", " ");

        if (normalize(student).equals(normalize(expected))) {
            mmeebbMetrics.recordAiInteraction("answer_evaluation", "fast_path");
            return AnswerEvaluationDto.accepted();
        }

        if (flashcard.getQuestionType() == QuestionType.MULTIPLE_CHOICE && isChoiceLetterFastPath(student, expected)) {
            mmeebbMetrics.recordAiInteraction("answer_evaluation", "fast_path");
            return new AnswerEvaluationDto(matchesChoiceLetter(student, expected), null, false);
        }

        return evaluateSemantically(student, expected, flashcard);
    }

    /**
     * Fast-path determinístico: só resolve localmente quando o gabarito é uma letra única
     * e o estudante respondeu com o padrão esperado (letra solta, "letra A", alternativa colada).
     * Qualquer outro texto (incluindo pedidos de ajuda) segue para a avaliação via Gemini.
     */
    private boolean isChoiceLetterFastPath(String student, String expected) {
        String expectedLetter = normalize(expected);
        if (expectedLetter.length() != 1) {
            return true;
        }
        return CHOICE_LETTER.matcher(normalize(student)).find();
    }

    private AnswerEvaluationDto evaluateSemantically(String student, String expected, Flashcard flashcard) {
        try {
            String prompt = String.format("""
                    PERGUNTA: %s

                    GABARITO OFICIAL: %s

                    RESPOSTA DO ESTUDANTE: %s""", flashcard.getQuestion(), expected, student);

            Response<AiMessage> response = chatLanguageModel.generate(List.of(
                    SystemMessage.from(SYSTEM_PROMPT),
                    UserMessage.from(prompt)
            ));

            String raw = response != null && response.content() != null ? response.content().text() : "";
            String cleaned = raw.trim()
                    .replaceAll("(?s)^```(?:json)?\\s*", "")
                    .replaceAll("(?s)\\s*```$", "")
                    .trim();

            JsonNode node = objectMapper.readTree(cleaned);
            mmeebbMetrics.recordAiInteraction("answer_evaluation", "gemini");
            boolean isDoubt = node.path("isDoubt").asBoolean(false);
            if (isDoubt) {
                log.info("[AnswerEvaluation] Flashcard {}: estudante pediu ajuda/explicação em vez de responder",
                        flashcard.getId());
                return AnswerEvaluationDto.doubt();
            }

            boolean correct = node.path("correct").asBoolean(false);
            String feedback = node.path("feedback").asText("").isBlank() ? null : node.path("feedback").asText().trim();

            log.info("[AnswerEvaluation] Flashcard {} avaliado semanticamente: {}",
                    flashcard.getId(), correct ? "CORRETO" : "INCORRETO");
            return new AnswerEvaluationDto(correct, feedback, false);

        } catch (Exception e) {
            mmeebbMetrics.recordAiInteraction("answer_evaluation", "fallback");
            log.error("[AnswerEvaluation] Falha na correção semântica do flashcard {}: {}",
                    flashcard.getId(), e.getMessage());
            return AnswerEvaluationDto.rejected();
        }
    }

    /**
     * O gabarito de múltipla escolha é apenas a letra, mas o estudante costuma responder
     * colando a alternativa inteira ("A) Inibidor de SGLT2..."). Extrai a letra inicial.
     */
    private boolean matchesChoiceLetter(String student, String expected) {
        String expectedLetter = normalize(expected);
        if (expectedLetter.length() != 1) {
            return normalize(student).equals(expectedLetter);
        }

        Matcher matcher = CHOICE_LETTER.matcher(normalize(student));
        return matcher.find() && matcher.group(1).equals(expectedLetter);
    }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9 ]", "")
                .trim();
    }
}
