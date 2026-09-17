package br.edu.unipam.tcc.dto;

import java.time.LocalTime;

/**
 * Retrato das preferências e da matrícula do estudante, exibido no menu de Configurações.
 */
public record StudentProfileDto(
        String fullName,
        String preferredName,
        String displayName,
        String ra,
        Long courseId,
        String courseName,
        Integer academicPeriod,
        LocalTime preferredStudyTime,
        boolean reviewNotificationsEnabled
) {
}
