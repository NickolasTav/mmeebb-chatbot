package br.edu.unipam.tcc.service.impl;

import br.edu.unipam.tcc.service.SubjectRagService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementação do serviço de Retrieval-Augmented Generation (RAG) dinâmico por disciplina.
 * Realiza a busca vetorial estritamente particionada no pgvector por subject_id e submete
 * os contextos ao Google Gemini com diretrizes explícitas anti-alucinação.
 */
@Slf4j
@Service
public class SubjectRagServiceImpl implements SubjectRagService {

    private static final int MAX_RESULTS = 4;
    private static final double MIN_SIMILARITY_SCORE = 0.55;

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            Você é o Tutor Acadêmico e Preceptor Inteligente do UNIPAM no Chatbot MMEEBB.
            Sua função é esclarecer as dúvidas dos estudantes com rigor conceitual, clareza pedagógica e objetividade.

            DIRETRIZES OBRIGATÓRIAS (ANTI-ALUCINAÇÃO):
            1. Responda à dúvida baseando-se EXCLUSIVAMENTE nas informações contidas na seção CONTEXTO RECUPERADO.
            2. Se os trechos não contiverem a resposta com precisão, declare honestamente que o material oficial da disciplina não aborda esse detalhe específico. NUNCA invente fatos ou condutas não descritas.
            3. Utilize a formatação nativa do WhatsApp:
               - Negrito com asteriscos (*termo*)
               - Itálico com underscores (_detalhe_)
               - Tópicos ou listas numeradas (* ou 1., 2.)
            4. Mantenha a resposta concisa e direta, facilitando a leitura rápida no celular do interno/estudante.""";

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final ChatLanguageModel chatLanguageModel;

    public SubjectRagServiceImpl(
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore,
            ChatLanguageModel chatLanguageModel
    ) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.chatLanguageModel = chatLanguageModel;
    }

    @Override
    public String answerDoubt(String userQuestion, Long subjectId) {
        validateInputs(userQuestion);

        String trimmedQuestion = userQuestion.trim();
        log.info("[SubjectRag] Processando dúvida no RAG (Disciplina ID: {}): \"{}\"",
                subjectId != null ? subjectId : "TODAS (Global)", trimmedQuestion);

        try {
            // 1. Gera o vetor de embedding para a pergunta do estudante
            Embedding queryEmbedding = embeddingModel.embed(trimmedQuestion).content();

            // 2. Constrói a busca vetorial (global ou filtrada por disciplina caso informada)
            EmbeddingSearchRequest.EmbeddingSearchRequestBuilder requestBuilder = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(MAX_RESULTS)
                    .minScore(MIN_SIMILARITY_SCORE);

            if (subjectId != null) {
                requestBuilder.filter(new IsEqualTo("subject_id", subjectId.toString()));
            }

            EmbeddingSearchRequest searchRequest = requestBuilder.build();

            // 3. Executa a busca no pgvector
            EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
            List<EmbeddingMatch<TextSegment>> matches = searchResult != null ? searchResult.matches() : List.of();

            if (matches == null || matches.isEmpty()) {
                log.info("[SubjectRag] Nenhum trecho com relevância mínima encontrado no pgvector para a dúvida.");
                return """
                        ⚠️ *Não encontrei referências sobre este tema no acervo de conhecimento cadastrado.*

                        _Tente reformular sua pergunta com outros termos ou consulte o preceptor/professor responsável._""";
            }

            log.info("[SubjectRag] {} trecho(s) relevante(s) recuperado(s) para a dúvida (Score máx: {})",
                    matches.size(), matches.get(0).score());

            // 4. Concatena os trechos recuperados para compor o contexto
            StringBuilder contextBuilder = new StringBuilder();
            for (int i = 0; i < matches.size(); i++) {
                EmbeddingMatch<TextSegment> match = matches.get(i);
                contextBuilder.append("--- Trecho ").append(i + 1).append(" ---\n")
                        .append(match.embedded().text())
                        .append("\n\n");
            }

            // 5. Constrói as mensagens de diálogo para o modelo de linguagem (Gemini)
            SystemMessage systemMessage = SystemMessage.from(SYSTEM_PROMPT_TEMPLATE);
            String userPromptContent = String.format("""
                    CONTEXTO RECUPERADO DA BASE DE CONHECIMENTO:
                    %s

                    DÚVIDA DO ESTUDANTE:
                    %s
                    """, contextBuilder, trimmedQuestion);

            UserMessage userMessage = UserMessage.from(userPromptContent);

            log.info("[SubjectRag] Submetendo prompt ao Google Gemini...");
            Response<AiMessage> aiResponse = chatLanguageModel.generate(List.of(systemMessage, userMessage));

            String responseText = aiResponse != null && aiResponse.content() != null
                    ? aiResponse.content().text()
                    : "";

            log.info("[SubjectRag] Resposta gerada com sucesso pelo Gemini ({} caracteres).", responseText.length());
            return responseText;

        } catch (Exception e) {
            log.error("[SubjectRag] Erro inesperado durante processamento RAG: {}", e.getMessage(), e);

            return """
                    ⚠️ *Desculpe, ocorreu uma instabilidade momentânea ao consultar a base de conhecimento de IA.*

                    _Por favor, envie sua dúvida novamente em instantes ou digite *menu* para voltar ao menu principal._""";
        }
    }

    private void validateInputs(String userQuestion) {
        if (userQuestion == null || userQuestion.isBlank()) {
            throw new IllegalArgumentException("A dúvida do usuário não pode ser nula ou vazia.");
        }
    }
}
