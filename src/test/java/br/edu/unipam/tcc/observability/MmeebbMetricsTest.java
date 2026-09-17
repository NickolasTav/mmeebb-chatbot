package br.edu.unipam.tcc.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MmeebbMetricsTest {

    private SimpleMeterRegistry registry;
    private MmeebbMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new MmeebbMetrics(registry);
    }

    @Test
    @DisplayName("Deve registrar contador de revisao correta com tag de especialidade")
    void deveRegistrarRevisaoCorreta() {
        metrics.recordReview(true, "Clinica Medica");

        assertEquals(1.0, registry.counter("med_mmeebb_reviews_total",
                "result", "CORRECT", "specialty", "Clinica Medica").count());
    }

    @Test
    @DisplayName("Deve registrar contador de revisao incorreta com especialidade desconhecida quando nula")
    void deveRegistrarRevisaoIncorretaComEspecialidadeDesconhecida() {
        metrics.recordReview(false, null);

        assertEquals(1.0, registry.counter("med_mmeebb_reviews_total",
                "result", "INCORRECT", "specialty", "unknown").count());
    }

    @Test
    @DisplayName("Deve registrar contador de mensagem uazapi por direcao")
    void deveRegistrarMensagemUazapiPorDirecao() {
        metrics.recordUazapiMessage("INBOUND");
        metrics.recordUazapiMessage("OUTBOUND");
        metrics.recordUazapiMessage("OUTBOUND");

        assertEquals(1.0, registry.counter("med_mmeebb_uaizap_messages_total", "direction", "INBOUND").count());
        assertEquals(2.0, registry.counter("med_mmeebb_uaizap_messages_total", "direction", "OUTBOUND").count());
    }

    @Test
    @DisplayName("Deve registrar contador de interacao de IA por origem e caminho")
    void deveRegistrarInteracaoDeIa() {
        metrics.recordAiInteraction("intent_router", "gemini");
        metrics.recordAiInteraction("answer_evaluation", "fast_path");

        assertEquals(1.0, registry.counter("med_mmeebb_ai_interactions_total",
                "source", "intent_router", "path", "gemini").count());
        assertEquals(1.0, registry.counter("med_mmeebb_ai_interactions_total",
                "source", "answer_evaluation", "path", "fast_path").count());
    }
}
