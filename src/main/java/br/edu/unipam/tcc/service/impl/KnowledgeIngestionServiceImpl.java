package br.edu.unipam.tcc.service.impl;

import br.edu.unipam.tcc.service.KnowledgeIngestionService;
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
    private final DocumentParser documentParser;
    private final DocumentSplitter documentSplitter;

    public KnowledgeIngestionServiceImpl(
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore
    ) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
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
