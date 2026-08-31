package br.edu.unipam.tcc.consumer;

import br.edu.unipam.tcc.config.RabbitMQConfig;
import br.edu.unipam.tcc.dto.OutgoingMessageDto;
import br.edu.unipam.tcc.dto.UazapiWebhookDto;
import br.edu.unipam.tcc.service.UazapiClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Consumidor assíncrono de eventos do RabbitMQ para WhatsApp.
 * Gerencia a fila de saída com anti-ban (taxa controlada, presença composing e delay de digitação)
 * e o consumo da fila de entrada.
 */
@Slf4j
@Component
public class WhatsappMessageConsumer {

    private final UazapiClientService uazapiClientService;
    private final long defaultTypingDelayMs;

    public WhatsappMessageConsumer(
            UazapiClientService uazapiClientService,
            @Value("${uazapi.typing-delay-ms:2000}") long defaultTypingDelayMs
    ) {
        this.uazapiClientService = uazapiClientService;
        this.defaultTypingDelayMs = defaultTypingDelayMs;
    }

    /**
     * Consome mensagens da fila de saída (whatsapp.outgoing.queue).
     * Garante concorrência unitária (concurrency = "1") para evitar rajadas e disparos simultâneos.
     *
     * @param outgoingDto Payload contendo telefone, mensagem e delay customizado opcional.
     */
    @RabbitListener(queues = RabbitMQConfig.OUTGOING_QUEUE, concurrency = "1")
    public void consumeOutgoingMessage(OutgoingMessageDto outgoingDto) {
        if (outgoingDto == null || outgoingDto.phoneNumber() == null || outgoingDto.phoneNumber().isBlank()) {
            log.warn("[WhatsappConsumer] Mensagem de saída ignorada: payload ou telefone nulo/vazio.");
            return;
        }

        String phone = outgoingDto.phoneNumber().trim();
        String text = outgoingDto.messageText();
        long delayMs = outgoingDto.typingDelayMs() != null ? outgoingDto.typingDelayMs() : defaultTypingDelayMs;

        log.info("[WhatsappConsumer] Processando saída para [{}] (Delay: {}ms)", phone, delayMs);

        try {
            // 1. Simula presença 'composing' (digitando...)
            uazapiClientService.sendPresence(phone, "composing");

            // 2. Aplica atraso controlado (anti-ban)
            if (delayMs > 0) {
                Thread.sleep(delayMs);
            }

            // 3. Dispara a mensagem de texto
            uazapiClientService.sendTextMessage(phone, text);

            // 4. Atualiza estado de presença para 'paused'
            uazapiClientService.sendPresence(phone, "paused");

            log.info("[WhatsappConsumer] Despacho concluído com sucesso para [{}]", phone);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[WhatsappConsumer] Thread interrompida durante typing delay para [{}]: {}", phone, e.getMessage());
        } catch (Exception e) {
            log.error("[WhatsappConsumer] Falha no processamento de envio para [{}]: {}", phone, e.getMessage(), e);
        }
    }

    /**
     * Consome mensagens brutas da fila de entrada (whatsapp.incoming.queue).
     * Ponto de entrada assíncrono para o motor de conversação e IA do MMEEBB.
     *
     * @param incomingDto Evento deserializado recebido via webhook.
     */
    @RabbitListener(queues = RabbitMQConfig.INCOMING_QUEUE)
    public void consumeIncomingMessage(UazapiWebhookDto incomingDto) {
        if (incomingDto == null) {
            log.warn("[WhatsappConsumer] Evento de entrada nulo recebido na fila.");
            return;
        }

        log.info("[WhatsappConsumer] Mensagem recebida da fila: Remetente=[{}], Texto=[{}]",
                incomingDto.getCleanPhoneNumber(),
                incomingDto.text());
    }
}
