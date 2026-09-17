package br.edu.unipam.tcc.controller;

import br.edu.unipam.tcc.dto.*;
import br.edu.unipam.tcc.entity.SystemConfiguration;
import br.edu.unipam.tcc.repository.SystemConfigurationRepository;
import br.edu.unipam.tcc.service.NgrokTunnelService;
import br.edu.unipam.tcc.service.UazapiInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Expõe a API REST consumida pelo painel web de conectividade ({@code /setup}), orquestrando
 * {@link NgrokTunnelService} e {@link UazapiInstanceService} para status consolidado, QR Code,
 * sincronização automática de webhook e teste de fumaça.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/setup")
public class UazapiSetupController {

    private static final String WEBHOOK_PATH = "/webhook/uazapi";
    private static final List<String> WEBHOOK_EVENTS = List.of("messages.upsert", "messages");

    private final NgrokTunnelService ngrokTunnelService;
    private final UazapiInstanceService uazapiInstanceService;
    private final SystemConfigurationRepository configRepository;

    @GetMapping("/status")
    public UazapiSetupStatusDto getStatus() {
        NgrokTunnelDto ngrok = ngrokTunnelService.getTunnelStatus();
        UazapiSetupStatusDto.UazapiStatus uazapi = uazapiInstanceService.getInstanceStatus();
        UazapiSetupStatusDto.WebhookStatus webhook = buildWebhookStatus(ngrok);
        return new UazapiSetupStatusDto(ngrok, uazapi, webhook);
    }

    @PostMapping("/tunnel/discover")
    public NgrokTunnelDto discoverTunnel() {
        return ngrokTunnelService.discoverTunnel();
    }

    @PostMapping("/instance/connect")
    public UazapiQrCodeDto connectInstance(@RequestBody(required = false) UazapiConnectRequestDto request) {
        return uazapiInstanceService.requestQrCode(request);
    }

    @PostMapping("/instance/provision")
    public UazapiProvisionResponseDto provisionInstance() {
        return uazapiInstanceService.provisionInstance();
    }

    @GetMapping("/instance/status")
    public UazapiInstanceStatusDto getInstanceStatus() {
        UazapiSetupStatusDto.UazapiStatus status = uazapiInstanceService.getInstanceStatus();

        if (!status.connected()) {
            return new UazapiInstanceStatusDto(status.instance(), status.connectionState(), false, false,
                    "Aguardando pareamento do QR Code.");
        }

        boolean webhookAutoConfigured = autoConfigureWebhookIfNeeded();
        return new UazapiInstanceStatusDto(status.instance(), status.connectionState(), true, webhookAutoConfigured,
                "Instância conectada e pronta para mensagens.");
    }

    @PostMapping("/webhook/sync")
    public WebhookSyncResponseDto syncWebhook(@RequestBody(required = false) WebhookSyncRequestDto request) {
        String webhookUrl = resolveWebhookUrl(request != null ? request.webhookUrl() : null);

        if (webhookUrl == null) {
            return new WebhookSyncResponseDto(false, null, WEBHOOK_EVENTS,
                    "Não foi possível determinar a URL do webhook: informe uma URL ou ative o túnel Ngrok.");
        }

        boolean success = uazapiInstanceService.configureWebhook(webhookUrl);
        String message = success
                ? "Webhook configurado com sucesso na Uazapi!"
                : "Falha ao configurar o webhook na Uazapi. Verifique a instância e a api-key.";

        return new WebhookSyncResponseDto(success, webhookUrl, WEBHOOK_EVENTS, message);
    }

    @PostMapping("/test-message")
    public SmokeTestResponseDto sendTestMessage(@RequestBody SmokeTestRequestDto request) {
        uazapiInstanceService.sendSmokeTestMessage(request.targetPhone(), request.message());
        return new SmokeTestResponseDto(true, request.targetPhone(), "Mensagem de teste enviada com sucesso.");
    }

    private UazapiSetupStatusDto.WebhookStatus buildWebhookStatus(NgrokTunnelDto ngrok) {
        String expectedUrl = ngrok.online() ? ngrok.publicUrl() + WEBHOOK_PATH : null;
        return configRepository.findByConfigKey("webhook.last-synced-url")
                .map(config -> new UazapiSetupStatusDto.WebhookStatus(
                        config.getConfigValue(),
                        config.getConfigValue().equals(expectedUrl),
                        config.getUpdatedAt()))
                .orElseGet(() -> new UazapiSetupStatusDto.WebhookStatus(expectedUrl, false, null));
    }

    private boolean autoConfigureWebhookIfNeeded() {
        NgrokTunnelDto ngrok = ngrokTunnelService.getTunnelStatus();
        if (!ngrok.online()) {
            return false;
        }

        String webhookUrl = ngrok.publicUrl() + WEBHOOK_PATH;
        if (isWebhookAlreadySynced(webhookUrl)) {
            return true;
        }

        return uazapiInstanceService.configureWebhook(webhookUrl);
    }

    private boolean isWebhookAlreadySynced(String expectedWebhookUrl) {
        return configRepository.findByConfigKey("webhook.last-synced-url")
                .map(SystemConfiguration::getConfigValue)
                .map(expectedWebhookUrl::equals)
                .orElse(false);
    }

    private String resolveWebhookUrl(String explicitWebhookUrl) {
        if (explicitWebhookUrl != null && !explicitWebhookUrl.isBlank()) {
            return explicitWebhookUrl;
        }

        NgrokTunnelDto ngrok = ngrokTunnelService.getTunnelStatus();
        return ngrok.online() ? ngrok.publicUrl() + WEBHOOK_PATH : null;
    }
}
