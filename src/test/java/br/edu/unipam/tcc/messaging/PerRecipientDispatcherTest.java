package br.edu.unipam.tcc.messaging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Agendador do modo per-recipient com pool real de 4 threads e atrasos curtos (0–200 ms).
 * O relógio é fixo: o atraso de cada entrega é exatamente o que o pacer calculou.
 */
class PerRecipientDispatcherTest {

    private final Clock relogio = Clock.fixed(Instant.parse("2026-10-11T12:00:00Z"), ZoneOffset.UTC);
    private final ScheduledExecutorService pool = Executors.newScheduledThreadPool(4);

    @AfterEach
    void tearDown() {
        pool.shutdownNow();
    }

    private PerRecipientDispatcher semEsperas() {
        return new PerRecipientDispatcher(relogio, new RecipientPacer(0, 0), pool);
    }

    @Test
    @DisplayName("Mesmo telefone: entregas com o mesmo horário saem na ordem de chegada, mesmo com 4 threads")
    void mesmoTelefoneMantemOrdem() throws InterruptedException {
        PerRecipientDispatcher dispatcher = semEsperas();
        List<Integer> entregues = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch fim = new CountDownLatch(50);

        for (int i = 0; i < 50; i++) {
            int ordem = i;
            dispatcher.dispatch("5534000000001", 0, () -> {
                dormir(ThreadLocalRandom.current().nextInt(0, 4));
                entregues.add(ordem);
                fim.countDown();
            });
        }

        assertTrue(fim.await(10, TimeUnit.SECONDS));
        assertEquals(IntStream.range(0, 50).boxed().toList(), entregues);
    }

    @Test
    @DisplayName("Telefones diferentes: a entrega de B não espera a entrega de A terminar")
    void telefonesDiferentesNaoSeBloqueiam() throws InterruptedException {
        PerRecipientDispatcher dispatcher = semEsperas();
        CountDownLatch liberaA = new CountDownLatch(1);
        CountDownLatch bEntregue = new CountDownLatch(1);

        dispatcher.dispatch("A", 0, () -> aguardar(liberaA));
        dispatcher.dispatch("B", 0, bEntregue::countDown);

        boolean bSaiuComAPreso = bEntregue.await(2, TimeUnit.SECONDS);
        liberaA.countDown();
        assertTrue(bSaiuComAPreso);
    }

    @Test
    @DisplayName("Exceção numa entrega não trava as próximas do mesmo telefone")
    void excecaoNaoTravaOTelefone() throws InterruptedException {
        PerRecipientDispatcher dispatcher = semEsperas();
        CountDownLatch segunda = new CountDownLatch(1);

        dispatcher.dispatch("A", 0, () -> {
            throw new IllegalStateException("Uazapi fora do ar");
        });
        dispatcher.dispatch("A", 0, segunda::countDown);

        assertTrue(segunda.await(2, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("A entrega não sai antes do horário calculado pelo pacer")
    void respeitaOHorarioDoPacer() throws InterruptedException {
        PerRecipientDispatcher dispatcher = semEsperas();
        CountDownLatch entregue = new CountDownLatch(1);
        long[] decorridoMs = new long[1];
        long inicio = System.nanoTime();

        dispatcher.dispatch("A", 150, () -> {
            decorridoMs[0] = (System.nanoTime() - inicio) / 1_000_000;
            entregue.countDown();
        });

        assertTrue(entregue.await(2, TimeUnit.SECONDS));
        assertTrue(decorridoMs[0] >= 145, "saiu em " + decorridoMs[0] + " ms");
    }

    @Test
    @DisplayName("Desligamento entrega o que já estava agendado antes de fechar o pool")
    void desligamentoDrenaPendentes() {
        PerRecipientDispatcher dispatcher = semEsperas();
        List<String> entregues = Collections.synchronizedList(new ArrayList<>());

        dispatcher.dispatch("A", 200, () -> entregues.add("A"));
        dispatcher.dispatch("B", 100, () -> entregues.add("B"));
        assertFalse(entregues.contains("A"));

        dispatcher.shutdown();

        assertEquals(List.of("B", "A"), entregues);
        assertTrue(pool.isShutdown());
    }

    private static void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void aguardar(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
