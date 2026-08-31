package br.edu.unipam.tcc.dto;

import java.time.LocalDateTime;

public record CourseResponseDto(
        Long id,
        String code,
        String name,
        String description,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
