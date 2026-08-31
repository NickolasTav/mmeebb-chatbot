package br.edu.unipam.tcc.consumer;

import br.edu.unipam.tcc.config.RabbitMQConfig;
import br.edu.unipam.tcc.dto.OutgoingMessageDto;
import br.edu.unipam.tcc.dto.UazapiWebhookDto;
import br.edu.unipam.tcc.service.ChatFlowOrchestrator;
import br.edu.unipam.tcc.service.UazapiClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Asynchronous RabbitMQ consumer for WhatsApp events.
 * Manages outgoing queue with anti-ban rate limiting and incoming queue
 * with typing presence simulation before delegating to the conversation orchestrator.
 */
@Slf4j
@Component
public class WhatsappMessageConsumer {

    private final UazapiClientService uazapiClientService;
    private final ChatFlowOrchestrator chatFlowOrchestrator;
    private final long defaultTypingDelayMs;

    public WhatsappMessageConsumer(
            UazapiClientService uazapiClientService,
            ChatFlowOrchestrator chatFlowOrchestrator,
            @Value("${uazapi.typing-delay-ms:1500}") long defaultTypingDelayMs
    ) {
        this.uazapiClientService = uazapiClientService;
        this.chatFlowOrchestrator = chatFlowOrchestrator;
        this.defaultTypingDelayMs = defaultTypingDelayMs;
    }

    /**
     * Consumes messages from the outgoing queue (whatsapp.outgoing.queue).
     * Single concurrency to avoid bursts.
     *
     * @param outgoingDto Outgoing message payload.
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
     * Consumes incoming raw messages from the incoming queue (whatsapp.incoming.queue).
     * Simulates typing presence with human delay and delegates to the chat orchestrator.
     *
     * @param incomingDto Deserialized webhook event payload.
     */
    @RabbitListener(queues = RabbitMQConfig.INCOMING_QUEUE)
    public void consumeIncomingMessage(UazapiWebhookDto incomingDto) {
        if (incomingDto == null) {
            log.warn("[WhatsappConsumer] Evento de entrada nulo recebido na fila.");
            return;
        }

        String phone = incomingDto.getCleanPhoneNumber();
        if (phone == null || phone.isBlank()) {
            log.warn("[WhatsappConsumer] Mensagem de entrada ignorada: telefone limpo vazio.");
            return;
        }

        if (Boolean.TRUE.equals(incomingDto.fromMe())) {
            log.debug("[WhatsappConsumer] Mensagem fromMe ignorada para [{}]", phone);
            return;
        }

        log.info("[WhatsappConsumer] Mensagem recebida de [{}]: \"{}\"", phone, incomingDto.text());

        try {
            // 1. Simula presença de digitação no WhatsApp (composing)
            uazapiClientService.sendPresence(phone, "composing");

            // 2. Aplica delay de digitação humano (anti-ban)
            if (defaultTypingDelayMs > 0) {
                Thread.sleep(defaultTypingDelayMs);
            }

            // 3. Delega o processamento para o orquestrador conversacional
            chatFlowOrchestrator.processIncomingMessage(incomingDto);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[WhatsappConsumer] Thread interrompida durante typing delay para [{}]: {}", phone, e.getMessage());
        } catch (Exception e) {
            log.error("[WhatsappConsumer] Falha no processamento do fluxo conversacional para [{}]: {}", phone, e.getMessage(), e);
        } finally {
            // 4. Sempre reseta a presença para 'paused'
            try {
                uazapiClientService.sendPresence(phone, "paused");
            } catch (Exception e) {
                log.warn("[WhatsappConsumer] Falha ao resetar presença para [{}]: {}", phone, e.getMessage());
            }
        }
    }
}
