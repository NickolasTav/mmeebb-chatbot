package br.edu.unipam.tcc.dto;

import br.edu.unipam.tcc.entity.enums.DifficultyLevel;
import br.edu.unipam.tcc.entity.enums.QuestionType;

import java.time.LocalDateTime;

public record FlashcardResponseDto(
        Long id,
        Long subjectId,
        String subjectName,
        String topic,
        QuestionType questionType,
        String question,
        String answer,
        String optionsJson,
        String explanation,
        DifficultyLevel difficulty,
        String source,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
