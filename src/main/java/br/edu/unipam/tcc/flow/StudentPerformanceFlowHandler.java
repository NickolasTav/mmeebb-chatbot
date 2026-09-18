package br.edu.unipam.tcc.flow;

import br.edu.unipam.tcc.dto.StudentPerformanceReportDto;
import br.edu.unipam.tcc.dto.TopicPerformanceDto;
import br.edu.unipam.tcc.entity.Student;
import br.edu.unipam.tcc.entity.enums.ChatState;
import br.edu.unipam.tcc.messaging.OutgoingMessagePublisher;
import br.edu.unipam.tcc.service.PerformanceAnalysisService;
import br.edu.unipam.tcc.service.PerformanceDiagnosisService;
import br.edu.unipam.tcc.session.ChatSessionState;
import br.edu.unipam.tcc.session.ChatSessionStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.StringJoiner;

/**
 * Monta a mensagem "Meu desempenho" no WhatsApp.
 *
 * <p>O relatório é enviado de uma vez e devolve o aluno ao menu principal — não existe estado
 * {@code PERFORMANCE_*} na FSM. Além de evitar um estado em que a conversa possa ficar presa,
 * isso mantém a sessão do Redis compatível com as versões anteriores da máquina de estados.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StudentPerformanceFlowHandler {

    private static final int MAX_TOPICS_PER_GROUP = 3;

    private final PerformanceAnalysisService performanceAnalysisService;
    private final PerformanceDiagnosisService performanceDiagnosisService;
    private final OutgoingMessagePublisher outgoingMessagePublisher;
    private final ChatSessionStore chatSessionStore;

    public void send(ChatSessionState session, Student student) {
        StudentPerformanceReportDto report = performanceAnalysisService.buildStudentReport(student);

        session.clearReviewContext();
        session.setCurrentState(ChatState.MAIN_MENU);
        chatSessionStore.save(session);

        String message = report.enoughData() ? fullReport(report) : invitation(report);
        outgoingMessagePublisher.publish(session.getPhoneNumber(), message);

        log.info("[Performance] Relatório enviado para [{}]: {} tentativa(s), {} tópico(s) analisado(s)",
                session.getPhoneNumber(), report.totalAttempts(), report.topics().size());
    }

    /**
     * Sem amostra, percentuais não significam nada — exibi-los daria uma falsa sensação de
     * diagnóstico. O aluno recebe o convite a revisar e a explicação do que o sistema fará com isso.
     */
    private String invitation(StudentPerformanceReportDto report) {
        return String.format("""
                📊 *Seu desempenho, %s*

                Ainda não tenho respostas suficientes para traçar o seu perfil de estudo.

                Envie *1* para revisar agora: conforme você responde, eu identifico os conteúdos em que \
                você mais erra e encurto os intervalos justamente neles.""", report.studentName());
    }

    private String fullReport(StudentPerformanceReportDto report) {
        StringJoiner message = new StringJoiner("\n\n");

        message.add(String.format("📊 *Seu desempenho, %s* — últimos %d dias",
                report.studentName(), report.windowDays()));
        message.add(String.format("Você respondeu *%d* questão(ões) e acertou *%d* (*%d%%*).",
                report.totalAttempts(), report.correctAttempts(), report.accuracyPercentage()));

        List<TopicPerformanceDto> weakest = report.weakestTopics(MAX_TOPICS_PER_GROUP);
        if (!weakest.isEmpty()) {
            StringBuilder block = new StringBuilder("🔴 *Onde você mais erra* _(revisão encurtada)_\n");
            weakest.forEach(topic -> block.append(String.format("• _%s_ — %d%% de erro (reforço a cada %d dias no máximo)\n",
                    topic.topic(), topic.errorPercentage(), topic.maxIntervalDays())));
            message.add(block.toString().trim());
        }

        List<TopicPerformanceDto> mastered = report.masteredTopics(MAX_TOPICS_PER_GROUP);
        if (!mastered.isEmpty()) {
            StringBuilder block = new StringBuilder("🟢 *Conteúdos consolidados*\n");
            mastered.forEach(topic -> block.append(String.format("• _%s_ — %d%% de erro\n",
                    topic.topic(), topic.errorPercentage())));
            message.add(block.toString().trim());
        }

        performanceDiagnosisService.diagnose(report)
                .ifPresent(diagnosis -> message.add("🤖 *Leitura do preceptor virtual*\n" + diagnosis));

        message.add("_Envie *1* para revisar agora ou *menu* para voltar._");
        return message.toString();
    }
}
