package br.edu.unipam.tcc.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeConfigTest {

    @Test
    @DisplayName("Deve fornecer relógio no fuso de São Paulo independente do fuso da JVM")
    void deveFornecerRelogioDeSaoPaulo() {
        assertEquals(ZoneId.of("America/Sao_Paulo"), new TimeConfig().clock().getZone());
    }
}
