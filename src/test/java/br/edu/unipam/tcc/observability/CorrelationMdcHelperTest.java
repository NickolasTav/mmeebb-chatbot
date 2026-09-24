package br.edu.unipam.tcc.observability;

import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CorrelationMdcHelperTest {

    @Mock
    private Tracer tracer;

    @AfterEach
    void limparMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("Deve popular MDC com telefone e acao durante a execucao e limpar ao final")
    void deveAplicarELimparContextoMdc() {
        CorrelationMdcHelper.runWithContext("5534999998888", "INBOUND", () -> {
            assertEquals("5534999998888", MDC.get("studentPhone"));
            assertEquals("INBOUND", MDC.get("action"));
        });

        assertNull(MDC.get("studentPhone"));
        assertNull(MDC.get("action"));
    }

    @Test
    @DisplayName("Deve limpar o MDC mesmo quando a tarefa lanca excecao")
    void deveLimparMdcQuandoTarefaLancaExcecao() {
        assertThrows(RuntimeException.class, () ->
                CorrelationMdcHelper.runWithContext("5534999997777", "OUTBOUND", () -> {
                    throw new RuntimeException("falha simulada");
                }));

        assertNull(MDC.get("studentPhone"));
        assertNull(MDC.get("action"));
    }

    @Test
    @DisplayName("Deve gerar traceId de fallback quando nao ha span ativo (ex.: thread de scheduler)")
    void deveGerarTraceIdFallbackQuandoNaoHaSpanAtivo() {
        when(tracer.currentSpan()).thenReturn(null);

        CorrelationMdcHelper.ensureTraceId(tracer);

        String traceId = MDC.get("traceId");
        assertNotNull(traceId);
        assertFalse(traceId.isBlank());
    }
}
