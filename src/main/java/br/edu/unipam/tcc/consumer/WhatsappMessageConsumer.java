package br.edu.unipam.tcc.consumer;

import br.edu.unipam.tcc.config.RabbitMQConfig;
import br.edu.unipam.tcc.dto.OutgoingMessageDto;
import br.edu.unipam.tcc.dto.UazapiWebhookDto;
import br.edu.unipam.tcc.observability.CorrelationMdcHelper;
import br.edu.unipam.tcc.observability.MmeebbMetrics;
import br.edu.unipam.tcc.service.ChatFlowOrchestrator;
import br.edu.unipam.tcc.service.UazapiClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Asynchronous RabbitMQ consumer for incoming WhatsApp events.
 * Listens to incoming webhook queue, simulates typing presence with human delay,
 * and delegates execution to the conversational flow orchestrator.
 * Runs with multiple concurrent consumers (see {@code uazapi.inbound.concurrency}) so a slow
 * open-ended question (Gemini/RAG) for one student does not block the others; a per-phone lock
 * still serializes messages from the same student to protect the Redis-backed session state.
 */
@Slf4j
@Component
public class WhatsappMessageConsumer {

    private final UazapiClientService uazapiClientService;
    private final ChatFlowOrchestrator chatFlowOrchestrator;
    private final MmeebbMetrics mmeebbMetrics;
    private final long defaultTypingDelayMs;
    private final ConcurrentHashMap<String, ReentrantLock> phoneLocks = new ConcurrentHashMap<>();

    public WhatsappMessageConsumer(
            UazapiClientService uazapiClientService,
            ChatFlowOrchestrator chatFlowOrchestrator,
            MmeebbMetrics mmeebbMetrics,
            @Value("${uazapi.typing-delay-ms:1500}") long defaultTypingDelayMs
    ) {
        this.uazapiClientService = uazapiClientService;
        this.chatFlowOrchestrator = chatFlowOrchestrator;
        this.mmeebbMetrics = mmeebbMetrics;
        this.defaultTypingDelayMs = defaultTypingDelayMs;
    }

    /**
     * Consumes incoming raw messages from the incoming queue (whatsapp.incoming.queue).
     * Simulates typing presence with human delay and delegates to the chat orchestrator.
     *
     * @param incomingDto Deserialized webhook event payload.
     */
    @RabbitListener(queues = RabbitMQConfig.INCOMING_QUEUE, concurrency = "${uazapi.inbound.concurrency:3-6}")
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
        mmeebbMetrics.recordUazapiMessage("INBOUND");

        // Serializa mensagens do MESMO telefone (o ChatSessionState no Redis não tem lock próprio),
        // mas deixa telefones diferentes rodarem em paralelo entre os consumers do listener.
        ReentrantLock lock = phoneLocks.computeIfAbsent(phone, p -> new ReentrantLock());
        lock.lock();
        try {
            CorrelationMdcHelper.runWithContext(phone, "INBOUND", () -> {
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
            });
        } finally {
            lock.unlock();
        }
    }
}
