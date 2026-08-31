package br.edu.unipam.tcc.consumer;

import br.edu.unipam.tcc.config.RabbitMQConfig;
import br.edu.unipam.tcc.dto.OutgoingMessageDto;
import br.edu.unipam.tcc.service.UazapiClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Consumidor RabbitMQ dedicado à fila de saída (whatsapp.outgoing.queue).
 * Garante concorrência estritamente unitária (concurrency = 1) e aplica
 * mecanismos de proteção anti-ban da Meta:
 * 1. Jitter pseudoaleatório entre mensagens (3 a 6 segundos);
 * 2. Simulação de presença de digitação (composing por 1.5s);
 * 3. Envio final da mensagem de texto via Uazapi.
 */
@Slf4j
@Component
public class WhatsappOutgoingConsumer {

    private final UazapiClientService uazapiClientService;
    private final long minJitterMs;
    private final long maxJitterMs;
    private final long composingDelayMs;

    public WhatsappOutgoingConsumer(
            UazapiClientService uazapiClientService,
            @Value("${uazapi.anti-ban.min-jitter-ms:3000}") long minJitterMs,
            @Value("${uazapi.anti-ban.max-jitter-ms:6000}") long maxJitterMs,
            @Value("${uazapi.anti-ban.composing-delay-ms:1500}") long composingDelayMs
    ) {
        this.uazapiClientService = uazapiClientService;
        this.minJitterMs = minJitterMs;
        this.maxJitterMs = maxJitterMs;
        this.composingDelayMs = composingDelayMs;
    }

    /**
     * Consome mensagens da fila de saída sequencialmente para evitar rajadas (bursts).
     *
     * @param outgoingDto DTO contendo o número do destinatário e corpo da mensagem.
     */
    @RabbitListener(queues = RabbitMQConfig.OUTGOING_QUEUE, concurrency = "1")
    public void consumeOutgoingMessage(OutgoingMessageDto outgoingDto) {
        if (outgoingDto == null || outgoingDto.phoneNumber() == null || outgoingDto.phoneNumber().isBlank()) {
            log.warn("[WhatsappOutgoingConsumer] Mensagem de saída ignorada: payload ou telefone nulo/vazio.");
            return;
        }

        String phone = outgoingDto.phoneNumber().trim();
        String text = outgoingDto.messageText();

        try {
            // 1. Aplica Jitter pseudoaleatório entre mensagens
            long jitter = calculateJitterMs();
            if (jitter > 0) {
                log.debug("[WhatsappOutgoingConsumer] Aplicando jitter anti-ban de {}ms para [{}]", jitter, phone);
                Thread.sleep(jitter);
            }

            // 2. Simula presença 'composing' (digitando...)
            uazapiClientService.sendPresence(phone, "composing");

            // 3. Pausa de digitação humana
            if (composingDelayMs > 0) {
                Thread.sleep(composingDelayMs);
            }

            // 4. Dispara a mensagem de texto via Uazapi
            uazapiClientService.sendTextMessage(phone, text);
            log.info("[WhatsappOutgoingConsumer] Mensagem despachada com sucesso para [{}]", phone);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[WhatsappOutgoingConsumer] Thread interrompida durante delay anti-ban para [{}]: {}",
                    phone, e.getMessage());
        } catch (Exception e) {
            log.error("[WhatsappOutgoingConsumer] Falha no processamento de envio para [{}]: {}",
                    phone, e.getMessage(), e);
        } finally {
            // 5. Sempre reseta a presença para 'paused'
            try {
                uazapiClientService.sendPresence(phone, "paused");
            } catch (Exception e) {
                log.warn("[WhatsappOutgoingConsumer] Falha ao resetar presença para [{}]: {}", phone, e.getMessage());
            }
        }
    }

    private long calculateJitterMs() {
        if (maxJitterMs > minJitterMs && minJitterMs >= 0) {
            return ThreadLocalRandom.current().nextLong(minJitterMs, maxJitterMs + 1);
        }
        return Math.max(0, minJitterMs);
    }
}
