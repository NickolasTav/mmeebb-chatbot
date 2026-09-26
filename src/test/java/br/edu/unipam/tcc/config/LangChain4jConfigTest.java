package br.edu.unipam.tcc.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class LangChain4jConfigTest {

    @Test
    @DisplayName("Deve instanciar ChatLanguageModel com configurações corretas")
    void deveInstanciarChatLanguageModelComConfiguracoesCorretas() {
        LangChain4jConfig config = new LangChain4jConfig();
        ReflectionTestUtils.setField(config, "geminiApiKey", "test-api-key");
        ReflectionTestUtils.setField(config, "geminiModelName", "gemini-1.5-flash");
        ReflectionTestUtils.setField(config, "geminiTemperature", 0.2);

        ChatLanguageModel chatModel = config.chatLanguageModel();

        assertThat(chatModel).isNotNull();
    }
}
