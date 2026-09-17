package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.dto.AttemptTotalsDto;
import br.edu.unipam.tcc.dto.TopicPerformanceDto;

import java.util.List;
import java.util.UUID;

/**
 * Traduz o histórico de tentativas em um diagnóstico de domínio por tópico.
 *
 * <p>A classificação é inteiramente determinística: os mesmos dados produzem sempre o mesmo
 * resultado, o que torna cada decisão do algoritmo reproduzível e defensável diante da banca.
 * A IA generativa atua depois, explicando o diagnóstico e reforçando o conteúdo — nunca decidindo
 * qual é o nível de domínio.
 */
public interface PerformanceAnalysisService {

    /**
     * Desempenho de todos os tópicos com histórico do estudante na janela, ordenado do mais
     * frágil ao mais dominado.
     */
    List<TopicPerformanceDto> analyzeTopics(UUID studentId);

    /**
     * Desempenho de um tópico específico. Devolve {@code SEM_DADOS} quando não há amostra
     * suficiente, o que faz o motor MMEEBB se comportar exatamente como na versão clássica.
     */
    TopicPerformanceDto analyzeTopic(UUID studentId, Long subjectId, String topic);

    /** Totais de tentativas e acertos do estudante na janela de análise. */
    AttemptTotalsDto totals(UUID studentId);

    /** Quantidade de dias considerada na análise, exibida ao estudante junto dos percentuais. */
    int windowDays();
}
