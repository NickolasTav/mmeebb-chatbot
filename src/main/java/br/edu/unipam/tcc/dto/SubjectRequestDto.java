package br.edu.unipam.tcc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubjectRequestDto(
        @NotNull(message = "O ID do curso é obrigatório.")
        Long courseId,

        @NotBlank(message = "O código da matéria/disciplina é obrigatório.")
        @Size(max = 50, message = "O código deve ter no máximo 50 caracteres.")
        String code,

        @NotBlank(message = "O nome da matéria/disciplina é obrigatório.")
        @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
        String name,

        String description,

        Boolean active
) {}
