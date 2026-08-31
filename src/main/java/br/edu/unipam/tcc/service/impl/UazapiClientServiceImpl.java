package br.edu.unipam.tcc.service.impl;

import br.edu.unipam.tcc.dto.SendMessageRequestDto;
import br.edu.unipam.tcc.dto.SendPresenceRequestDto;
import br.edu.unipam.tcc.service.UazapiClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Implementação do cliente HTTP da Uazapi utilizando o Spring RestClient.
 */
@Slf4j
@Service
public class UazapiClientServiceImpl implements UazapiClientService {

    private final RestClient restClient;
    private final String baseUrl;
    private final String apiKey;
    private final String instance;

    public UazapiClientServiceImpl(
            RestClient.Builder restClientBuilder,
            @Value("${uazapi.base-url:https://free.uazapi.com}") String baseUrl,
            @Value("${uazapi.api-key:}") String apiKey,
            @Value("${uazapi.instance:}") String instance
    ) {
        this.baseUrl = baseUrl != null ? baseUrl.replaceAll("/+$", "") : "https://free.uazapi.com";
        this.apiKey = apiKey != null ? apiKey : "";
        this.instance = instance != null ? instance : "";
        this.restClient = restClientBuilder
                .baseUrl(this.baseUrl)
                .build();
    }

    @Override
    public void sendTextMessage(String phoneNumber, String message) {
        if (phoneNumber == null || phoneNumber.isBlank() || message == null || message.isBlank()) {
            log.warn("[UazapiClient] Envio de texto cancelado: número ou mensagem vazios/nulos.");
            return;
        }

        SendMessageRequestDto payload = new SendMessageRequestDto(phoneNumber.trim(), message.trim());
        log.info("[UazapiClient] Disparando envio de texto para [{}] via Uazapi...", phoneNumber);

        // 1. Rota primária nativa: POST /send/text (Padrão Uazapi / UazapiGO)
        boolean sent = executePost("/send/text", payload, phoneNumber);
        
        // 2. Fallback com instância: POST /message/sendText/{instance}
        if (!sent && instance != null && !instance.isBlank()) {
            log.info("[UazapiClient] Tentando fallback para /message/sendText/{}...", instance);
            sent = executePost("/message/sendText/" + instance, payload, phoneNumber);
        }
        
        // 3. Fallback legado: POST /message/sendText
        if (!sent) {
            log.info("[UazapiClient] Tentando fallback para /message/sendText...");
            executePost("/message/sendText", payload, phoneNumber);
        }
    }

    private boolean executePost(String uri, Object payload, String targetPhone) {
        try {
            restClient.post()
                    .uri(uri)
                    .header("apikey", this.apiKey)
                    .header("token", this.apiKey)
                    .header("Authorization", "Bearer " + this.apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("[UazapiClient] Mensagem de texto enviada com sucesso via [{}] para [{}]", uri, targetPhone);
            return true;
        } catch (Exception e) {
            log.warn("[UazapiClient] Falha ao enviar via rota [{}] para [{}]: {}", uri, targetPhone, e.getMessage());
            return false;
        }
    }

    @Override
    public void sendPresence(String phoneNumber, String presence) {
        if (phoneNumber == null || phoneNumber.isBlank() || presence == null || presence.isBlank()) {
            log.debug("[UazapiClient] Envio de presença cancelado: número ou presença vazios/nulos.");
            return;
        }

        try {
            SendPresenceRequestDto payload = new SendPresenceRequestDto(phoneNumber.trim(), presence.trim());
            log.debug("[UazapiClient] Disparando POST /send/presence ({}) para [{}]", presence, phoneNumber);

            restClient.post()
                    .uri("/send/presence")
                    .header("apikey", this.apiKey)
                    .header("token", this.apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.debug("[UazapiClient] Estado de presença ({}) enviado para [{}]", presence, phoneNumber);
        } catch (Exception e) {
            log.debug("[UazapiClient] Gateway não processou envio de presença ({}) para [{}]: {}", presence, phoneNumber, e.getMessage());
        }
    }
}
