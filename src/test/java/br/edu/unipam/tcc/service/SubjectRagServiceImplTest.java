package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.service.impl.SubjectRagServiceImpl;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SubjectRagServiceImplTest {

    private EmbeddingModel embeddingModel;
    private EmbeddingStore<TextSegment> embeddingStore;
    private ChatLanguageModel chatLanguageModel;
    private SubjectRagServiceImpl subjectRagService;

    @BeforeEach
    void setUp() {
        embeddingModel = Mockito.mock(EmbeddingModel.class);
        @SuppressWarnings("unchecked")
        EmbeddingStore<TextSegment> storeMock = Mockito.mock(EmbeddingStore.class);
        embeddingStore = storeMock;
        chatLanguageModel = Mockito.mock(ChatLanguageModel.class);

        subjectRagService = new SubjectRagServiceImpl(embeddingModel, embeddingStore, chatLanguageModel);
    }

    @Test
    @DisplayName("Deve responder dúvida com contexto recuperado e isolado por disciplina")
    void deveResponderDuvidaComContextoRecuperadoDoPgVector() {
        // Arrange
        String question = "Quais são os 4 pilares do tratamento da insuficiência cardíaca com fração reduzida?";
        Long subjectId = 10L;

        Embedding queryEmbedding = new Embedding(new float[]{0.1f, 0.2f});
        when(embeddingModel.embed(question)).thenReturn(Response.from(queryEmbedding));

        TextSegment contextSegment = TextSegment.from("Tratamento IC: iSGLT2, Betabloqueador, Espironolactona e IECA/BRA/Sacubitril.");
        EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(0.85, "match-id", queryEmbedding, contextSegment);
        EmbeddingSearchResult<TextSegment> searchResult = new EmbeddingSearchResult<>(List.of(match));

        when(embeddingStore.search(any(EmbeddingSearchRequest.class))).thenReturn(searchResult);

        String generatedAnswer = "💡 *Tutor MMEEBB:* Os 4 pilares são: 1. iSGLT2, 2. Betabloqueador, 3. Espironolactona e 4. Sacubitril/Valsartana.";
        when(chatLanguageModel.generate(anyList())).thenReturn(Response.from(AiMessage.from(generatedAnswer)));

        // Act
        String result = subjectRagService.answerDoubt(question, subjectId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).contains("4 pilares");

        // Valida que o filtro por subject_id foi aplicado na busca
        ArgumentCaptor<EmbeddingSearchRequest> searchCaptor = ArgumentCaptor.forClass(EmbeddingSearchRequest.class);
        verify(embeddingStore, times(1)).search(searchCaptor.capture());

        EmbeddingSearchRequest capturedRequest = searchCaptor.getValue();
        assertThat(capturedRequest.maxResults()).isEqualTo(4);
        assertThat(capturedRequest.filter()).isInstanceOf(IsEqualTo.class);

        IsEqualTo isEqualToFilter = (IsEqualTo) capturedRequest.filter();
        assertThat(isEqualToFilter.key()).isEqualTo("subject_id");
        assertThat(isEqualToFilter.comparisonValue()).isEqualTo("10");
    }

    @Test
    @DisplayName("Deve retornar aviso padrão anti-alucinação quando nenhum trecho for encontrado no pgvector")
    void deveRetornarMensagemPadraoQuandoNenhumTrechoForEncontrado() {
        // Arrange
        String question = "Qual é a dosagem de um medicamento não documentado?";
        Long subjectId = 10L;

        Embedding queryEmbedding = new Embedding(new float[]{0.1f, 0.2f});
        when(embeddingModel.embed(question)).thenReturn(Response.from(queryEmbedding));

        EmbeddingSearchResult<TextSegment> emptySearchResult = new EmbeddingSearchResult<>(Collections.emptyList());
        when(embeddingStore.search(any(EmbeddingSearchRequest.class))).thenReturn(emptySearchResult);

        // Act
        String result = subjectRagService.answerDoubt(question, subjectId);

        // Assert
        assertThat(result).contains("Não encontrei referências");
        verify(chatLanguageModel, never()).generate(anyList());
    }

    @Test
    @DisplayName("Deve lançar exceção quando pergunta ou subjectId forem nulos ou vazios")
    void deveLancarExcecaoQuandoParametrosForemNulosOuVazios() {
        assertThatThrownBy(() -> subjectRagService.answerDoubt(null, 10L))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> subjectRagService.answerDoubt("   ", 10L))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> subjectRagService.answerDoubt("Pergunta válida", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
