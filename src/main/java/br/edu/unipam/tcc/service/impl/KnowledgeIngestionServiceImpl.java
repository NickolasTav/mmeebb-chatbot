package br.edu.unipam.tcc.service.impl;

import br.edu.unipam.tcc.service.KnowledgeIngestionService;
import br.edu.unipam.tcc.entity.Flashcard;
import br.edu.unipam.tcc.repository.FlashcardRepository;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementação do serviço de ingestão de conhecimento educacional.
 * Realiza o parsing de documentos com Apache Tika, aplica divisão recursiva (300 tokens com overlap de 30),
 * enriquece cada trecho com metadados relacionais (course_id, subject_id, topic)
 * e persiste os embeddings vetoriais na tabela tb_knowledge_embedding do pgvector.
 */
@Slf4j
@Service
public class KnowledgeIngestionServiceImpl implements KnowledgeIngestionService {

    private static final int CHUNK_MAX_TOKENS = 300;
    private static final int CHUNK_OVERLAP_TOKENS = 30;

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final FlashcardRepository flashcardRepository;
    private final DocumentParser documentParser;
    private final DocumentSplitter documentSplitter;

    public KnowledgeIngestionServiceImpl(
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore,
            FlashcardRepository flashcardRepository
    ) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.flashcardRepository = flashcardRepository;
        this.documentParser = new ApacheTikaDocumentParser();
        this.documentSplitter = DocumentSplitters.recursive(CHUNK_MAX_TOKENS, CHUNK_OVERLAP_TOKENS);
    }

    @Override
    public int ingestSubjectDocuments(Path directoryPath, Long courseId, Long subjectId, String topic) {
        validateInputs(directoryPath, courseId, subjectId, topic);

        log.info("[KnowledgeIngestion] Iniciando ingestão no diretório [{}] para Curso ID: {}, Disciplina ID: {}, Tópico: \"{}\"",
                directoryPath, courseId, subjectId, topic);

        // 1. Carrega todos os documentos do diretório via Apache Tika
        List<Document> documents = FileSystemDocumentLoader.loadDocuments(directoryPath, documentParser);
        if (documents == null || documents.isEmpty()) {
            log.warn("[KnowledgeIngestion] Nenhum documento encontrado em [{}]", directoryPath);
            return 0;
        }

        log.info("[KnowledgeIngestion] {} documento(s) carregado(s) com sucesso. Iniciando segmentação e injeção de metadados...",
                documents.size());

        // 2. Segmenta e injeta metadados em cada trecho
        List<TextSegment> allSegments = new ArrayList<>();
        for (Document document : documents) {
            List<TextSegment> segments = documentSplitter.split(document);
            String sourceName = document.metadata() != null ? document.metadata().getString("file_name") : "documento";

            for (TextSegment segment : segments) {
                segment.metadata().put("course_id", courseId.toString());
                segment.metadata().put("subject_id", subjectId.toString());
                segment.metadata().put("topic", topic);
                if (sourceName != null) {
                    segment.metadata().put("source", sourceName);
                }
                allSegments.add(segment);
            }
        }

        if (allSegments.isEmpty()) {
            log.warn("[KnowledgeIngestion] Nenhum segmento de texto gerado a partir dos documentos em [{}]", directoryPath);
            return 0;
        }

        log.info("[KnowledgeIngestion] Gerando embeddings para {} segmentos via EmbeddingModel...", allSegments.size());

        // 3. Gera os embeddings vetoriais
        Response<List<Embedding>> embeddingResponse = embeddingModel.embedAll(allSegments);
        List<Embedding> embeddings = embeddingResponse.content();

        // 4. Salva no pgvector
        log.info("[KnowledgeIngestion] Armazenando {} vetores no pgvector (tb_knowledge_embedding)...", embeddings.size());
        embeddingStore.addAll(embeddings, allSegments);

        log.info("[KnowledgeIngestion] Ingestão concluída com sucesso! Total de segmentos indexados: {}", allSegments.size());
        return allSegments.size();
    }

    @Override
    public int ingestFlashcardsAsKnowledge(Long courseId, Long subjectId) {
        log.info("[KnowledgeIngestion] Sincronizando flashcards para o RAG. Filtros -> Curso ID: {}, Disciplina ID: {}",
                courseId, subjectId);

        List<Flashcard> flashcards;
        if (subjectId != null) {
            flashcards = flashcardRepository.findBySubjectIdAndActiveTrue(subjectId);
        } else if (courseId != null) {
            flashcards = flashcardRepository.findByActive(true).stream()
                    .filter(f -> f.getSubject() != null && f.getSubject().getCourse() != null &&
                            courseId.equals(f.getSubject().getCourse().getId()))
                    .toList();
        } else {
            flashcards = flashcardRepository.findByActive(true);
        }

        if (flashcards.isEmpty()) {
            log.warn("[KnowledgeIngestion] Nenhum flashcard ativo encontrado para os filtros fornecidos.");
            return 0;
        }

        log.info("[KnowledgeIngestion] {} flashcard(s) ativo(s) recuperado(s). Construindo segmentos de conhecimento...", flashcards.size());

        List<TextSegment> segments = new ArrayList<>();
        for (Flashcard card : flashcards) {
            String topic = card.getTopic() != null ? card.getTopic() : "Geral";
            String subjectName = card.getSubject() != null ? card.getSubject().getName() : "Geral";
            String cId = (card.getSubject() != null && card.getSubject().getCourse() != null)
                    ? card.getSubject().getCourse().getId().toString()
                    : (courseId != null ? courseId.toString() : "1");
            String sId = card.getSubject() != null ? card.getSubject().getId().toString()
                    : (subjectId != null ? subjectId.toString() : "1");

            StringBuilder sb = new StringBuilder();
            sb.append("Tópico: ").append(topic).append("\n");
            sb.append("Disciplina: ").append(subjectName).append("\n");
            sb.append("Questão / Conceito Clínico: ").append(card.getQuestion()).append("\n");
            sb.append("Gabarito / Resposta Correta: ").append(card.getAnswer()).append("\n");

            if (card.getOptionsJson() != null && !card.getOptionsJson().isBlank()) {
                sb.append("Opções:\n").append(card.getOptionsJson()).append("\n");
            }

            if (card.getExplanation() != null && !card.getExplanation().isBlank()) {
                sb.append("Fundamentação Teórica e Justificativa Clínica:\n").append(card.getExplanation()).append("\n");
            }

            if (card.getSource() != null && !card.getSource().isBlank()) {
                sb.append("Fonte / Referência Oficial: ").append(card.getSource()).append("\n");
            }

            TextSegment segment = TextSegment.from(sb.toString().trim());
            segment.metadata().put("course_id", cId);
            segment.metadata().put("subject_id", sId);
            segment.metadata().put("topic", topic);
            segment.metadata().put("source", "flashcard_" + card.getId());

            segments.add(segment);
        }

        log.info("[KnowledgeIngestion] Gerando embeddings vetoriais para {} segmentos de flashcards...", segments.size());
        Response<List<Embedding>> embeddingResponse = embeddingModel.embedAll(segments);
        List<Embedding> embeddings = embeddingResponse.content();

        log.info("[KnowledgeIngestion] Persistindo {} vetores no pgvector (tb_knowledge_embedding)...", embeddings.size());
        embeddingStore.addAll(embeddings, segments);

        log.info("[KnowledgeIngestion] Sincronização concluída com sucesso! {} flashcards indexados no RAG.", segments.size());
        return segments.size();
    }

    private void validateInputs(Path directoryPath, Long courseId, Long subjectId, String topic) {
        if (directoryPath == null) {
            throw new IllegalArgumentException("O caminho do diretório de documentos não pode ser nulo.");
        }
        if (!Files.exists(directoryPath) || !Files.isDirectory(directoryPath)) {
            throw new IllegalArgumentException("Diretório de documentos não existe ou não é uma pasta válida: " + directoryPath);
        }
        if (courseId == null) {
            throw new IllegalArgumentException("O ID do curso (courseId) é obrigatório.");
        }
        if (subjectId == null) {
            throw new IllegalArgumentException("O ID da disciplina (subjectId) é obrigatório.");
        }
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("O tópico do conteúdo é obrigatório.");
        }
    }
}
