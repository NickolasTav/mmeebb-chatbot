package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.dto.FlashcardRequestDto;
import br.edu.unipam.tcc.dto.FlashcardResponseDto;
import br.edu.unipam.tcc.dto.FlashcardStatusDto;
import br.edu.unipam.tcc.entity.Course;
import br.edu.unipam.tcc.entity.Flashcard;
import br.edu.unipam.tcc.entity.Subject;
import br.edu.unipam.tcc.entity.enums.DifficultyLevel;
import br.edu.unipam.tcc.entity.enums.QuestionType;
import br.edu.unipam.tcc.exception.ResourceNotFoundException;
import br.edu.unipam.tcc.repository.FlashcardRepository;
import br.edu.unipam.tcc.repository.SubjectRepository;
import br.edu.unipam.tcc.service.impl.FlashcardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlashcardServiceImplTest {

    @Mock
    private FlashcardRepository flashcardRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @InjectMocks
    private FlashcardServiceImpl flashcardService;

    private Subject subject;
    private Flashcard flashcard;

    @BeforeEach
    void setUp() {
        Course course = Course.builder().id(1L).name("Medicina").code("MED").build();
        subject = Subject.builder().id(10L).course(course).name("Clínica Médica").code("CLIN_MED").build();

        flashcard = Flashcard.builder()
                .id(100L)
                .subject(subject)
                .topic("Cardiologia")
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .question("Qual o tratamento de primeira linha para IC com fração de ejeção reduzida?")
                .answer("A")
                .optionsJson("[\"A) IECA/BRA + Betabloqueador + Antagonista Mineralocorticoide + iSGLT2\", \"B) Apenas Digoxina\"]")
                .explanation("O tratamento padrão ouro envolve a terapia quádrupla.")
                .difficulty(DifficultyLevel.HARD)
                .source("Diretriz SBC 2023")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Deve listar flashcards sem filtros")
    void shouldFindAllWithoutFilters() {
        when(flashcardRepository.findAll()).thenReturn(List.of(flashcard));

        List<FlashcardResponseDto> result = flashcardService.findAll(null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).topic()).isEqualTo("Cardiologia");
        assertThat(result.get(0).subjectName()).isEqualTo("Clínica Médica");
    }

    @Test
    @DisplayName("Deve listar flashcards por disciplina e status ativo")
    void shouldFindBySubjectIdAndActive() {
        when(flashcardRepository.findBySubjectIdAndActive(10L, true)).thenReturn(List.of(flashcard));

        List<FlashcardResponseDto> result = flashcardService.findAll(10L, true);

        assertThat(result).hasSize(1);
        verify(flashcardRepository, times(1)).findBySubjectIdAndActive(10L, true);
    }

    @Test
    @DisplayName("Deve buscar flashcard por ID com sucesso")
    void shouldFindById() {
        when(flashcardRepository.findById(100L)).thenReturn(Optional.of(flashcard));

        FlashcardResponseDto result = flashcardService.findById(100L);

        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.answer()).isEqualTo("A");
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException para flashcard inexistente")
    void shouldThrowWhenFlashcardNotFound() {
        when(flashcardRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> flashcardService.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Flashcard com ID 999 não encontrado");
    }

    @Test
    @DisplayName("Deve criar flashcard com sucesso")
    void shouldCreateFlashcardSuccessfully() {
        FlashcardRequestDto request = new FlashcardRequestDto(
                10L,
                "Cardiologia",
                QuestionType.FLASHCARD,
                "Qual a meta pressórica na HAS estágio 1?",
                "< 130/80 mmHg",
                null,
                "Segundo diretriz brasileira",
                DifficultyLevel.MEDIUM,
                "SBC",
                true
        );

        when(subjectRepository.findById(10L)).thenReturn(Optional.of(subject));
        when(flashcardRepository.save(any(Flashcard.class))).thenReturn(flashcard);

        FlashcardResponseDto result = flashcardService.create(request);

        assertThat(result).isNotNull();
        verify(flashcardRepository, times(1)).save(any(Flashcard.class));
    }

    @Test
    @DisplayName("Deve atualizar status do flashcard com sucesso")
    void shouldUpdateStatusSuccessfully() {
        FlashcardStatusDto statusDto = new FlashcardStatusDto(false);
        when(flashcardRepository.findById(100L)).thenReturn(Optional.of(flashcard));
        when(flashcardRepository.save(any(Flashcard.class))).thenReturn(flashcard);

        FlashcardResponseDto result = flashcardService.updateStatus(100L, statusDto);

        assertThat(result.active()).isFalse();
    }

    @Test
    @DisplayName("Deve ativar flashcard com sucesso")
    void shouldActivateFlashcard() {
        when(flashcardRepository.findById(100L)).thenReturn(Optional.of(flashcard));
        when(flashcardRepository.save(any(Flashcard.class))).thenReturn(flashcard);

        FlashcardResponseDto result = flashcardService.activate(100L);

        assertThat(result.active()).isTrue();
    }

    @Test
    @DisplayName("Deve desativar flashcard com sucesso")
    void shouldDeactivateFlashcard() {
        when(flashcardRepository.findById(100L)).thenReturn(Optional.of(flashcard));
        when(flashcardRepository.save(any(Flashcard.class))).thenReturn(flashcard);

        FlashcardResponseDto result = flashcardService.deactivate(100L);

        assertThat(result.active()).isFalse();
    }
}
