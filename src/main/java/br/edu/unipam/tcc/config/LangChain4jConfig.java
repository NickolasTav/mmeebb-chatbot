package br.edu.unipam.tcc.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.time.Duration;

/**
 * Configuração Spring para integração do LangChain4j com Google Gemini e pgvector.
 * Configura os beans ChatLanguageModel, EmbeddingModel (gemini-embedding-001 com 768 dimensões)
 * e PgVectorEmbeddingStore apontando para a tabela tb_knowledge_embedding.
 */
@Slf4j
@Configuration
public class LangChain4jConfig {

    public static final String EMBEDDING_TABLE = "tb_knowledge_embedding";
    public static final int EMBEDDING_DIMENSION = 768;
    public static final String DEFAULT_EMBEDDING_MODEL_NAME = "gemini-embedding-001";
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);
    public static final int DEFAULT_MAX_RETRIES = 3;

    @Value("${gemini.api-key:}")
    private String geminiApiKey;

    @Value("${gemini.model-name:gemini-3.5-flash-lite}")
    private String geminiModelName;

    @Value("${gemini.embedding-model-name:gemini-embedding-001}")
    private String geminiEmbeddingModelName;

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

        return GoogleAiGeminiChatModel.builder()
                .apiKey(effectiveKey)
                .modelName(effectiveChatModel)
                .temperature(geminiTemperature)
                .timeout(DEFAULT_TIMEOUT)
                .maxRetries(DEFAULT_MAX_RETRIES)
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        String effectiveKey = resolveApiKey();
        String effectiveEmbeddingModel = (geminiEmbeddingModelName != null && !geminiEmbeddingModelName.isBlank())
                ? geminiEmbeddingModelName.trim()
                : DEFAULT_EMBEDDING_MODEL_NAME;

        log.info("[LangChain4jConfig] Inicializando EmbeddingModel (Google Gemini: {}, Dim: {}, Timeout: {}s)",
                effectiveEmbeddingModel, EMBEDDING_DIMENSION, DEFAULT_TIMEOUT.getSeconds());

        return GoogleAiEmbeddingModel.builder()
                .apiKey(effectiveKey)
                .modelName(effectiveEmbeddingModel)
                .outputDimensionality(EMBEDDING_DIMENSION)
                .timeout(DEFAULT_TIMEOUT)
                .maxRetries(DEFAULT_MAX_RETRIES)
                .build();
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(DataSource dataSource) {
        log.info("[LangChain4jConfig] Inicializando PgVectorEmbeddingStore (Tabela: {}, Dim: {})",
                EMBEDDING_TABLE, EMBEDDING_DIMENSION);

        return PgVectorEmbeddingStore.datasourceBuilder()
                .datasource(dataSource)
                .table(EMBEDDING_TABLE)
                .dimension(EMBEDDING_DIMENSION)
                .useIndex(true)
                .indexListSize(100)
                .createTable(false)
                .dropTableFirst(false)
                .build();
    }

    private String resolveApiKey() {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            log.warn("[LangChain4jConfig] Chave gemini.api-key não informada. Utilizando placeholder temporário para inicialização segura.");
            return "dummy-key-for-initialization";
        }
        return geminiApiKey.trim();
    }
}
