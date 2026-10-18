package br.edu.unipam.tcc.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DailyLimitPropertiesTest {

    @Test
    @DisplayName("Deve usar o limite do estudante e, sem ele, o padrão global")
    void deveResolverLimiteDoEstudanteOuPadrao() {
        DailyLimitProperties properties = new DailyLimitProperties(20, 5, 100, 0.25);

        assertEquals(20, properties.resolve(null));
        assertEquals(35, properties.resolve(35));
    }

    @Test
    @DisplayName("Deve aceitar só valores dentro da faixa, inclusive os extremos")
    void deveAceitarSoValoresDentroDaFaixa() {
        DailyLimitProperties properties = new DailyLimitProperties(20, 5, 100, 0.25);

        assertTrue(properties.accepts(5));
        assertTrue(properties.accepts(100));
        assertFalse(properties.accepts(4));
        assertFalse(properties.accepts(101));
    }

    @ParameterizedTest
    @CsvSource({
            "20, 5",
            "100, 25",
            "8, 2",
            "7, 1",
            "5, 1"     // limite pequeno ainda garante uma questão nova
    })
    @DisplayName("Deve reservar um quarto do limite para questões novas, com no mínimo uma vaga")
    void deveReservarUmQuartoDoLimiteParaNovas(int limit, int expected) {
        assertEquals(expected, new DailyLimitProperties(20, 5, 100, 0.25).reservedNewQuestions(limit));
    }

    @Test
    @DisplayName("Parcela zero deve desligar a reserva de questões novas")
    void parcelaZeroDesligaAReserva() {
        assertEquals(0, new DailyLimitProperties(20, 5, 100, 0).reservedNewQuestions(20));
    }

    @ParameterizedTest
    @CsvSource({
            "4, 5, 100, 0.25",    // padrão abaixo do mínimo
            "101, 5, 100, 0.25",  // padrão acima do máximo
            "20, 0, 100, 0.25",   // mínimo zero
            "20, 50, 10, 0.25",   // faixa invertida
            "20, 5, 100, -0.1",   // parcela negativa
            "20, 5, 100, 1.5"     // parcela acima do limite inteiro
    })
    @DisplayName("Deve impedir a subida com configuração inconsistente")
    void deveRecusarConfiguracaoInconsistente(int defaultLimit, int min, int max, double newShare) {
        assertThrows(IllegalStateException.class, () -> new DailyLimitProperties(defaultLimit, min, max, newShare));
    }
}
