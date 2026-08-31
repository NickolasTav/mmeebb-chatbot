package br.edu.unipam.tcc.dto;

import br.edu.unipam.tcc.entity.enums.DifficultyLevel;
import br.edu.unipam.tcc.entity.enums.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FlashcardRequestDto(
        @NotNull(message = "O ID da matéria/disciplina é obrigatório.")
        Long subjectId,

        @NotBlank(message = "O tópico é obrigatório.")
        @Size(max = 150, message = "O tópico deve ter no máximo 150 caracteres.")
        String topic,

        QuestionType questionType,

        @NotBlank(message = "A pergunta/enunciado é obrigatória.")
        String question,

        @NotBlank(message = "A resposta correta é obrigatória.")
        String answer,

        String optionsJson,

        String explanation,

        DifficultyLevel difficulty,

        String source,

        Boolean active
) {}
