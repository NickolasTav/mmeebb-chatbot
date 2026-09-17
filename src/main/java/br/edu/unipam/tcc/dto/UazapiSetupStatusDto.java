package br.edu.unipam.tcc.dto;

import java.time.LocalDateTime;

public record UazapiSetupStatusDto(
        NgrokTunnelDto ngrok,
        UazapiStatus uazapi,
        WebhookStatus webhook
) {

    public record UazapiStatus(
            String baseUrl,
            String instance,
            boolean hasApiKey,
            String maskedApiKey,
            String connectionState,
            boolean connected,
            String profileName,
            String profilePictureUrl
    ) {
    }

    public record WebhookStatus(
            String targetUrl,
            boolean isConfigured,
            LocalDateTime lastSyncedAt
    ) {
    }
}
