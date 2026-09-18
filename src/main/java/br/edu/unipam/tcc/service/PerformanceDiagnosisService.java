package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.dto.StudentPerformanceReportDto;

import java.util.Optional;

/**
 * Traduz o diagnóstico numérico em uma orientação de estudo em linguagem natural.
 *
 * <p>É aqui que a IA generativa entra na personalização: a decisão de quanto encurtar ou esticar
 * cada intervalo continua determinística, e o modelo cuida do que ele faz melhor — dizer ao
 * estudante, em duas ou três frases, onde concentrar o esforço.
 *
 * <p>O resultado é opcional de propósito: o relatório precisa chegar íntegro ao aluno mesmo sem
 * chave de API, com cota estourada ou sob instabilidade do provedor.
 */
public interface PerformanceDiagnosisService {

    Optional<String> diagnose(StudentPerformanceReportDto report);
}
