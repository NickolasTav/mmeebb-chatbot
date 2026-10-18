package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.dto.DailyReviewPlanDto;
import br.edu.unipam.tcc.entity.Student;

/**
 * Limite diário de questões. A semeadura deixa todo o banco do curso vencido no primeiro dia; é aqui
 * que essa pendência vira uma meta diária do tamanho escolhido pelo estudante.
 *
 * <p>O limite só decide <em>quais</em> questões vencidas chegam hoje. O intervalo 2^n e a camada
 * adaptativa não mudam: o próximo reforço continua contado a partir do dia em que o estudante
 * responde.
 */
public interface DailyReviewQueueService {

    /**
     * Monta a fila do dia: primeiro as revisões vencidas, na ordem da camada adaptativa (tópicos
     * frágeis, depois as mais atrasadas), e depois as questões novas, até completar o limite menos
     * o que já foi respondido hoje. Uma parcela do limite fica reservada para as novas, para o banco
     * continuar avançando mesmo quando as revisões vencidas bastariam para encher o dia.
     */
    DailyReviewPlanDto planToday(Student student);

    /**
     * As mesmas contagens de {@link #planToday}, sem carregar nem priorizar a fila — para o lembrete
     * diário, que só precisa anunciar os números.
     */
    DailyReviewPlanDto summarizeToday(Student student);
}
