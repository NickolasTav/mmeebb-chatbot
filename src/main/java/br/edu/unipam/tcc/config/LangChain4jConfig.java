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

/**
 * Configuração Spring para integração do LangChain4j com Google Gemini e pgvector.
 * Configura os beans ChatLanguageModel, EmbeddingModel (text-embedding-004 com 768 dimensões)
 * e PgVectorEmbeddingStore apontando para a tabela tb_knowledge_embedding.
 */
@Slf4j
@Configuration
public class LangChain4jConfig {

    public static final String EMBEDDING_TABLE = "tb_knowledge_embedding";
    public static final int EMBEDDING_DIMENSION = 768;
    public static final String EMBEDDING_MODEL_NAME = "text-embedding-004";

    @Value("${gemini.api-key:}")
    private String geminiApiKey;

    @Value("${gemini.model-name:gemini-1.5-flash}")
    private String geminiModelName;

    @Value("${gemini.temperature:0.2}")
    private Double geminiTemperature;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        String effectiveKey = resolveApiKey();
        log.info("[LangChain4jConfig] Inicializando ChatLanguageModel (Google Gemini: {}, Temp: {})",
                geminiModelName, geminiTemperature);

        return GoogleAiGeminiChatModel.builder()
                .apiKey(effectiveKey)
                .modelName(geminiModelName)
                .temperature(geminiTemperature)
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        String effectiveKey = resolveApiKey();
        log.info("[LangChain4jConfig] Inicializando EmbeddingModel (Google Gemini: {}, Dim: {})",
                EMBEDDING_MODEL_NAME, EMBEDDING_DIMENSION);

        return GoogleAiEmbeddingModel.builder()
                .apiKey(effectiveKey)
                .modelName(EMBEDDING_MODEL_NAME)
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
