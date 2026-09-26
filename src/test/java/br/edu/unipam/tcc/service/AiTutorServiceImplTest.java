package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.dto.TutorContextDto;
import br.edu.unipam.tcc.observability.MmeebbMetrics;
import br.edu.unipam.tcc.service.impl.AiTutorServiceImpl;
import br.edu.unipam.tcc.session.ConversationHistoryStore;
import br.edu.unipam.tcc.session.ConversationTurn;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiTutorServiceImplTest {

    private static final String PHONE = "5534999998888";

    @Mock private ChatLanguageModel chatLanguageModel;
    @Mock private ConversationHistoryStore conversationHistoryStore;
    @Mock private MmeebbMetrics mmeebbMetrics;

    @InjectMocks private AiTutorServiceImpl tutor;

    @Captor private ArgumentCaptor<List<ChatMessage>> messagesCaptor;

    private TutorContextDto freeContext() {
        return new TutorContextDto(PHONE, "Maria", "Medicina", 5, "Cardiologia", null);
    }

    private TutorContextDto reviewContext() {
        return new TutorContextDto(PHONE, "Maria", "Medicina", 5, "Cardiologia",
                new TutorContextDto.ReviewQuestion(
                        "Arritmias",
                        "Qual a primeira conduta na FA instável?",
                        "A) Amiodarona B) Cardioversão elétrica sincronizada",
                        "Cardioversão elétrica sincronizada",
                        "A instabilidade hemodinâmica exige reversão imediata do ritmo."));
    }

    private void givenGeminiAnswers(String text) {
        when(chatLanguageModel.generate(anyList())).thenReturn(Response.from(AiMessage.from(text)));
    }

    private List<ChatMessage> sentMessages() {
        verify(chatLanguageModel).generate(messagesCaptor.capture());
        return messagesCaptor.getValue();
    }

    private String systemPrompt() {
        return ((SystemMessage) sentMessages().get(0)).text();
    }

    @Test
    @DisplayName("Smoke Test: deve devolver a resposta do Gemini e registrar a métrica do tutor")
    void deveDevolverARespostaDoGemini() {
        givenGeminiAnswers("  Sepse é uma disfunção orgânica causada por resposta desregulada à infecção.  ");

        String answer = tutor.answer(freeContext(), "o que é sepse?");

        assertThat(answer).isEqualTo("Sepse é uma disfunção orgânica causada por resposta desregulada à infecção.");
        verify(mmeebbMetrics).recordAiInteraction("tutor", "gemini");
    }

    @Test
    @DisplayName("Deve colocar nome, curso, período e disciplina do aluno no prompt de sistema")
    void deveColocarOPerfilDoAlunoNoPrompt() {
        givenGeminiAnswers("ok");

        tutor.answer(freeContext(), "o que é fibrilação atrial?");

        assertThat(systemPrompt())
                .contains("Tutor Acadêmico do Chatbot MMEEBB")
                .contains("- Nome: Maria")
                .contains("- Curso: Medicina (5º período)")
                .contains("- Disciplina em foco: Cardiologia");
    }

    @Test
    @DisplayName("Deve omitir do prompt os dados do aluno que não são conhecidos")
    void deveOmitirDadosDesconhecidos() {
        givenGeminiAnswers("ok");

        tutor.answer(new TutorContextDto(PHONE, "Maria", null, 5, null, null), "o que é sepse?");

        assertThat(systemPrompt())
                .contains("- Nome: Maria")
                .doesNotContain("Curso:")
                .doesNotContain("período")
                .doesNotContain("Disciplina em foco");
    }

    @Test
    @DisplayName("Deve enviar o histórico em ordem, alternando aluno e IA, antes da pergunta atual")
    void deveEnviarOHistoricoEmOrdem() {
        when(conversationHistoryStore.recent(PHONE)).thenReturn(List.of(
                new ConversationTurn("o que é FA?", "FA é uma arritmia supraventricular."),
                new ConversationTurn("quais os sintomas?", "Palpitações, dispneia e fadiga.")));
        givenGeminiAnswers("Controle de frequência, ritmo e anticoagulação.");

        tutor.answer(freeContext(), "e o tratamento?");

        List<ChatMessage> messages = sentMessages();
        assertThat(messages).hasSize(6);
        assertThat(messages.get(0)).isInstanceOf(SystemMessage.class);
        assertThat(((UserMessage) messages.get(1)).singleText()).isEqualTo("o que é FA?");
        assertThat(((AiMessage) messages.get(2)).text()).isEqualTo("FA é uma arritmia supraventricular.");
        assertThat(((UserMessage) messages.get(3)).singleText()).isEqualTo("quais os sintomas?");
        assertThat(((AiMessage) messages.get(4)).text()).isEqualTo("Palpitações, dispneia e fadiga.");
        assertThat(((UserMessage) messages.get(5)).singleText()).isEqualTo("e o tratamento?");
    }

    @Test
    @DisplayName("Deve ignorar trocas incompletas do histórico sem derrubar a dúvida")
    void deveIgnorarTrocasIncompletasDoHistorico() {
        when(conversationHistoryStore.recent(PHONE)).thenReturn(List.of(
                new ConversationTurn("pergunta sem resposta", null),
                new ConversationTurn(null, "resposta sem pergunta"),
                new ConversationTurn("o que é FA?", "FA é uma arritmia supraventricular.")));
        givenGeminiAnswers("ok");

        tutor.answer(freeContext(), "e o tratamento?");

        List<ChatMessage> messages = sentMessages();
        assertThat(messages).hasSize(4);
        assertThat(((UserMessage) messages.get(1)).singleText()).isEqualTo("o que é FA?");
    }

    @Test
    @DisplayName("Em revisão, deve enviar a questão com gabarito confidencial e a regra de não revelar")
    void deveEnviarAQuestaoComARegraDeNaoRevelar() {
        givenGeminiAnswers("Pense no que define instabilidade hemodinâmica.");

        tutor.answer(reviewContext(), "qual é a certa?");

        assertThat(systemPrompt())
                .contains("QUESTÃO QUE O ESTUDANTE ESTÁ REVISANDO AGORA")
                .contains("- Tópico: Arritmias")
                .contains("- Enunciado: Qual a primeira conduta na FA instável?")
                .contains("- Alternativas: A) Amiodarona B) Cardioversão elétrica sincronizada")
                .contains("- Gabarito (CONFIDENCIAL): Cardioversão elétrica sincronizada")
                .contains("- Explicação (CONFIDENCIAL): A instabilidade hemodinâmica exige reversão imediata do ritmo.")
                .contains("NÃO revele qual alternativa é a correta");
    }

    @Test
    @DisplayName("Fora da revisão, o prompt não deve ter bloco de questão nem regra de revisão")
    void naoDeveTerBlocoDeRevisaoForaDaRevisao() {
        givenGeminiAnswers("ok");

        tutor.answer(freeContext(), "o que é sepse?");

        assertThat(systemPrompt())
                .doesNotContain("CONFIDENCIAL")
                .doesNotContain("REGRA DA REVISÃO");
    }

    @Test
    @DisplayName("Deve gravar no histórico só a pergunta e a resposta, nunca o gabarito")
    void deveGravarSoPerguntaERespostaNoHistorico() {
        givenGeminiAnswers("Pense no que define instabilidade hemodinâmica.");

        tutor.answer(reviewContext(), "  qual é a certa?  ");

        verify(conversationHistoryStore).append(PHONE, "qual é a certa?",
                "Pense no que define instabilidade hemodinâmica.");
    }

    @Test
    @DisplayName("Falha do Gemini: devolve instabilidade, não grava histórico e registra o fallback")
    void deveResponderComInstabilidadeQuandoOGeminiFalhar() {
        when(chatLanguageModel.generate(anyList())).thenThrow(new RuntimeException("503 high demand"));

        String answer = tutor.answer(freeContext(), "o que é sepse?");

        assertThat(answer).contains("instabilidade momentânea");
        verify(conversationHistoryStore, never()).append(anyString(), anyString(), anyString());
        verify(mmeebbMetrics).recordAiInteraction("tutor", "fallback");
    }

    @Test
    @DisplayName("Resposta em branco do Gemini é tratada como falha")
    void deveTratarRespostaEmBrancoComoFalha() {
        givenGeminiAnswers("   ");

        String answer = tutor.answer(freeContext(), "o que é sepse?");

        assertThat(answer).contains("instabilidade momentânea");
        verify(conversationHistoryStore, never()).append(anyString(), anyString(), anyString());
        verify(mmeebbMetrics).recordAiInteraction("tutor", "fallback");
    }

    @Test
    @DisplayName("Falha ao ler o histórico não pode estourar no consumidor: devolve instabilidade")
    void deveResponderComInstabilidadeQuandoOHistoricoFalhar() {
        when(conversationHistoryStore.recent(PHONE)).thenThrow(new IllegalStateException("Redis fora do ar"));

        String answer = tutor.answer(freeContext(), "o que é sepse?");

        assertThat(answer).contains("instabilidade momentânea");
        verify(mmeebbMetrics).recordAiInteraction("tutor", "fallback");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    @DisplayName("Deve rejeitar dúvida vazia sem chamar a IA")
    void deveRejeitarDuvidaVazia(String question) {
        assertThatThrownBy(() -> tutor.answer(freeContext(), question))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(chatLanguageModel);
    }

    @Test
    @DisplayName("Deve esquecer a conversa do aluno")
    void deveEsquecerAConversa() {
        tutor.clearConversation(PHONE);

        verify(conversationHistoryStore).clear(PHONE);
    }
}
