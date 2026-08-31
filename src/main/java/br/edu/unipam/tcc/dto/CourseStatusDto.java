package br.edu.unipam.tcc.dto;

import jakarta.validation.constraints.NotNull;

public record CourseStatusDto(
        @NotNull(message = "O campo active é obrigatório.")
        Boolean active
) {}
