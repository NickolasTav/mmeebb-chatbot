package br.edu.unipam.tcc.service.impl;

import br.edu.unipam.tcc.dto.FlashcardRequestDto;
import br.edu.unipam.tcc.dto.FlashcardResponseDto;
import br.edu.unipam.tcc.dto.FlashcardStatusDto;
import br.edu.unipam.tcc.entity.Flashcard;
import br.edu.unipam.tcc.entity.Subject;
import br.edu.unipam.tcc.entity.enums.DifficultyLevel;
import br.edu.unipam.tcc.entity.enums.QuestionType;
import br.edu.unipam.tcc.exception.ResourceNotFoundException;
import br.edu.unipam.tcc.repository.FlashcardRepository;
import br.edu.unipam.tcc.repository.SubjectRepository;
import br.edu.unipam.tcc.service.FlashcardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlashcardServiceImpl implements FlashcardService {

    private final FlashcardRepository flashcardRepository;
    private final SubjectRepository subjectRepository;

    @Override
    @Transactional(readOnly = true)
    public List<FlashcardResponseDto> findAll(Long subjectId, Boolean active) {
        List<Flashcard> flashcards;
        if (subjectId != null && active != null) {
            flashcards = flashcardRepository.findBySubjectIdAndActive(subjectId, active);
        } else if (subjectId != null) {
            flashcards = flashcardRepository.findBySubjectId(subjectId);
        } else if (active != null) {
            flashcards = flashcardRepository.findByActive(active);
        } else {
            flashcards = flashcardRepository.findAll();
        }
        return flashcards.stream().map(this::toResponseDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FlashcardResponseDto findById(Long id) {
        Flashcard flashcard = findFlashcardOrThrow(id);
        return toResponseDto(flashcard);
    }

    @Override
    @Transactional
    public FlashcardResponseDto create(FlashcardRequestDto requestDto) {
        log.info("[FlashcardService] Criando novo flashcard para Matéria ID: {}", requestDto.subjectId());
        Subject subject = subjectRepository.findById(requestDto.subjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Disciplina com ID " + requestDto.subjectId() + " não encontrada."));

        Flashcard flashcard = Flashcard.builder()
                .subject(subject)
                .topic(requestDto.topic().trim())
                .questionType(requestDto.questionType() != null ? requestDto.questionType() : QuestionType.FLASHCARD)
                .question(requestDto.question().trim())
                .answer(requestDto.answer().trim())
                .optionsJson(requestDto.optionsJson() != null ? requestDto.optionsJson().trim() : null)
                .explanation(requestDto.explanation() != null ? requestDto.explanation().trim() : null)
                .difficulty(requestDto.difficulty() != null ? requestDto.difficulty() : DifficultyLevel.MEDIUM)
                .source(requestDto.source() != null ? requestDto.source().trim() : null)
                .active(requestDto.active() != null ? requestDto.active() : true)
                .build();

        Flashcard saved = flashcardRepository.save(flashcard);
        log.info("[FlashcardService] Flashcard criado com sucesso! ID: {}", saved.getId());
        return toResponseDto(saved);
    }

    @Override
    @Transactional
    public FlashcardResponseDto update(Long id, FlashcardRequestDto requestDto) {
        log.info("[FlashcardService] Atualizando flashcard ID: {}", id);
        Flashcard flashcard = findFlashcardOrThrow(id);

        Subject subject = subjectRepository.findById(requestDto.subjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Disciplina com ID " + requestDto.subjectId() + " não encontrada."));

        flashcard.setSubject(subject);
        flashcard.setTopic(requestDto.topic().trim());
        if (requestDto.questionType() != null) {
            flashcard.setQuestionType(requestDto.questionType());
        }
        flashcard.setQuestion(requestDto.question().trim());
        flashcard.setAnswer(requestDto.answer().trim());
        flashcard.setOptionsJson(requestDto.optionsJson() != null ? requestDto.optionsJson().trim() : null);
        flashcard.setExplanation(requestDto.explanation() != null ? requestDto.explanation().trim() : null);
        if (requestDto.difficulty() != null) {
            flashcard.setDifficulty(requestDto.difficulty());
        }
        flashcard.setSource(requestDto.source() != null ? requestDto.source().trim() : null);
        if (requestDto.active() != null) {
            flashcard.setActive(requestDto.active());
        }

        Flashcard updated = flashcardRepository.save(flashcard);
        return toResponseDto(updated);
    }

    @Override
    @Transactional
    public FlashcardResponseDto updateStatus(Long id, FlashcardStatusDto statusDto) {
        log.info("[FlashcardService] Atualizando status do flashcard ID: {} para active={}", id, statusDto.active());
        Flashcard flashcard = findFlashcardOrThrow(id);
        flashcard.setActive(statusDto.active());
        Flashcard updated = flashcardRepository.save(flashcard);
        return toResponseDto(updated);
    }

    @Override
    @Transactional
    public FlashcardResponseDto activate(Long id) {
        return updateStatus(id, new FlashcardStatusDto(true));
    }

    @Override
    @Transactional
    public FlashcardResponseDto deactivate(Long id) {
        return updateStatus(id, new FlashcardStatusDto(false));
    }

    private Flashcard findFlashcardOrThrow(Long id) {
        return flashcardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flashcard com ID " + id + " não encontrado."));
    }

    private FlashcardResponseDto toResponseDto(Flashcard flashcard) {
        return new FlashcardResponseDto(
                flashcard.getId(),
                flashcard.getSubject().getId(),
                flashcard.getSubject().getName(),
                flashcard.getTopic(),
                flashcard.getQuestionType(),
                flashcard.getQuestion(),
                flashcard.getAnswer(),
                flashcard.getOptionsJson(),
                flashcard.getExplanation(),
                flashcard.getDifficulty(),
                flashcard.getSource(),
                flashcard.getActive(),
                flashcard.getCreatedAt(),
                flashcard.getUpdatedAt()
        );
    }
}
