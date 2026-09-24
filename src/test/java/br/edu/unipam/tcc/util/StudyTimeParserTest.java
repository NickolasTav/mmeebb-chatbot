package br.edu.unipam.tcc.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StudyTimeParserTest {

    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @DisplayName("Deve aceitar os formatos de horário 24h usados no WhatsApp")
    @CsvSource(delimiter = '|', value = {
            "12:15|12:15",
            "07:05|07:05",
            "7:05|07:05",
            "12.15|12:15",
            "12h15|12:15",
            "12H15|12:15",
            "12h15min|12:15",
            "12h|12:00",
            "12|12:00",
            "9|09:00",
            "0|00:00",
            "12 h 15|12:15",
            "' 18:30 '|18:30",
            "00:00|00:00",
            "23:59|23:59"
    })
    void shouldParseAcceptedFormats(String input, String expected) {
        assertEquals(Optional.of(LocalTime.parse(expected)), StudyTimeParser.parse(input));
    }

    @ParameterizedTest(name = "\"{0}\" deve ser rejeitado")
    @DisplayName("Deve rejeitar horários fora da faixa ou ambíguos")
    @ValueSource(strings = {"24:00", "12:60", "25h", "7:5", "1215", "123", "-3", "12:", "12:15:30", "abc", "meio-dia", "", "   "})
    void shouldRejectInvalidInputs(String input) {
        assertEquals(Optional.empty(), StudyTimeParser.parse(input));
    }

    @Test
    @DisplayName("Deve rejeitar entrada nula")
    void shouldRejectNull() {
        assertEquals(Optional.empty(), StudyTimeParser.parse(null));
    }
}
