package br.edu.unipam.tcc.controller;

import br.edu.unipam.tcc.dto.RagSyncResponseDto;
import br.edu.unipam.tcc.service.KnowledgeIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;

@Slf4j
@RestController
@RequestMapping("/api/admin/rag")
@RequiredArgsConstructor
public class AdminRagController {

    private final KnowledgeIngestionService knowledgeIngestionService;

    @PostMapping("/sync-flashcards")
    public ResponseEntity<RagSyncResponseDto> syncFlashcards(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long subjectId
    ) {
        log.info("[AdminRagController] Requisição para sincronizar flashcards para o RAG (Curso: {}, Matéria: {})",
                courseId, subjectId);
        int totalIngested = knowledgeIngestionService.ingestFlashcardsAsKnowledge(courseId, subjectId);
        return ResponseEntity.ok(new RagSyncResponseDto(
                totalIngested,
                String.format("Sincronização concluída com sucesso! %d flashcard(s) indexado(s) no pgvector para o RAG.", totalIngested)
        ));
    }

    @PostMapping("/ingest-directory")
    public ResponseEntity<RagSyncResponseDto> ingestDirectory(
            @RequestParam String directoryPath,
            @RequestParam Long courseId,
            @RequestParam Long subjectId,
            @RequestParam String topic
    ) {
        log.info("[AdminRagController] Requisição para ingestão de pasta [{}] no RAG (Curso: {}, Matéria: {}, Tópico: \"{}\")",
                directoryPath, courseId, subjectId, topic);
        int totalIngested = knowledgeIngestionService.ingestSubjectDocuments(Path.of(directoryPath), courseId, subjectId, topic);
        return ResponseEntity.ok(new RagSyncResponseDto(
                totalIngested,
                String.format("Ingestão de diretório concluída com sucesso! %d segmento(s) indexado(s) no pgvector.", totalIngested)
        ));
    }
}
