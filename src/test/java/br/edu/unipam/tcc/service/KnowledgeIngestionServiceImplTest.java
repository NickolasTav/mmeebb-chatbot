package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.service.impl.KnowledgeIngestionServiceImpl;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class KnowledgeIngestionServiceImplTest {

    private EmbeddingModel embeddingModel;
    private EmbeddingStore<TextSegment> embeddingStore;
    private KnowledgeIngestionServiceImpl ingestionService;

    @BeforeEach
    void setUp() {
        embeddingModel = Mockito.mock(EmbeddingModel.class);
        @SuppressWarnings("unchecked")
        EmbeddingStore<TextSegment> storeMock = Mockito.mock(EmbeddingStore.class);
        embeddingStore = storeMock;

        ingestionService = new KnowledgeIngestionServiceImpl(embeddingModel, embeddingStore);
    }

    @Test
    @DisplayName("Deve ingerir documentos com metadados relacionais e split recursivo")
    void deveIngerirDocumentosComMetadadosCorretos(@TempDir Path tempDir) throws IOException {
        // Arrange
        Path sampleFile = tempDir.resolve("cardiologia_insuficiencia_cardiaca.txt");
        String content = """
                A insuficiência cardíaca com fração de ejeção reduzida (ICFEr) é definida por FE < 40%.
                O tratamento farmacológico padrão inclui quatro pilares essenciais:
                1. Inibidores de SGLT2 (Dapagliflozina ou Empagliflozina);
                2. Betabloqueadores (Carvedilol, Bisoprolol ou Metoprolol Succinato);
                3. Antagonistas de Receptores de Mineralocorticoides (Espironolactona);
                4. IECA / BRA ou Inibidor do Receptor de Angiotensina e Neprilisina (Sacubitril/Valsartana).
                A otimização dessas medicações reduz mortalidade e hospitalização.
                """;
        Files.writeString(sampleFile, content);

        Embedding mockEmbedding = new Embedding(new float[]{0.1f, 0.2f});
        when(embeddingModel.embedAll(anyList())).thenAnswer(invocation -> {
            List<TextSegment> segments = invocation.getArgument(0);
            List<Embedding> embeddings = segments.stream().map(s -> mockEmbedding).toList();
            return Response.from(embeddings);
        });

        // Act
        int totalIngested = ingestionService.ingestSubjectDocuments(tempDir, 1L, 10L, "Insuficiência Cardíaca");

        // Assert
        assertThat(totalIngested).isGreaterThan(0);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TextSegment>> segmentsCaptor = ArgumentCaptor.forClass(List.class);
        verify(embeddingStore, times(1)).addAll(anyList(), segmentsCaptor.capture());

        List<TextSegment> capturedSegments = segmentsCaptor.getValue();
        assertThat(capturedSegments).isNotEmpty();
        for (TextSegment segment : capturedSegments) {
            assertThat(segment.metadata().getString("course_id")).isEqualTo("1");
            assertThat(segment.metadata().getString("subject_id")).isEqualTo("10");
            assertThat(segment.metadata().getString("topic")).isEqualTo("Insuficiência Cardíaca");
            assertThat(segment.text()).isNotEmpty();
        }
    }

    @Test
    @DisplayName("Deve lançar exceção quando diretório não existir")
    void deveLancarExcecaoQuandoDiretorioNaoExistir() {
        Path invalidDir = Path.of("c:/diretorio/inexistente/para/teste");

        assertThatThrownBy(() -> ingestionService.ingestSubjectDocuments(invalidDir, 1L, 10L, "Tópico"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Diretório de documentos não existe");
    }

    @Test
    @DisplayName("Deve lançar exceção quando parâmetros obrigatórios forem nulos")
    void deveLancarExcecaoQuandoParametrosForemNulos(@TempDir Path tempDir) {
        assertThatThrownBy(() -> ingestionService.ingestSubjectDocuments(null, 1L, 10L, "Tópico"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> ingestionService.ingestSubjectDocuments(tempDir, null, 10L, "Tópico"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> ingestionService.ingestSubjectDocuments(tempDir, 1L, null, "Tópico"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> ingestionService.ingestSubjectDocuments(tempDir, 1L, 10L, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
