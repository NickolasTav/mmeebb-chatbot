package br.edu.unipam.tcc.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Metricas de dominio do MMEEBB expostas via Actuator/Prometheus, usadas para
 * evidencia cientifica no TCC e para o dashboard Grafana de acompanhamento do piloto.
 */
@Component
public class MmeebbMetrics {

    private final MeterRegistry registry;

    public MmeebbMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordReview(boolean correct, String specialty) {
        Counter.builder("med_mmeebb_reviews_total")
                .tag("result", correct ? "CORRECT" : "INCORRECT")
                .tag("specialty", specialty == null ? "unknown" : specialty)
                .register(registry)
                .increment();
    }

    public void recordUazapiMessage(String direction) {
        Counter.builder("med_mmeebb_uaizap_messages_total")
                .tag("direction", direction)
                .register(registry)
                .increment();
    }

    public void recordAiInteraction(String source, String path) {
        Counter.builder("med_mmeebb_ai_interactions_total")
                .tag("source", source)
                .tag("path", path)
                .register(registry)
                .increment();
    }
}
