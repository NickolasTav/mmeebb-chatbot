package br.edu.unipam.tcc.dto;

import java.time.LocalDateTime;

public record SubjectResponseDto(
        Long id,
        Long courseId,
        String courseName,
        String code,
        String name,
        String description,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
