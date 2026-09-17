package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.entity.Student;

/**
 * Cadastro do estudante no primeiro contato pelo WhatsApp.
 */
public interface StudentOnboardingService {

    /**
     * Cria o estudante, vincula-o ao curso escolhido e inicializa os agendamentos
     * MMEEBB de todos os flashcards ativos das disciplinas desse curso.
     *
     * @param phoneNumber    Número de WhatsApp (somente dígitos), chave única do estudante.
     * @param fullName       Nome completo informado no formulário.
     * @param ra             Registro acadêmico informado, ou {@code null} se dispensado.
     * @param courseId       Curso escolhido.
     * @param academicPeriod Período letivo informado.
     * @return Estudante cadastrado.
     */
    Student register(String phoneNumber, String fullName, String ra, Long courseId, Integer academicPeriod);
}
