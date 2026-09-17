package br.edu.unipam.tcc.dto;

import java.util.List;

public record WebhookSyncResponseDto(
        boolean success,
        String configuredUrl,
        List<String> events,
        String message
) {
}
