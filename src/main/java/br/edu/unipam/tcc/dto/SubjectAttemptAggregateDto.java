package br.edu.unipam.tcc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Desempenho agregado de uma disciplina somando todos os estudantes, usado na visão
 * administrativa que embasa a seção de resultados da monografia.
 */
public record SubjectAttemptAggregateDto(
        Long subjectId,
        String subjectName,
        long attempts,
        long correctAttempts
) {

    @JsonProperty("accuracy")
    public double accuracy() {
        return attempts == 0 ? 0.0 : (double) correctAttempts / attempts;
    }
}
