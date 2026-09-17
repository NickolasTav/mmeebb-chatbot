package br.edu.unipam.tcc.dto;

public record UazapiProvisionResponseDto(
        boolean success,
        String instance,
        String maskedApiKey,
        String message
) {
}
