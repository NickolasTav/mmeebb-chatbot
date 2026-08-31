package br.edu.unipam.tcc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CourseRequestDto(
        @NotBlank(message = "O código do curso é obrigatório.")
        @Size(max = 50, message = "O código do curso deve ter no máximo 50 caracteres.")
        String code,

        @NotBlank(message = "O nome do curso é obrigatório.")
        @Size(max = 150, message = "O nome do curso deve ter no máximo 150 caracteres.")
        String name,

        String description,

        Boolean active
) {}
