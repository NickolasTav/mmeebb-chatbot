package br.edu.unipam.tcc.dto;

public record UazapiConnectRequestDto(
        String baseUrl,
        String instance,
        String apiKey
) {
}
