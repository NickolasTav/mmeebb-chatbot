package br.edu.unipam.tcc.messaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AntiBanModeTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"global", "GLOBAL", "  Global  "})
    @DisplayName("Vazio ou 'global' mantém a estratégia anti-ban original (fila única)")
    void vazioOuGlobalViraGlobal(String valor) {
        assertEquals(AntiBanMode.GLOBAL, AntiBanMode.from(valor));
    }

    @ParameterizedTest
    @ValueSource(strings = {"per-recipient", "PER-RECIPIENT", " per-recipient "})
    @DisplayName("'per-recipient' liga o ritmo por destinatário")
    void perRecipientViraPerRecipient(String valor) {
        assertEquals(AntiBanMode.PER_RECIPIENT, AntiBanMode.from(valor));
    }

    @Test
    @DisplayName("Valor desconhecido falha na subida em vez de trocar a estratégia em silêncio")
    void valorDesconhecidoFalha() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> AntiBanMode.from("turbo"));
        assertTrue(erro.getMessage().contains("global"));
        assertTrue(erro.getMessage().contains("per-recipient"));
    }
}
