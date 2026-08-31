package br.edu.unipam.tcc.dto;

import jakarta.validation.constraints.NotNull;

public record SeedScheduleRequestDto(
        @NotNull(message = "O ID da matéria/disciplina é obrigatório.")
        Long subjectId
) {}
