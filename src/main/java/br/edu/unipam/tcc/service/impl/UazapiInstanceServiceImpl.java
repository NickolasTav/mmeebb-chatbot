package br.edu.unipam.tcc.service.impl;

import br.edu.unipam.tcc.dto.UazapiConnectRequestDto;
import br.edu.unipam.tcc.dto.UazapiProvisionResponseDto;
import br.edu.unipam.tcc.dto.UazapiQrCodeDto;
import br.edu.unipam.tcc.dto.UazapiSetupStatusDto;
import br.edu.unipam.tcc.entity.SystemConfiguration;
import br.edu.unipam.tcc.repository.SystemConfigurationRepository;
import br.edu.unipam.tcc.service.UazapiInstanceService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gerencia a conexão da instância Uazapi: solicitação de QR Code, consulta de status de
 * pareamento e sincronização automática do webhook, com fallback entre rotas conhecidas
 * de diferentes variantes do gateway (Uazapi oficial / UazapiGO).
 */
@Slf4j
@Service
public class UazapiInstanceServiceImpl implements UazapiInstanceService {

    private final RestClient restClient;
    private final SystemConfigurationRepository configRepository;
    private final String defaultBaseUrl;
    private final String defaultApiKey;
    private final String defaultInstance;
    private final String defaultAdminToken;

    public UazapiInstanceServiceImpl(
            RestClient.Builder restClientBuilder,
            SystemConfigurationRepository configRepository,
            @Value("${uazapi.base-url:https://free.uazapi.com}") String defaultBaseUrl,
            @Value("${uazapi.api-key:}") String defaultApiKey,
            @Value("${uazapi.instance:}") String defaultInstance,
            @Value("${uazapi.admin-token:}") String defaultAdminToken
    ) {
        this.restClient = restClientBuilder.build();
        this.configRepository = configRepository;
        this.defaultBaseUrl = defaultBaseUrl != null ? defaultBaseUrl.replaceAll("/+$", "") : "https://free.uazapi.com";
        this.defaultApiKey = defaultApiKey != null ? defaultApiKey : "";
        this.defaultInstance = defaultInstance != null ? defaultInstance : "";
        this.defaultAdminToken = defaultAdminToken != null ? defaultAdminToken : "";
    }

    @Override
    @Transactional
    public UazapiQrCodeDto requestQrCode(UazapiConnectRequestDto request) {
        UazapiCredentials creds = persistAndResolve(request);

        if (creds.instance().isBlank() || !creds.hasApiKey()) {
            UazapiProvisionResponseDto provisioned = provisionInstance();
            if (!provisioned.success()) {
                return new UazapiQrCodeDto("ERROR", null, null, null, null, provisioned.message());
            }
            creds = resolveCredentials();
        }

        try {
            JsonNode body = post(creds, "/instance/connect", Map.of());
            return toQrCodeDto(creds.instance(), body, "QR Code gerado. Escaneie com o WhatsApp no seu celular.");
        } catch (RestClientException e) {
            log.warn("[UazapiInstanceService] Falha ao solicitar QR Code para [{}]: {}", creds.instance(), e.getMessage());
            return new UazapiQrCodeDto("ERROR", creds.instance(), null, null, null,
                    "Não foi possível obter o QR Code. No servidor demo gratuito, a instância expira após 1h — " +
                            "tente provisionar uma nova.");
        }
    }

    @Override
    @Transactional
    public UazapiProvisionResponseDto provisionInstance() {
        if (defaultAdminToken.isBlank()) {
            return new UazapiProvisionResponseDto(false, null, null,
                    "UAZAPI_ADMIN_TOKEN não configurado. Gere um admintoken no painel da sua conta Uazapi " +
                            "(free.uazapi.com/uazapi.dev) e defina a variável de ambiente.");
        }

        String baseUrl = resolveCredentials().baseUrl();
        String instanceName = "mmeebb-" + UUID.randomUUID().toString().substring(0, 8);

        try {
            JsonNode body = restClient.post()
                    .uri(URI.create(baseUrl + "/instance/init"))
                    .header("admintoken", defaultAdminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("name", instanceName))
                    .retrieve()
                    .body(JsonNode.class);

            String newToken = firstText(body, "token", "instance.token");
            if (newToken == null) {
                log.warn("[UazapiInstanceService] Instância [{}] criada, mas resposta sem token reconhecível.", instanceName);
                return new UazapiProvisionResponseDto(false, instanceName, null,
                        "Instância criada na Uazapi, mas não foi possível extrair o token da resposta.");
            }

            saveConfig("uazapi.instance", instanceName, "Nome da instância ativa na Uazapi");
            saveConfig("uazapi.api-key", newToken, "Chave de autenticação da instância");

            log.info("[UazapiInstanceService] Instância [{}] provisionada automaticamente.", instanceName);
            return new UazapiProvisionResponseDto(true, instanceName, maskApiKey(newToken),
                    "Instância provisionada automaticamente. No servidor demo gratuito ela expira em 1h.");
        } catch (RestClientException e) {
            log.warn("[UazapiInstanceService] Falha ao provisionar instância [{}]: {}", instanceName, e.getMessage());
            return new UazapiProvisionResponseDto(false, instanceName, null,
                    "Falha ao provisionar instância na Uazapi: " + e.getMessage());
        }
    }

    @Override
    public UazapiSetupStatusDto.UazapiStatus getInstanceStatus() {
        UazapiCredentials creds = resolveCredentials();

        if (creds.instance().isBlank()) {
            return offlineStatus(creds);
        }

        try {
            JsonNode body = get(creds, "/instance/status");
            return toStatus(creds, body);
        } catch (RestClientException primaryError) {
            log.debug("[UazapiInstanceService] /instance/status falhou para [{}], tentando /instance/connectionState: {}",
                    creds.instance(), primaryError.getMessage());
            try {
                JsonNode body = get(creds, "/instance/connectionState");
                return toStatus(creds, body);
            } catch (RestClientException fallbackError) {
                log.debug("[UazapiInstanceService] Instância [{}] indisponível: {}", creds.instance(), fallbackError.getMessage());
                return offlineStatus(creds);
            }
        }
    }

    @Override
    public boolean configureWebhook(String webhookUrl) {
        UazapiCredentials creds = resolveCredentials();
        if (creds.instance().isBlank() || webhookUrl == null || webhookUrl.isBlank()) {
            return false;
        }

        Map<String, Object> payload = Map.of(
                "url", webhookUrl,
                "events", List.of("messages.upsert", "messages"),
                "enabled", true
        );

        try {
            post(creds, "/webhook", payload);
            saveConfig("webhook.last-synced-url", webhookUrl, "Última URL de webhook configurada na Uazapi");
            return true;
        } catch (RestClientException primaryError) {
            log.debug("[UazapiInstanceService] /webhook falhou para [{}], tentando /instance/webhook: {}",
                    creds.instance(), primaryError.getMessage());
            try {
                post(creds, "/instance/webhook", payload);
                saveConfig("webhook.last-synced-url", webhookUrl, "Última URL de webhook configurada na Uazapi");
                return true;
            } catch (RestClientException fallbackError) {
                log.warn("[UazapiInstanceService] Falha ao sincronizar webhook para [{}]: {}",
                        creds.instance(), fallbackError.getMessage());
                return false;
            }
        }
    }

    @Override
    public void sendSmokeTestMessage(String phoneNumber, String message) {
        if (phoneNumber == null || phoneNumber.isBlank() || message == null || message.isBlank()) {
            return;
        }

        UazapiCredentials creds = resolveCredentials();
        try {
            post(creds, "/send/text", Map.of("number", phoneNumber.trim(), "text", message.trim()));
        } catch (RestClientException e) {
            log.warn("[UazapiInstanceService] Falha ao enviar mensagem de teste para [{}]: {}", phoneNumber, e.getMessage());
        }
    }

    private JsonNode get(UazapiCredentials creds, String path) {
        return restClient.get()
                .uri(URI.create(creds.baseUrl() + path))
                .header("apikey", creds.apiKey())
                .header("token", creds.apiKey())
                .header("Authorization", "Bearer " + creds.apiKey())
                .retrieve()
                .body(JsonNode.class);
    }

    private JsonNode post(UazapiCredentials creds, String path, Object payload) {
        return restClient.post()
                .uri(URI.create(creds.baseUrl() + path))
                .header("apikey", creds.apiKey())
                .header("token", creds.apiKey())
                .header("Authorization", "Bearer " + creds.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(JsonNode.class);
    }

    private UazapiQrCodeDto toQrCodeDto(String instance, JsonNode body, String message) {
        String qrcode = firstText(body, "qrcode", "qrCode", "QRCode", "base64", "Base64", "instance.qrcode");
        String pairingCode = firstText(body, "pairingCode", "paircode", "code");
        return new UazapiQrCodeDto("CONNECTING", instance, qrcode, pairingCode, 45, message);
    }

    private UazapiSetupStatusDto.UazapiStatus toStatus(UazapiCredentials creds, JsonNode body) {
        String rawState = firstText(body, "state", "status", "Status", "instance.status", "instance.state");
        String state = rawState != null ? rawState.toLowerCase() : "close";
        boolean connected = "open".equals(state) || "connected".equals(state)
                || (body != null && body.path("connected").asBoolean(false))
                || (body != null && body.path("loggedIn").asBoolean(false))
                || (body != null && body.path("status").path("connected").asBoolean(false))
                || (body != null && body.path("status").path("loggedIn").asBoolean(false));
        String profileName = firstText(body, "profileName", "instance.profileName", "name");
        String profilePictureUrl = firstText(body, "profilePictureUrl", "instance.profilePictureUrl");

        return new UazapiSetupStatusDto.UazapiStatus(
                creds.baseUrl(), creds.instance(), creds.hasApiKey(), maskApiKey(creds.apiKey()),
                state, connected, profileName, profilePictureUrl);
    }

    private UazapiSetupStatusDto.UazapiStatus offlineStatus(UazapiCredentials creds) {
        return new UazapiSetupStatusDto.UazapiStatus(
                creds.baseUrl(), creds.instance(), creds.hasApiKey(), maskApiKey(creds.apiKey()),
                "close", false, null, null);
    }

    private String firstText(JsonNode root, String... paths) {
        if (root == null) {
            return null;
        }
        for (String path : paths) {
            JsonNode value = root;
            for (String part : path.split("\\.")) {
                if (value == null) {
                    break;
                }
                value = value.get(part);
            }
            if (value != null && value.isTextual() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return null;
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        if (apiKey.length() <= 7) {
            return "***";
        }
        return apiKey.substring(0, 3) + "***" + apiKey.substring(apiKey.length() - 4);
    }

    private UazapiCredentials persistAndResolve(UazapiConnectRequestDto request) {
        String baseUrl = defaultBaseUrl;
        String instance = defaultInstance;
        String apiKey = defaultApiKey;

        if (request != null) {
            if (request.baseUrl() != null && !request.baseUrl().isBlank()) {
                baseUrl = request.baseUrl().replaceAll("/+$", "");
                saveConfig("uazapi.base-url", baseUrl, "URL base do gateway Uazapi");
            }
            if (request.instance() != null && !request.instance().isBlank()) {
                instance = request.instance().trim();
                saveConfig("uazapi.instance", instance, "Nome da instância ativa na Uazapi");
            }
            if (request.apiKey() != null && !request.apiKey().isBlank()) {
                apiKey = request.apiKey().trim();
                saveConfig("uazapi.api-key", apiKey, "Chave de autenticação da instância");
            }
        }

        return resolveStoredOrDefault(baseUrl, instance, apiKey);
    }

    private UazapiCredentials resolveCredentials() {
        return resolveStoredOrDefault(defaultBaseUrl, defaultInstance, defaultApiKey);
    }

    private UazapiCredentials resolveStoredOrDefault(String baseUrl, String instance, String apiKey) {
        String resolvedBaseUrl = configRepository.findByConfigKey("uazapi.base-url")
                .map(SystemConfiguration::getConfigValue)
                .filter(value -> !value.isBlank())
                .orElse(baseUrl);
        String resolvedInstance = configRepository.findByConfigKey("uazapi.instance")
                .map(SystemConfiguration::getConfigValue)
                .filter(value -> !value.isBlank())
                .orElse(instance);
        String resolvedApiKey = configRepository.findByConfigKey("uazapi.api-key")
                .map(SystemConfiguration::getConfigValue)
                .filter(value -> !value.isBlank())
                .orElse(apiKey);

        return new UazapiCredentials(
                resolvedBaseUrl.replaceAll("/+$", ""),
                resolvedInstance != null ? resolvedInstance : "",
                resolvedApiKey != null ? resolvedApiKey : ""
        );
    }

    private void saveConfig(String key, String value, String description) {
        SystemConfiguration config = configRepository.findByConfigKey(key)
                .orElse(SystemConfiguration.builder().configKey(key).build());
        config.setConfigValue(value);
        config.setDescription(description);
        configRepository.save(config);
    }

    private record UazapiCredentials(String baseUrl, String instance, String apiKey) {
        boolean hasApiKey() {
            return apiKey != null && !apiKey.isBlank();
        }
    }
}
