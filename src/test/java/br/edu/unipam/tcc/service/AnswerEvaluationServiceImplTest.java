package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.dto.AnswerEvaluationDto;
import br.edu.unipam.tcc.entity.Flashcard;
import br.edu.unipam.tcc.entity.enums.QuestionType;
import br.edu.unipam.tcc.service.impl.AnswerEvaluationServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnswerEvaluationServiceImplTest {

    private AnswerEvaluationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AnswerEvaluationServiceImpl(
                Mockito.mock(ChatLanguageModel.class), new ObjectMapper());
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
}
