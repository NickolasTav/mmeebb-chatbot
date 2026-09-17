package br.edu.unipam.tcc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload de ingestão de conteúdo no banco vetorial.
 * Curso, disciplina e tópico viram metadados do embedding, permitindo que a busca
 * semântica seja particionada por matéria sem varrer o acervo inteiro.
 *
 * @param courseId  Curso ao qual o conteúdo pertence.
 * @param subjectId Disciplina/matéria do conteúdo.
 * @param topic     Assunto específico dentro da disciplina (ex.: "Arritmias").
 * @param title     Título opcional do material, prefixado ao texto indexado.
 * @param content   Texto a ser segmentado e vetorizado.
 */
public record KnowledgeIngestRequestDto(
        @NotNull(message = "O ID do curso é obrigatório.")
        Long courseId,

        @NotNull(message = "O ID da matéria/disciplina é obrigatório.")
        Long subjectId,

        @NotBlank(message = "O tópico é obrigatório.")
        @Size(max = 150, message = "O tópico deve ter no máximo 150 caracteres.")
        String topic,

        @Size(max = 200, message = "O título deve ter no máximo 200 caracteres.")
        String title,

        @NotBlank(message = "O conteúdo a ser indexado é obrigatório.")
        String content
) {}
