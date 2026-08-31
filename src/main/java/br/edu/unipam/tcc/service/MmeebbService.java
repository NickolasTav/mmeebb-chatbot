package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.entity.Flashcard;
import br.edu.unipam.tcc.entity.RepetitionSchedule;
import br.edu.unipam.tcc.entity.Student;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface MmeebbService {

    /**
     * Limite superior do expoente N no algoritmo MMEEBB (2^13 = 8192 dias).
     */
    int MAX_N_INDEX = 13;

    /**
     * Calcula o Intervalo de Reforço de Aprendizado (IRA) em dias: IRA = 2^N.
     *
     * @param nIndex Expoente N (0 a 13)
     * @return Intervalo em dias
     * @throws IllegalArgumentException se nIndex < 0
     */
    int calculateIntervalDays(int nIndex);

    /**
     * Calcula a data da próxima revisão com base na data de referência e no índice N.
     *
     * @param baseDate Data de referência
     * @param nIndex   Expoente N (0 a 13)
     * @return Data calculada da próxima revisão
     */
    LocalDate calculateNextReviewDate(LocalDate baseDate, int nIndex);

    /**
     * Inicializa um novo agendamento de repetição espaçada com N=0 (IRA=1 dia).
     *
     * @param student   Estudante
     * @param flashcard Flashcard
     * @param startDate Data de início
     * @return Novo RepetitionSchedule pronto para persistência
     */
    RepetitionSchedule initializeSchedule(Student student, Flashcard flashcard, LocalDate startDate);

    /**
     * Processa a resposta do aluno para um agendamento existente:
     * - Se acertou: incrementa N (até o teto de 13), calcula novo intervalo 2^N e projeta próxima data.
     * - Se errou: reseta N para 0, define intervalo como 1 dia (2^0) para o dia seguinte e zera acertos consecutivos.
     *
     * @param schedule   Agendamento atual
     * @param isCorrect  Indica se o aluno acertou/lembrou
     * @param answeredAt Timestamp da resposta
     * @return Agendamento atualizado
     */
    RepetitionSchedule processAnswer(RepetitionSchedule schedule, boolean isCorrect, LocalDateTime answeredAt);
}
