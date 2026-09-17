package br.edu.unipam.tcc.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Parâmetros de calibração da personalização adaptativa.
 *
 * <p>Os limiares ficam aqui, e não no código, porque são dados de calibração do piloto: durante a
 * validação experimental o pesquisador ajusta as faixas sem recompilar. Já os tetos de intervalo
 * pertencem ao próprio algoritmo e vivem em {@code TopicMastery}.
 *
 * <p>{@code enabled=false} devolve o sistema ao MMEEBB clássico — serve de contingência e permite
 * comparar as duas versões do método na monografia.
 */
@Getter
@Component
public class AdaptiveProperties {

    private final boolean enabled;
    private final int windowDays;
    private final int minAttempts;
    private final double fragileErrorRate;
    private final double consolidatingErrorRate;
    private final int reinforcementMaxCards;
    private final int reportMinAttempts;

    public AdaptiveProperties(
            @Value("${mmeebb.adaptive.enabled:true}") boolean enabled,
            @Value("${mmeebb.adaptive.window-days:90}") int windowDays,
            @Value("${mmeebb.adaptive.min-attempts:3}") int minAttempts,
            @Value("${mmeebb.adaptive.fragile-error-rate:0.50}") double fragileErrorRate,
            @Value("${mmeebb.adaptive.consolidating-error-rate:0.20}") double consolidatingErrorRate,
            @Value("${mmeebb.adaptive.reinforcement-max-cards:2}") int reinforcementMaxCards,
            @Value("${mmeebb.adaptive.report-min-attempts:5}") int reportMinAttempts
    ) {
        this.enabled = enabled;
        this.windowDays = windowDays;
        this.minAttempts = minAttempts;
        this.fragileErrorRate = fragileErrorRate;
        this.consolidatingErrorRate = consolidatingErrorRate;
        this.reinforcementMaxCards = reinforcementMaxCards;
        this.reportMinAttempts = reportMinAttempts;
    }
}
