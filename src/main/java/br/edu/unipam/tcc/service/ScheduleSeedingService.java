package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.entity.Course;
import br.edu.unipam.tcc.entity.Student;

/**
 * Inicialização dos agendamentos MMEEBB de um curso para um estudante.
 */
public interface ScheduleSeedingService {

    /**
     * Cria agendamentos, liberados para revisão já hoje, para os flashcards ativos do curso
     * que o estudante ainda não possui. Agendamentos existentes — e o progresso de repetição
     * espaçada acumulado neles — nunca são alterados, o que permite voltar a um curso anterior
     * sem perder o histórico.
     *
     * @return quantidade de agendamentos criados
     */
    int seedMissingSchedules(Student student, Course course);
}
