package br.edu.unipam.tcc.messaging;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

/**
 * Calcula quando cada mensagem de saída deve ser enviada no modo {@link AntiBanMode#PER_RECIPIENT}.
 * Puro (sem relógio nem sorteio próprios): o chamador informa o instante atual e o jitter sorteado.
 *
 * <p>Regras:
 * <ol>
 *   <li><b>Cadência por destinatário igual à de hoje:</b> o texto sai em
 *       {@code max(agora, últimoTexto[telefone]) + jitter + composing}; o composing começa
 *       {@code composing} ms antes do texto.</li>
 *   <li><b>Teto global como lacuna:</b> dois textos quaisquer ficam a pelo menos
 *       {@code globalMinIntervalMs} um do outro. O texto vai para o primeiro horário, a partir do desejado,
 *       que respeite essa distância de <i>todos</i> os textos já reservados. Não é uma fila: um aluno com
 *       várias mensagens agendadas não empurra os outros para trás dele.</li>
 * </ol>
 * Memória limitada: textos já passados são descartados a cada reserva.
 */
public class RecipientPacer {

    /** Horários (epoch ms) do início do composing e do envio do texto. */
    public record Slot(long composingAtMs, long textAtMs) {
    }

    private final long composingDelayMs;
    private final long globalMinIntervalMs;
    private final Map<String, Long> lastTextAtByPhone = new HashMap<>();
    private final TreeSet<Long> reservedTextTimes = new TreeSet<>();

    public RecipientPacer(long composingDelayMs, long globalMinIntervalMs) {
        this.composingDelayMs = Math.max(0, composingDelayMs);
        this.globalMinIntervalMs = Math.max(0, globalMinIntervalMs);
    }

    public long globalMinIntervalMs() {
        return globalMinIntervalMs;
    }

    public synchronized Slot reserve(String phone, long nowMs, long jitterMs) {
        forgetPast(nowMs);

        long base = Math.max(nowMs, lastTextAtByPhone.getOrDefault(phone, nowMs));
        long textAt = firstFreeSlotFrom(base + Math.max(0, jitterMs) + composingDelayMs);

        lastTextAtByPhone.put(phone, textAt);
        if (globalMinIntervalMs > 0) {
            reservedTextTimes.add(textAt);
        }
        return new Slot(textAt - composingDelayMs, textAt);
    }

    private long firstFreeSlotFrom(long desired) {
        if (globalMinIntervalMs <= 0) {
            return desired;
        }
        long candidate = desired;
        // Textos reservados estão a >= teto entre si; basta varrer os vizinhos a partir de (candidato - teto).
        for (long reserved : reservedTextTimes.tailSet(candidate - globalMinIntervalMs, false)) {
            if (reserved >= candidate + globalMinIntervalMs) {
                break;
            }
            candidate = reserved + globalMinIntervalMs;
        }
        return candidate;
    }

    private void forgetPast(long nowMs) {
        lastTextAtByPhone.values().removeIf(textAt -> textAt <= nowMs);
        reservedTextTimes.headSet(nowMs - globalMinIntervalMs, true).clear();
    }
}
