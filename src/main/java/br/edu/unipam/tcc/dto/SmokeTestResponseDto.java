package br.edu.unipam.tcc.dto;

public record SmokeTestResponseDto(
        boolean success,
        String targetPhone,
        String message
) {
}
