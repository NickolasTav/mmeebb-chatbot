package br.edu.unipam.tcc.controller;

import br.edu.unipam.tcc.config.RabbitMQConfig;
import br.edu.unipam.tcc.dto.UazapiWebhookDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller responsável por receber eventos de webhook da Uazapi (WhatsApp),
 * validar os dados recebidos e enfileirar as mensagens válidas de forma não bloqueante.
 */
@Slf4j
@RestController
@RequestMapping("/webhook/uazapi")
@RequiredArgsConstructor
public class UazapiWebhookController {

    private final RabbitTemplate rabbitTemplate;

    @PostMapping
    public ResponseEntity<Map<String, String>> handleWebhook(@RequestBody(required = false) UazapiWebhookDto payload) {
        if (payload == null) {
            log.warn("[UazapiWebhook] Payload nulo recebido no webhook.");
            return ResponseEntity.ok(Map.of("status", "IGNORED_NULL_PAYLOAD"));
        }

        if (Boolean.TRUE.equals(payload.fromMe())) {
            log.debug("[UazapiWebhook] Mensagem própria do bot ignorada (fromMe = true). RemoteJid: {}", payload.remoteJid());
            return ResponseEntity.ok(Map.of("status", "IGNORED_FROM_ME"));
        }

        if (payload.text() == null || payload.text().isBlank()) {
            log.debug("[UazapiWebhook] Evento sem conteúdo de texto ignorado. RemoteJid: {}", payload.remoteJid());
            return ResponseEntity.ok(Map.of("status", "IGNORED_EMPTY_TEXT"));
        }

        String cleanPhone = payload.getCleanPhoneNumber();
        log.info("[UazapiWebhook] Mensagem recebida de [{}] (JID: {}). Enfileirando no RabbitMQ...", cleanPhone, payload.remoteJid());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.INCOMING_ROUTING_KEY,
                payload
        );

        return ResponseEntity.ok(Map.of("status", "RECEIVED"));
    }
}
