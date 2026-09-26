package br.edu.unipam.tcc.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Garante que a V9 removeu a base vetorial do RAG (spec-09) sem tocar nas tabelas do MMEEBB.
 * A imagem continua sendo a do pgvector porque a V1 histórica cria a extensão {@code vector}.
 * Sem Docker disponível, a classe é ignorada.
 */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class KnowledgeBaseRemovalMigrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("A V9 deve remover a tabela vetorial do banco de conhecimento")
    void shouldDropKnowledgeEmbeddingTable() {
        assertThat(tableExists("tb_knowledge_embedding")).isFalse();
    }

    @Test
    @DisplayName("A V9 não deve tocar nas tabelas do MMEEBB")
    void shouldKeepReviewTables() {
        assertThat(tableExists("tb_flashcard")).isTrue();
        assertThat(tableExists("tb_repetition_schedule")).isTrue();
        assertThat(tableExists("tb_review_attempt")).isTrue();
    }

    private boolean tableExists(String table) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?",
                Integer.class, table);
        return count != null && count > 0;
    }
}
