package br.edu.unipam.tcc.messaging;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Agenda as entregas da fila de saída no modo {@link AntiBanMode#PER_RECIPIENT}.
 *
 * <p>O listener da fila de saída chama {@link #dispatch} e volta na hora (não dorme): o horário vem do
 * {@link RecipientPacer} e a entrega roda num pool pequeno. A ordem por telefone é garantida por
 * encadeamento: a entrega N+1 de um telefone só começa quando a N terminou, mesmo que o horário já tenha
 * chegado (uma chamada HTTP lenta não inverte a ordem das mensagens de um aluno). Telefones diferentes não
 * esperam uns pelos outros.
 *
 * <p>Entregas agendadas vivem em memória (a mensagem já foi confirmada no RabbitMQ). No desligamento normal,
 * {@link #shutdown()} espera as pendentes antes de fechar o pool; numa queda brusca elas se perdem.
 */
@Slf4j
@Component
public class PerRecipientDispatcher {

    static final int POOL_SIZE = 4;
    private static final long SHUTDOWN_DRAIN_SECONDS = 30;

    private final Clock clock;
    private final RecipientPacer pacer;
    private final ScheduledExecutorService executor;
    private final Map<String, CompletableFuture<Void>> tailByPhone = new ConcurrentHashMap<>();

    @Autowired
    public PerRecipientDispatcher(
            Clock clock,
            @Value("${uazapi.anti-ban.composing-delay-ms:1500}") long composingDelayMs,
            @Value("${uazapi.anti-ban.global-min-interval-ms:1000}") long globalMinIntervalMs) {
        this(clock, new RecipientPacer(composingDelayMs, globalMinIntervalMs), newPool());
    }

    PerRecipientDispatcher(Clock clock, RecipientPacer pacer, ScheduledExecutorService executor) {
        this.clock = clock;
        this.pacer = pacer;
        this.executor = executor;
    }

    /**
     * Reserva o horário do telefone e agenda a entrega (composing → texto → paused) para o início do composing.
     *
     * @param jitterMs jitter anti-ban já sorteado pelo chamador
     * @param delivery entrega completa; exceções são registradas e não travam o telefone
     */
    public synchronized void dispatch(String phone, long jitterMs, Runnable delivery) {
        long now = clock.millis();
        RecipientPacer.Slot slot = pacer.reserve(phone, now, jitterMs);
        long delayMs = Math.max(0, slot.composingAtMs() - now);

        CompletableFuture<Void> timer = new CompletableFuture<>();
        executor.schedule(() -> timer.complete(null), delayMs, TimeUnit.MILLISECONDS);

        CompletableFuture<Void> previous = tailByPhone.getOrDefault(phone, CompletableFuture.completedFuture(null));
        CompletableFuture<Void> next = CompletableFuture.allOf(previous, timer)
                .thenRunAsync(() -> runSafely(phone, delivery), executor);
        tailByPhone.put(phone, next);
        next.whenComplete((ignored, error) -> tailByPhone.remove(phone, next));

        log.debug("[PerRecipientDispatcher] Envio para [{}] agendado: composing em {} ms, texto em {} ms",
                phone, delayMs, slot.textAtMs() - now);
    }

    public long globalMinIntervalMs() {
        return pacer.globalMinIntervalMs();
    }

    @PreDestroy
    public void shutdown() {
        CompletableFuture<?>[] pending = tailByPhone.values().toArray(CompletableFuture[]::new);
        if (pending.length > 0) {
            try {
                CompletableFuture.allOf(pending).get(SHUTDOWN_DRAIN_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (TimeoutException e) {
                log.warn("[PerRecipientDispatcher] Desligando com {} telefone(s) ainda com envio pendente.",
                        tailByPhone.size());
            } catch (Exception e) {
                log.warn("[PerRecipientDispatcher] Falha ao drenar envios pendentes: {}", e.getMessage());
            }
        }
        executor.shutdown();
    }

    private static void runSafely(String phone, Runnable delivery) {
        try {
            delivery.run();
        } catch (Exception e) {
            log.error("[PerRecipientDispatcher] Falha na entrega para [{}]: {}", phone, e.getMessage(), e);
        }
    }

    private static ScheduledExecutorService newPool() {
        AtomicInteger counter = new AtomicInteger();
        ScheduledThreadPoolExecutor pool = new ScheduledThreadPoolExecutor(POOL_SIZE, runnable -> {
            Thread thread = new Thread(runnable, "outbound-pacer-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
        pool.setRemoveOnCancelPolicy(true);
        return pool;
    }
}
