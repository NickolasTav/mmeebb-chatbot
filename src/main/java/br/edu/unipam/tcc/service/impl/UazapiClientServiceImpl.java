package br.edu.unipam.tcc.service.impl;

import br.edu.unipam.tcc.dto.SendMessageRequestDto;
import br.edu.unipam.tcc.dto.SendPresenceRequestDto;
import br.edu.unipam.tcc.entity.SystemConfiguration;
import br.edu.unipam.tcc.repository.SystemConfigurationRepository;
import br.edu.unipam.tcc.service.UazapiClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;

/**
 * Implementação do cliente HTTP da Uazapi utilizando o Spring RestClient.
 *
 * <p>As credenciais (base URL, instância, API key) são resolvidas a cada chamada a partir de
 * {@code system_configurations} (persistidas pelo painel {@code /setup}, incluindo instâncias
 * provisionadas automaticamente via {@code UazapiInstanceService.provisionInstance()}), com
 * fallback para os valores estáticos do {@code application.yml}/{@code .env}.</p>
 */
@Slf4j
@Service
public class UazapiClientServiceImpl implements UazapiClientService {

    private final RestClient restClient;
    private final SystemConfigurationRepository configRepository;
    private final String defaultBaseUrl;
    private final String defaultApiKey;
    private final String defaultInstance;

    public UazapiClientServiceImpl(
            RestClient.Builder restClientBuilder,
            SystemConfigurationRepository configRepository,
            @Value("${uazapi.base-url:https://free.uazapi.com}") String defaultBaseUrl,
            @Value("${uazapi.api-key:}") String defaultApiKey,
            @Value("${uazapi.instance:}") String defaultInstance
    ) {
        this.configRepository = configRepository;
        this.defaultBaseUrl = defaultBaseUrl != null ? defaultBaseUrl.replaceAll("/+$", "") : "https://free.uazapi.com";
        this.defaultApiKey = defaultApiKey != null ? defaultApiKey : "";
        this.defaultInstance = defaultInstance != null ? defaultInstance : "";
        this.restClient = restClientBuilder.build();
    }

    @Override
    public void sendTextMessage(String phoneNumber, String message) {
        if (phoneNumber == null || phoneNumber.isBlank() || message == null || message.isBlank()) {
            log.warn("[UazapiClient] Envio de texto cancelado: número ou mensagem vazios/nulos.");
            return;
        }

        UazapiCredentials creds = resolveCredentials();
        SendMessageRequestDto payload = new SendMessageRequestDto(phoneNumber.trim(), message.trim());
        log.info("[UazapiClient] Disparando envio de texto para [{}] via Uazapi (instância [{}])...", phoneNumber, creds.instance());

        // 1. Rota primária nativa: POST /send/text (Padrão Uazapi / UazapiGO)
        boolean sent = executePost(creds, "/send/text", payload, phoneNumber);

        // 2. Fallback com instância: POST /message/sendText/{instance}
        if (!sent && !creds.instance().isBlank()) {
            log.info("[UazapiClient] Tentando fallback para /message/sendText/{}...", creds.instance());
            sent = executePost(creds, "/message/sendText/" + creds.instance(), payload, phoneNumber);
        }

        // 3. Fallback legado: POST /message/sendText
        if (!sent) {
            log.info("[UazapiClient] Tentando fallback para /message/sendText...");
            executePost(creds, "/message/sendText", payload, phoneNumber);
        }
    }

    private boolean executePost(UazapiCredentials creds, String path, Object payload, String targetPhone) {
        try {
            restClient.post()
                    .uri(URI.create(creds.baseUrl() + path))
                    .header("apikey", creds.apiKey())
                    .header("token", creds.apiKey())
                    .header("Authorization", "Bearer " + creds.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("[UazapiClient] Mensagem de texto enviada com sucesso via [{}] para [{}]", path, targetPhone);
            return true;
        } catch (Exception e) {
            log.warn("[UazapiClient] Falha ao enviar via rota [{}] para [{}]: {}", path, targetPhone, e.getMessage());
            return false;
        }
    }

    @Override
    public void sendPresence(String phoneNumber, String presence) {
        if (phoneNumber == null || phoneNumber.isBlank() || presence == null || presence.isBlank()) {
            log.debug("[UazapiClient] Envio de presença cancelado: número ou presença vazios/nulos.");
            return;
        }

        UazapiCredentials creds = resolveCredentials();
        try {
            SendPresenceRequestDto payload = new SendPresenceRequestDto(phoneNumber.trim(), presence.trim());
            log.debug("[UazapiClient] Disparando POST /send/presence ({}) para [{}]", presence, phoneNumber);

            restClient.post()
                    .uri(URI.create(creds.baseUrl() + "/send/presence"))
                    .header("apikey", creds.apiKey())
                    .header("token", creds.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.debug("[UazapiClient] Estado de presença ({}) enviado para [{}]", presence, phoneNumber);
        } catch (Exception e) {
            log.debug("[UazapiClient] Gateway não processou envio de presença ({}) para [{}]: {}", presence, phoneNumber, e.getMessage());
        }
    }

    private UazapiCredentials resolveCredentials() {
        String baseUrl = configRepository.findByConfigKey("uazapi.base-url")
                .map(SystemConfiguration::getConfigValue)
                .filter(value -> !value.isBlank())
                .orElse(defaultBaseUrl);
        String instance = configRepository.findByConfigKey("uazapi.instance")
                .map(SystemConfiguration::getConfigValue)
                .filter(value -> !value.isBlank())
                .orElse(defaultInstance);
        String apiKey = configRepository.findByConfigKey("uazapi.api-key")
                .map(SystemConfiguration::getConfigValue)
                .filter(value -> !value.isBlank())
                .orElse(defaultApiKey);

        return new UazapiCredentials(baseUrl.replaceAll("/+$", ""), instance, apiKey);
    }

    private record UazapiCredentials(String baseUrl, String instance, String apiKey) {
    }
}
