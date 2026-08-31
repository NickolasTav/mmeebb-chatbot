package br.edu.unipam.tcc.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

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

    @Test
    @DisplayName("Deve instanciar EmbeddingModel com modelo text-embedding-004")
    void deveInstanciarEmbeddingModelComDimensoes768() {
        LangChain4jConfig config = new LangChain4jConfig();
        ReflectionTestUtils.setField(config, "geminiApiKey", "test-api-key");

        EmbeddingModel embeddingModel = config.embeddingModel();

        assertThat(embeddingModel).isNotNull();
    }

    @Test
    @DisplayName("Deve instanciar PgVectorEmbeddingStore com DataSource e tabela tb_knowledge_embedding")
    void deveInstanciarEmbeddingStoreApontandoParaPgVector() throws SQLException {
        LangChain4jConfig config = new LangChain4jConfig();
        DataSource mockDataSource = Mockito.mock(DataSource.class);
        Connection mockConnection = Mockito.mock(Connection.class);
        DatabaseMetaData mockMetaData = Mockito.mock(DatabaseMetaData.class);
        java.sql.Statement mockStatement = Mockito.mock(java.sql.Statement.class);
        org.postgresql.PGConnection mockPGConnection = Mockito.mock(org.postgresql.PGConnection.class);

        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.getMetaData()).thenReturn(mockMetaData);
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockConnection.unwrap(org.postgresql.PGConnection.class)).thenReturn(mockPGConnection);
        when(mockMetaData.getDatabaseProductName()).thenReturn("PostgreSQL");

        EmbeddingStore<TextSegment> store = config.embeddingStore(mockDataSource);

        assertThat(store).isNotNull();
    }
}
