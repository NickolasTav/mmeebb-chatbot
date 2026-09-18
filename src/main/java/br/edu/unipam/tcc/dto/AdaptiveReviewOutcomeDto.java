package br.edu.unipam.tcc.dto;

import br.edu.unipam.tcc.entity.RepetitionSchedule;
import br.edu.unipam.tcc.entity.enums.TopicMastery;

/**
 * Resultado de uma resposta processada pela camada adaptativa.
 *
 * <p>Os três sinalizadores existem para a mensagem enviada ao estudante: um intervalo que encurta
 * depois de um acerto, ou um erro que não zera o ciclo, precisam ser explicados — sem isso a
 * personalização é lida como defeito.
 */
public record AdaptiveReviewOutcomeDto(
        RepetitionSchedule schedule,
        TopicPerformanceDto performance,
        boolean intervalCapped,
        boolean lapseSoftened,
        int reinforcedCards
) {

    public TopicMastery mastery() {
        return performance.mastery();
    }

    /** Indica se a decisão divergiu do MMEEBB clássico e, portanto, precisa ser explicada. */
    public boolean divergedFromClassic() {
        return intervalCapped || lapseSoftened || reinforcedCards > 0;
    }
}
