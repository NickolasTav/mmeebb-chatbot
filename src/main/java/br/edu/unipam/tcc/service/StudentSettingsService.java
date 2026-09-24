package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.dto.StudentProfileDto;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Preferências que o próprio estudante altera pelo menu de Configurações do WhatsApp.
 */
public interface StudentSettingsService {

    StudentProfileDto getProfile(UUID studentId);

    /**
     * @param preferredName apelido já validado, ou {@code null} para voltar a usar o primeiro nome
     */
    void updatePreferredName(UUID studentId, String preferredName);

    /**
     * @return {@code true} se o lembrete ainda pode sair hoje; {@code false} se só a partir de amanhã
     */
    boolean updateStudyTime(UUID studentId, LocalTime studyTime);

    /**
     * @return novo estado dos lembretes: {@code true} quando ficaram ativos
     */
    boolean toggleReviewNotifications(UUID studentId);

    /**
     * Troca (ou mantém) o curso e atualiza o período, preservando o progresso de cursos anteriores.
     *
     * @throws br.edu.unipam.tcc.exception.ResourceNotFoundException se o estudante não existir ou
     *                                                              o curso não existir/estiver inativo
     */
    void changeEnrollment(UUID studentId, Long courseId, int academicPeriod);
}
