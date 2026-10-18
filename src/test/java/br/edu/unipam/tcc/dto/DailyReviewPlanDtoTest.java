package br.edu.unipam.tcc.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DailyReviewPlanDtoTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "12 | 8 | 12 revisões + 8 novas",
            "1  | 1 | 1 revisão + 1 nova",
            "0  | 8 | 8 novas",
            "5  | 0 | 5 revisões",
            "0  | 1 | 1 nova",
            "1  | 0 | 1 revisão"
    })
    @DisplayName("Detalhamento deve usar singular e plural e omitir o lado vazio")
    void detalhamentoDeveFlexionarEOmitirLadoVazio(int reviews, int newQuestions, String expected) {
        assertEquals(expected, DailyReviewPlanDto.breakdown(reviews, newQuestions));
    }

    @Test
    @DisplayName("Sem nenhuma questão, o detalhamento deve ser vazio")
    void detalhamentoVazioSemQuestoes() {
        assertEquals("", DailyReviewPlanDto.breakdown(0, 0));
    }

    @Test
    @DisplayName("Meta concluída só quando a cota acabou e ainda há questões vencidas")
    void metaConcluidaSoComExcedente() {
        assertTrue(new DailyReviewPlanDto(List.of(), 0, 0, 20, 20, 361).goalReached());
        assertFalse(new DailyReviewPlanDto(List.of(), 0, 0, 20, 4, 0).goalReached());
        assertFalse(new DailyReviewPlanDto(List.of(), 3, 0, 20, 17, 50).goalReached());
    }
}
