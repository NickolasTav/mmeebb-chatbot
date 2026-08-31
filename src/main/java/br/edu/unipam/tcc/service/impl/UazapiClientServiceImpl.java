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

        try {
            SendMessageRequestDto payload = new SendMessageRequestDto(phoneNumber.trim(), message.trim());
            log.info("[UazapiClient] Disparando POST /message/sendText para [{}]", phoneNumber);

            restClient.post()
                    .uri("/message/sendText")
                    .header("apikey", this.apiKey)
                    .header("token", this.apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("[UazapiClient] Mensagem de texto enviada com sucesso para [{}]", phoneNumber);
        } catch (Exception e) {
            log.error("[UazapiClient] Falha ao enviar mensagem de texto para [{}]: {}", phoneNumber, e.getMessage(), e);
        }
    }

    @Override
    public void sendPresence(String phoneNumber, String presence) {
        if (phoneNumber == null || phoneNumber.isBlank() || presence == null || presence.isBlank()) {
            log.warn("[UazapiClient] Envio de presença cancelado: número ou presença vazios/nulos.");
            return;
        }

        try {
            SendPresenceRequestDto payload = new SendPresenceRequestDto(phoneNumber.trim(), presence.trim());
            log.debug("[UazapiClient] Disparando POST /chat/sendPresence ({}) para [{}]", presence, phoneNumber);

            restClient.post()
                    .uri("/chat/sendPresence")
                    .header("apikey", this.apiKey)
                    .header("token", this.apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.debug("[UazapiClient] Estado de presença ({}) enviado para [{}]", presence, phoneNumber);
        } catch (Exception e) {
            log.warn("[UazapiClient] Não foi possível atualizar presença ({}) para [{}]: {}", presence, phoneNumber, e.getMessage());
        }
    }
}
