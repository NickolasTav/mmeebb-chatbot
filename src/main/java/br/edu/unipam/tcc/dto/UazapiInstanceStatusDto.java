package br.edu.unipam.tcc.dto;

public record UazapiInstanceStatusDto(
        String instance,
        String state,
        boolean connected,
        boolean webhookAutoConfigured,
        String message
) {
}
