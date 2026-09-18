package br.edu.unipam.tcc.service.impl;

import br.edu.unipam.tcc.dto.StudentPerformanceReportDto;
import br.edu.unipam.tcc.dto.TopicPerformanceDto;
import br.edu.unipam.tcc.observability.MmeebbMetrics;
import br.edu.unipam.tcc.service.PerformanceDiagnosisService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceDiagnosisServiceImpl implements PerformanceDiagnosisService {

    private static final int MAX_TOPICS_IN_PROMPT = 8;

    private static final String SYSTEM_PROMPT = """
            Você é o preceptor acadêmico do Chatbot MMEEBB do UNIPAM, comentando o desempenho de um
            estudante em suas revisões espaçadas.

            DIRETRIZES OBRIGATÓRIAS:
            1. Use SOMENTE os tópicos e percentuais apresentados. Nunca invente conteúdo, diagnóstico
               clínico ou dado que não esteja na lista.
            2. Escreva no máximo 3 frases curtas, em português do Brasil, falando diretamente com o
               estudante.
            3. Diga onde concentrar o esforço nos próximos dias e reconheça o que já está consolidado.
            4. Use a formatação do WhatsApp: *negrito* para o que importa e _itálico_ para os tópicos.
            5. Nada de saudação, despedida, listas ou títulos: apenas o comentário corrido.""";

    private final ChatLanguageModel chatLanguageModel;
    private final MmeebbMetrics mmeebbMetrics;

    @Override
    public Optional<String> diagnose(StudentPerformanceReportDto report) {
        if (report == null || !report.enoughData() || report.topics().isEmpty()) {
            return Optional.empty();
        }

        try {
            Response<AiMessage> response = chatLanguageModel.generate(List.of(
                    SystemMessage.from(SYSTEM_PROMPT),
                    UserMessage.from(buildPrompt(report))
            ));

            String text = response != null && response.content() != null ? response.content().text() : "";
            if (text == null || text.isBlank()) {
                return Optional.empty();
            }

            mmeebbMetrics.recordAiInteraction("performance_diagnosis", "gemini");
            return Optional.of(text.trim());

        } catch (Exception e) {
            mmeebbMetrics.recordAiInteraction("performance_diagnosis", "fallback");
            log.error("[PerformanceDiagnosis] Falha ao gerar o diagnóstico; o relatório segue sem ele: {}",
                    e.getMessage());
            return Optional.empty();
        }
    }

    private String buildPrompt(StudentPerformanceReportDto report) {
        String topics = report.topics().stream()
                .limit(MAX_TOPICS_IN_PROMPT)
                .map(this::describeTopic)
                .collect(Collectors.joining("\n"));

        return String.format("""
                ESTUDANTE: %s
                JANELA ANALISADA: últimos %d dias
                RESPOSTAS: %d, com %d acertos (%d%% de acerto geral)

                DESEMPENHO POR TÓPICO (do mais frágil ao mais dominado):
                %s""",
                report.studentName(), report.windowDays(), report.totalAttempts(),
                report.correctAttempts(), report.accuracyPercentage(), topics);
    }

    private String describeTopic(TopicPerformanceDto topic) {
        return String.format("- %s (%s): %d%% de erro em %d resposta(s) — classificação %s",
                topic.topic(), topic.subjectName(), topic.errorPercentage(),
                topic.attempts(), topic.mastery().name());
    }
}
