package br.edu.unipam.tcc.consumer;

import br.edu.unipam.tcc.config.RabbitMQConfig;
import br.edu.unipam.tcc.dto.OutgoingMessageDto;
import br.edu.unipam.tcc.messaging.AntiBanMode;
import br.edu.unipam.tcc.messaging.PerRecipientDispatcher;
import br.edu.unipam.tcc.observability.CorrelationMdcHelper;
import br.edu.unipam.tcc.observability.MmeebbMetrics;
import br.edu.unipam.tcc.service.UazapiClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Consumidor RabbitMQ dedicado à fila de saída (whatsapp.outgoing.queue), com concorrência
 * estritamente unitária (concurrency = 1) e os mecanismos de proteção anti-ban da Meta:
 * 1. Jitter pseudoaleatório entre mensagens (3 a 6 segundos);
 * 2. Simulação de presença de digitação (composing por 1.5s);
 * 3. Envio final da mensagem de texto via Uazapi.
 *
 * <p>Estratégia ({@code uazapi.anti-ban.mode}):
 * <ul>
 *   <li>{@code global} (padrão, original): o próprio listener dorme o jitter e o composing de cada
 *       mensagem; alunos diferentes esperam uns pelos outros.</li>
 *   <li>{@code per-recipient}: o listener só sorteia o jitter e entrega ao {@link PerRecipientDispatcher},
 *       que mantém a mesma cadência por aluno e um teto global entre textos de alunos diferentes.</li>
 * </ul>
 */
@Slf4j
@Component
public class WhatsappOutgoingConsumer {

    private final UazapiClientService uazapiClientService;
    private final MmeebbMetrics mmeebbMetrics;
    private final long minJitterMs;
    private final long maxJitterMs;
    private final long composingDelayMs;
    private final AntiBanMode mode;
    private final PerRecipientDispatcher dispatcher;

    /** Modo global (estratégia original), sem despachante. */
    public WhatsappOutgoingConsumer(
            UazapiClientService uazapiClientService,
            MmeebbMetrics mmeebbMetrics,
            long minJitterMs,
            long maxJitterMs,
            long composingDelayMs
    ) {
        this(uazapiClientService, mmeebbMetrics, minJitterMs, maxJitterMs, composingDelayMs,
                AntiBanMode.GLOBAL.propertyValue(), null);
    }

    @Autowired
    public WhatsappOutgoingConsumer(
            UazapiClientService uazapiClientService,
            MmeebbMetrics mmeebbMetrics,
            @Value("${uazapi.anti-ban.min-jitter-ms:3000}") long minJitterMs,
            @Value("${uazapi.anti-ban.max-jitter-ms:6000}") long maxJitterMs,
            @Value("${uazapi.anti-ban.composing-delay-ms:1500}") long composingDelayMs,
            @Value("${uazapi.anti-ban.mode:global}") String mode,
            PerRecipientDispatcher dispatcher
    ) {
        this.uazapiClientService = uazapiClientService;
        this.mmeebbMetrics = mmeebbMetrics;
        this.minJitterMs = minJitterMs;
        this.maxJitterMs = maxJitterMs;
        this.composingDelayMs = composingDelayMs;
        this.mode = AntiBanMode.from(mode);
        this.dispatcher = dispatcher;
        if (this.mode == AntiBanMode.PER_RECIPIENT && dispatcher == null) {
            throw new IllegalStateException("uazapi.anti-ban.mode=per-recipient exige o PerRecipientDispatcher");
        }
        if (this.mode == AntiBanMode.PER_RECIPIENT) {
            log.info("[WhatsappOutgoingConsumer] Anti-ban de saida: modo per-recipient (jitter {}-{} ms, "
                            + "composing {} ms, teto global {} ms)",
                    minJitterMs, maxJitterMs, composingDelayMs, dispatcher.globalMinIntervalMs());
        } else {
            log.info("[WhatsappOutgoingConsumer] Anti-ban de saida: modo global (jitter {}-{} ms, composing {} ms)",
                    minJitterMs, maxJitterMs, composingDelayMs);
        }
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

        if (mode == AntiBanMode.PER_RECIPIENT) {
            // O despachante agenda o composing para depois do jitter; o listener volta na hora.
            dispatcher.dispatch(phone, calculateJitterMs(),
                    () -> CorrelationMdcHelper.runWithContext(phone, "OUTBOUND", () -> deliver(phone, text, 0)));
            return;
        }

        CorrelationMdcHelper.runWithContext(phone, "OUTBOUND", () -> deliver(phone, text, calculateJitterMs()));
    }

    /** Ciclo anti-ban de uma mensagem: jitter (se houver) → composing → pausa de digitação → texto → paused. */
    private void deliver(String phone, String text, long jitter) {
        try {
            // 1. Aplica Jitter pseudoaleatório entre mensagens
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
            mmeebbMetrics.recordUazapiMessage("OUTBOUND");
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
