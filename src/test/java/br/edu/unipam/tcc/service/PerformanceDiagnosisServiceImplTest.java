package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.dto.StudentPerformanceReportDto;
import br.edu.unipam.tcc.dto.TopicPerformanceDto;
import br.edu.unipam.tcc.entity.enums.TopicMastery;
import br.edu.unipam.tcc.observability.MmeebbMetrics;
import br.edu.unipam.tcc.service.impl.PerformanceDiagnosisServiceImpl;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PerformanceDiagnosisServiceImplTest {

    private ChatLanguageModel chatLanguageModel;
    private MmeebbMetrics mmeebbMetrics;
    private PerformanceDiagnosisService service;

    @BeforeEach
    void setUp() {
        chatLanguageModel = mock(ChatLanguageModel.class);
        mmeebbMetrics = mock(MmeebbMetrics.class);
        service = new PerformanceDiagnosisServiceImpl(chatLanguageModel, mmeebbMetrics);
    }

    private StudentPerformanceReportDto report(boolean enoughData, TopicPerformanceDto... topics) {
        return new StudentPerformanceReportDto(UUID.randomUUID(), "Mari", 90, 20L, 12L, 0.6,
                enoughData, List.of(topics));
    }

    private TopicPerformanceDto topic(String name, double errorRate, TopicMastery mastery) {
        return new TopicPerformanceDto(4L, "Farmacologia Clinica", name,
                10L, Math.round(errorRate * 10), errorRate, mastery);
    }

    @Test
    @DisplayName("Smoke Test: deve devolver o diagnóstico gerado pelo modelo")
    void deveDevolverODiagnosticoGerado() {
        when(chatLanguageModel.generate(anyList()))
                .thenReturn(Response.from(AiMessage.from("Concentre-se em antibioticoterapia nesta semana.")));

        Optional<String> diagnostico = service.diagnose(
                report(true, topic("Antibioticoterapia", 0.7, TopicMastery.FRAGIL)));

        assertThat(diagnostico).contains("Concentre-se em antibioticoterapia nesta semana.");
        verify(mmeebbMetrics).recordAiInteraction("performance_diagnosis", "gemini");
    }

    @Test
    @DisplayName("Deve enviar ao modelo os tópicos e percentuais reais do estudante")
    void deveEnviarOsTopicosReaisAoModelo() {
        when(chatLanguageModel.generate(anyList())).thenReturn(Response.from(AiMessage.from("ok")));

        service.diagnose(report(true,
                topic("Antibioticoterapia", 0.7, TopicMastery.FRAGIL),
                topic("Anti-hipertensivos", 0.1, TopicMastery.DOMINADO)));

        ArgumentCaptor<List<ChatMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(chatLanguageModel).generate(captor.capture());

        String prompt = captor.getValue().toString();
        assertThat(prompt).contains("Antibioticoterapia").contains("70%")
                .contains("Anti-hipertensivos").contains("FRAGIL");
    }

    @Test
    @DisplayName("Não deve gastar token quando ainda não há amostra suficiente")
    void naoDeveGastarTokenSemAmostraSuficiente() {
        Optional<String> diagnostico = service.diagnose(report(false));

        assertThat(diagnostico).isEmpty();
        verify(chatLanguageModel, never()).generate(anyList());
    }

    @Test
    @DisplayName("Falha do Gemini não pode derrubar o relatório: devolve vazio e registra o fallback")
    void falhaDoGeminiNaoPodeDerrubarORelatorio() {
        when(chatLanguageModel.generate(anyList())).thenThrow(new RuntimeException("503 high demand"));

        Optional<String> diagnostico = service.diagnose(
                report(true, topic("Antibioticoterapia", 0.7, TopicMastery.FRAGIL)));

        assertThat(diagnostico).isEmpty();
        verify(mmeebbMetrics).recordAiInteraction("performance_diagnosis", "fallback");
    }

    @Test
    @DisplayName("Resposta vazia do modelo deve ser tratada como ausência de diagnóstico")
    void respostaVaziaDeveSerTratadaComoAusencia() {
        when(chatLanguageModel.generate(anyList())).thenReturn(Response.from(AiMessage.from("   ")));

        assertThat(service.diagnose(report(true, topic("Antibioticoterapia", 0.7, TopicMastery.FRAGIL)))).isEmpty();
    }
}
