package br.edu.unipam.tcc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class MmeebbChatbotApplicationTests {

    @Test
    @DisplayName("Smoke Test: Deve validar existência da classe principal da aplicação")
    void smokeTestAppClassLoads() {
        MmeebbChatbotApplication app = new MmeebbChatbotApplication();
        assertNotNull(app);
    }
}
