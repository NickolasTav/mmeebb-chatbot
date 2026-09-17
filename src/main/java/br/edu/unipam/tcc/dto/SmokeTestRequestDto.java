package br.edu.unipam.tcc.dto;

import jakarta.validation.constraints.NotBlank;

public record SmokeTestRequestDto(
        @NotBlank(message = "O telefone de destino é obrigatório.")
        String targetPhone,

        @NotBlank(message = "A mensagem de teste é obrigatória.")
        String message
) {
}
