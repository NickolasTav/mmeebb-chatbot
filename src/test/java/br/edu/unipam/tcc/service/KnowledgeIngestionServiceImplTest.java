package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.entity.Course;
import br.edu.unipam.tcc.entity.Flashcard;
import br.edu.unipam.tcc.entity.Subject;
import br.edu.unipam.tcc.entity.enums.DifficultyLevel;
import br.edu.unipam.tcc.entity.enums.QuestionType;
import br.edu.unipam.tcc.repository.FlashcardRepository;
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
    private FlashcardRepository flashcardRepository;
    private KnowledgeIngestionServiceImpl ingestionService;

    @BeforeEach
    void setUp() {
        embeddingModel = Mockito.mock(EmbeddingModel.class);
        @SuppressWarnings("unchecked")
        EmbeddingStore<TextSegment> storeMock = Mockito.mock(EmbeddingStore.class);
        embeddingStore = storeMock;
        flashcardRepository = Mockito.mock(FlashcardRepository.class);

        ingestionService = new KnowledgeIngestionServiceImpl(embeddingModel, embeddingStore, flashcardRepository);
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
    @DisplayName("Deve sincronizar flashcards ativos como conhecimento no pgvector quando filtrado por subjectId")
    void deveSincronizarFlashcardsPorSubjectId() {
        // Arrange
        Course course = Course.builder().id(1L).name("Medicina").code("MEDICINA").build();
        Subject subject = Subject.builder().id(10L).name("Clínica Médica").course(course).code("CLIN_MED").build();

        Flashcard card = Flashcard.builder()
                .id(100L)
                .subject(subject)
                .topic("Cardiologia - SCA")
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .question("Qual a conduta prioritária no IAM com supra de ST?")
                .answer("A")
                .optionsJson("[\"A) Reperfusão imediata\", \"B) Observação\"]")
                .explanation("O tempo porta-balão alvo para angioplastia primária é de até 90 minutos segundo a SBC.")
                .difficulty(DifficultyLevel.HARD)
                .source("SBC 2024")
                .active(true)
                .build();

        when(flashcardRepository.findBySubjectIdAndActiveTrue(10L)).thenReturn(List.of(card));

        Embedding mockEmbedding = new Embedding(new float[]{0.3f, 0.4f});
        when(embeddingModel.embedAll(anyList())).thenAnswer(invocation -> {
            List<TextSegment> segments = invocation.getArgument(0);
            List<Embedding> embeddings = segments.stream().map(s -> mockEmbedding).toList();
            return Response.from(embeddings);
        });

        // Act
        int totalIngested = ingestionService.ingestFlashcardsAsKnowledge(1L, 10L);

        // Assert
        assertThat(totalIngested).isEqualTo(1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TextSegment>> segmentsCaptor = ArgumentCaptor.forClass(List.class);
        verify(embeddingStore).addAll(anyList(), segmentsCaptor.capture());

        List<TextSegment> segments = segmentsCaptor.getValue();
        assertThat(segments).hasSize(1);
        TextSegment seg = segments.get(0);
        assertThat(seg.metadata().getString("course_id")).isEqualTo("1");
        assertThat(seg.metadata().getString("subject_id")).isEqualTo("10");
        assertThat(seg.metadata().getString("topic")).isEqualTo("Cardiologia - SCA");
        assertThat(seg.metadata().getString("source")).isEqualTo("flashcard_100");
        assertThat(seg.text()).contains("Qual a conduta prioritária no IAM com supra de ST?");
        assertThat(seg.text()).contains("tempo porta-balão");
    }

    @Test
    @DisplayName("Deve retornar 0 quando não houver flashcards para sincronizar")
    void deveRetornarZeroQuandoNaoHouverFlashcards() {
        when(flashcardRepository.findBySubjectIdAndActiveTrue(99L)).thenReturn(List.of());

        int result = ingestionService.ingestFlashcardsAsKnowledge(null, 99L);

        assertThat(result).isZero();
        verifyNoInteractions(embeddingModel);
        verifyNoInteractions(embeddingStore);
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
