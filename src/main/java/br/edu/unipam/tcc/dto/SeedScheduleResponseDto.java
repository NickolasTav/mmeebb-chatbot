package br.edu.unipam.tcc.dto;

import java.util.UUID;

public record SeedScheduleResponseDto(
        UUID studentId,
        String phoneNumber,
        String studentName,
        Long subjectId,
        String subjectName,
        int schedulesCreated
) {}
