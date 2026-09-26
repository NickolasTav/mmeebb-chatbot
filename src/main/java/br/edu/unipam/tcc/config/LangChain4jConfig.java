package br.edu.unipam.tcc.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuração Spring do LangChain4j com o Google Gemini. Expõe apenas o ChatLanguageModel usado
 * pelo tutor, pelo roteador de intenção, pela correção de respostas e pelo diagnóstico de
 * desempenho: desde a spec-09 não há base vetorial nem modelo de embeddings.
 */
@Slf4j
@Configuration
public class LangChain4jConfig {

    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);
    public static final int DEFAULT_MAX_RETRIES = 3;

    @Value("${gemini.api-key:}")
    private String geminiApiKey;

    @Value("${gemini.model-name:gemini-3.5-flash-lite}")
    private String geminiModelName;

    @Value("${gemini.temperature:0.2}")
    private Double geminiTemperature;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        String effectiveKey = resolveApiKey();
        String effectiveChatModel = (geminiModelName != null && !geminiModelName.isBlank())
                ? geminiModelName.trim()
                : "gemini-3.5-flash-lite";

        log.info("[LangChain4jConfig] Inicializando ChatLanguageModel (Google Gemini: {}, Temp: {}, Timeout: {}s)",
                effectiveChatModel, geminiTemperature, DEFAULT_TIMEOUT.getSeconds());

        GoogleAiGeminiChatModel chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(effectiveKey)
                .modelName(effectiveChatModel)
                .temperature(geminiTemperature)
                .timeout(DEFAULT_TIMEOUT)
                .maxRetries(DEFAULT_MAX_RETRIES)
                .build();

        dev.langchain4j.model.googleai.GeminiServiceCustomizer.configureTimeouts(chatModel, DEFAULT_TIMEOUT);
        return chatModel;
    }

    private String resolveApiKey() {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            log.warn("[LangChain4jConfig] Chave gemini.api-key não informada. Utilizando placeholder temporário para inicialização segura.");
            return "dummy-key-for-initialization";
        }
        return geminiApiKey.trim();
    }
}
