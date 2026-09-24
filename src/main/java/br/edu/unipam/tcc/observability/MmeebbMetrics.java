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

    /**
     * Quantas revisoes foram decididas em cada nivel de dominio e com qual acao adaptativa
     * (teto aplicado, lapso amortecido ou comportamento classico). Permite medir, no piloto,
     * o quanto a personalizacao realmente altera o MMEEBB original.
     */
    public void recordAdaptiveDecision(String mastery, String action) {
        Counter.builder("med_mmeebb_adaptive_decisions_total")
                .tag("mastery", mastery == null ? "unknown" : mastery)
                .tag("action", action)
                .register(registry)
                .increment();
    }

    /** Questoes antecipadas como reforco dirigido apos erro em topico fragil. */
    public void recordAdaptiveReinforcement(int cards) {
        Counter.builder("med_mmeebb_adaptive_reinforcements_total")
                .register(registry)
                .increment(cards);
    }
}
