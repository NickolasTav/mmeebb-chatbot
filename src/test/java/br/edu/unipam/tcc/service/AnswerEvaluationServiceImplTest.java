package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.dto.AnswerEvaluationDto;
import br.edu.unipam.tcc.entity.Flashcard;
import br.edu.unipam.tcc.entity.enums.QuestionType;
import br.edu.unipam.tcc.observability.MmeebbMetrics;
import br.edu.unipam.tcc.service.impl.AnswerEvaluationServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

class AnswerEvaluationServiceImplTest {

    private ChatLanguageModel chatLanguageModel;
    private AnswerEvaluationServiceImpl service;

    @BeforeEach
    void setUp() {
        chatLanguageModel = Mockito.mock(ChatLanguageModel.class);
        service = new AnswerEvaluationServiceImpl(chatLanguageModel, new ObjectMapper(), Mockito.mock(MmeebbMetrics.class));
    }

    private void stubGeminiResponse(String json) {
        Response<AiMessage> response = Response.from(AiMessage.from(json));
        when(chatLanguageModel.generate(anyList())).thenReturn(response);
    }

    private Flashcard multipleChoice(String gabarito) {
        return Flashcard.builder()
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .question("Qual a terapia padrão-ouro na ICFEr?")
                .answer(gabarito)
                .build();
    }

    @ParameterizedTest
    @DisplayName("Deve aceitar múltipla escolha pela letra inicial, mesmo com a alternativa colada inteira")
    @ValueSource(strings = {
            "A",
            "a",
            "A)",
            "a) ",
            "letra A",
            "alternativa a",
            "A) Inibidor de SGLT2 + Betabloqueador + Antagonista do Receptor Mineralocorticoide + IECA/BRA/INRA"
    })
    void shouldAcceptChoiceByLeadingLetter(String studentAnswer) {
        AnswerEvaluationDto result = service.evaluate(studentAnswer, multipleChoice("A"));
        assertTrue(result.correct(), "Deveria aceitar a resposta: \"" + studentAnswer + "\"");
    }

    @ParameterizedTest
    @DisplayName("Deve recusar múltipla escolha quando a letra inicial não corresponde ao gabarito")
    @ValueSource(strings = {
            "B",
            "C) Amiodarona + Espironolactona",
            "letra D",
            "amiodarona com espironolactona"
    })
    void shouldRejectWrongChoice(String studentAnswer) {
        AnswerEvaluationDto result = service.evaluate(studentAnswer, multipleChoice("A"));
        assertFalse(result.correct(), "Deveria recusar a resposta: \"" + studentAnswer + "\"");
    }

    @Test
    @DisplayName("Deve aceitar resposta dissertativa idêntica sem acionar o modelo de linguagem")
    void shouldAcceptExactDissertativeAnswerWithoutLlm() {
        Flashcard card = Flashcard.builder()
                .questionType(QuestionType.FLASHCARD)
                .question("Defina lesão renal aguda.")
                .answer("Aumento da creatinina sérica >= 0.3 mg/dL em 48h")
                .build();

        AnswerEvaluationDto result = service.evaluate(
                "aumento da creatinina serica >= 0.3 mg/dl em 48h", card);

        assertTrue(result.correct());
    }

    @Test
    @DisplayName("Deve recusar resposta vazia sem acionar o modelo de linguagem")
    void shouldRejectBlankAnswer() {
        assertFalse(service.evaluate("   ", multipleChoice("A")).correct());
    }

    @Test
    @DisplayName("Deve sinalizar dúvida em questão de múltipla escolha sem pontuar como errada")
    void shouldFlagDoubtOnMultipleChoiceWithoutScoring() {
        stubGeminiResponse("{\"correct\": false, \"feedback\": null, \"isDoubt\": true}");

        AnswerEvaluationDto result = service.evaluate("não entendi, pode explicar essa questão?", multipleChoice("A"));

        assertTrue(result.isDoubt());
        assertFalse(result.correct());
    }

    @Test
    @DisplayName("Deve sinalizar dúvida em questão discursiva sem pontuar como errada")
    void shouldFlagDoubtOnDissertativeWithoutScoring() {
        Flashcard card = Flashcard.builder()
                .questionType(QuestionType.FLASHCARD)
                .question("Defina lesão renal aguda.")
                .answer("Aumento da creatinina sérica >= 0.3 mg/dL em 48h")
                .build();
        stubGeminiResponse("{\"correct\": false, \"feedback\": null, \"isDoubt\": true}");

        AnswerEvaluationDto result = service.evaluate("não sei, me dá uma dica?", card);

        assertTrue(result.isDoubt());
        assertFalse(result.correct());
    }

    @Test
    @DisplayName("Não deve acionar o modelo de linguagem quando a resposta bate no fast-path de letra")
    void shouldNotCallLlmWhenChoiceLetterFastPathMatches() {
        AnswerEvaluationDto result = service.evaluate("letra A", multipleChoice("A"));

        assertTrue(result.correct());
        assertFalse(result.isDoubt());
        Mockito.verifyNoInteractions(chatLanguageModel);
    }

    @Test
    @DisplayName("Deve manter isDoubt falso quando o Gemini classifica como tentativa de resposta")
    void shouldKeepIsDoubtFalseWhenGeminiClassifiesAsAnswerAttempt() {
        Flashcard card = Flashcard.builder()
                .questionType(QuestionType.FLASHCARD)
                .question("Defina lesão renal aguda.")
                .answer("Aumento da creatinina sérica >= 0.3 mg/dL em 48h")
                .build();
        stubGeminiResponse("{\"correct\": false, \"feedback\": \"Quase lá, revise o valor de corte.\", \"isDoubt\": false}");

        AnswerEvaluationDto result = service.evaluate("acho que é qualquer aumento da creatinina", card);

        assertFalse(result.isDoubt());
        assertFalse(result.correct());
    }
}
