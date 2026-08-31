package br.edu.unipam.tcc.service;

import java.nio.file.Path;

/**
 * Serviço responsável pela ingestão de documentos educacionais multidisciplinares
 * na base de conhecimento vetorial (pgvector) particionada por curso e disciplina.
 */
public interface KnowledgeIngestionService {

    /**
     * Carrega, faz o parsing com Apache Tika, divide e armazena documentos de um diretório no pgvector.
     * Injeta metadados relacionais (course_id, subject_id, topic) em cada segmento de texto.
     *
     * @param directoryPath Caminho da pasta contendo os documentos (PDF, MD, DOCX, TXT).
     * @param courseId      Identificador do Curso correspondente.
     * @param subjectId     Identificador da Matéria / Disciplina correspondente.
     * @param topic         Descrição contextual do tópico (ex: "Cardiologia - Arritmias").
     * @return Quantidade total de segmentos de texto ingeridos com sucesso.
     */
    int ingestSubjectDocuments(Path directoryPath, Long courseId, Long subjectId, String topic);
}
