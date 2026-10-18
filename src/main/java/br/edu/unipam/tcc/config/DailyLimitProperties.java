package br.edu.unipam.tcc.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Limite diário de questões: o padrão global, a faixa que o estudante pode escolher nas
 * Configurações e a parcela do limite reservada para questões novas.
 *
 * <p>Fica fora do código porque é calibração do piloto, como os limiares adaptativos. A
 * consistência é verificada na subida: um padrão fora da faixa faria o menu recusar justamente o
 * valor que o próprio sistema aplica.
 */
@Getter
@Component
public class DailyLimitProperties {

    private final int defaultLimit;
    private final int min;
    private final int max;
    private final double newShare;

    public DailyLimitProperties(
            @Value("${mmeebb.daily-limit.default:20}") int defaultLimit,
            @Value("${mmeebb.daily-limit.min:5}") int min,
            @Value("${mmeebb.daily-limit.max:100}") int max,
            @Value("${mmeebb.daily-limit.new-share:0.25}") double newShare
    ) {
        if (min < 1 || min > max || defaultLimit < min || defaultLimit > max) {
            throw new IllegalStateException(String.format(
                    "Limite diário inválido: padrão=%d, mín=%d, máx=%d (exige 1 <= mín <= padrão <= máx).",
                    defaultLimit, min, max));
        }
        if (newShare < 0 || newShare > 1) {
            throw new IllegalStateException(String.format(
                    "Parcela de questões novas inválida: %s (exige 0 a 1).", newShare));
        }
        this.defaultLimit = defaultLimit;
        this.min = min;
        this.max = max;
        this.newShare = newShare;
    }

    /** O limite do estudante, ou o padrão global quando ele não escolheu nenhum. */
    public int resolve(Integer studentLimit) {
        return studentLimit != null ? studentLimit : defaultLimit;
    }

    public boolean accepts(int limit) {
        return limit >= min && limit <= max;
    }

    /**
     * Vagas do dia guardadas para questões novas. Sem a reserva, as revisões vencidas ocupam o
     * limite inteiro a partir do terceiro dia e o banco deixa de avançar por semanas. Com parcela
     * positiva, ao menos uma vaga é garantida mesmo nos limites pequenos; zero desliga a reserva.
     */
    public int reservedNewQuestions(int limit) {
        if (newShare <= 0) {
            return 0;
        }
        return Math.min(limit, Math.max(1, (int) Math.floor(limit * newShare)));
    }
}
