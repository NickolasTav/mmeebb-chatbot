package br.edu.unipam.tcc.observability;

import io.micrometer.tracing.Tracer;
import org.slf4j.MDC;

import java.security.SecureRandom;

/**
 * Enriquecimento de contexto MDC para correlacao de logs no Grafana Loki.
 * Garante que campos de negocio (telefone do aluno, acao) e o traceId nunca
 * fiquem nulos, mesmo em threads sem span ativo (ex.: @Scheduled).
 */
public final class CorrelationMdcHelper {

    private static final String TRACE_ID_KEY = "traceId";
    private static final String STUDENT_PHONE_KEY = "studentPhone";
    private static final String ACTION_KEY = "action";
    private static final SecureRandom RANDOM = new SecureRandom();

    private CorrelationMdcHelper() {
    }

    public static void runWithContext(String studentPhone, String action, Runnable task) {
        MDC.put(STUDENT_PHONE_KEY, studentPhone);
        MDC.put(ACTION_KEY, action);
        try {
            task.run();
        } finally {
            MDC.remove(STUDENT_PHONE_KEY);
            MDC.remove(ACTION_KEY);
        }
    }

    /**
     * Threads de @Scheduled nao possuem span ativo do Micrometer Tracing, entao o
     * traceId nunca seria populado automaticamente. Gera um hex de correlacao local
     * apenas para agrupar logs da mesma execucao no Grafana Loki.
     */
    public static void ensureTraceId(Tracer tracer) {
        if (tracer != null && tracer.currentSpan() != null) {
            return;
        }
        byte[] bytes = new byte[8];
        RANDOM.nextBytes(bytes);
        StringBuilder hex = new StringBuilder(16);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        MDC.put(TRACE_ID_KEY, hex.toString());
    }
}
