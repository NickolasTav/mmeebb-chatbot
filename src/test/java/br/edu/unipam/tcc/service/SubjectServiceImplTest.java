package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.dto.SubjectRequestDto;
import br.edu.unipam.tcc.dto.SubjectResponseDto;
import br.edu.unipam.tcc.dto.SubjectStatusDto;
import br.edu.unipam.tcc.entity.Course;
import br.edu.unipam.tcc.entity.Subject;
import br.edu.unipam.tcc.exception.BusinessException;
import br.edu.unipam.tcc.exception.ResourceNotFoundException;
import br.edu.unipam.tcc.repository.CourseRepository;
import br.edu.unipam.tcc.repository.SubjectRepository;
import br.edu.unipam.tcc.service.impl.SubjectServiceImpl;
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
class SubjectServiceImplTest {

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private SubjectServiceImpl subjectService;

    private Course course;
    private Subject subjectClinMed;

    @BeforeEach
    void setUp() {
        course = Course.builder()
                .id(1L)
                .code("MEDICINA")
                .name("Medicina")
                .active(true)
                .build();

        subjectClinMed = Subject.builder()
                .id(10L)
                .course(course)
                .code("CLIN_MED")
                .name("Clínica Médica")
                .description("Disciplina de Clínica Médica")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Deve listar matérias sem filtros")
    void shouldFindAllWithoutFilters() {
        when(subjectRepository.findAll()).thenReturn(List.of(subjectClinMed));

        List<SubjectResponseDto> result = subjectService.findAll(null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("CLIN_MED");
        assertThat(result.get(0).courseName()).isEqualTo("Medicina");
    }

    @Test
    @DisplayName("Deve listar matérias por curso e status ativo")
    void shouldFindByCourseIdAndActive() {
        when(subjectRepository.findByCourseIdAndActive(1L, true)).thenReturn(List.of(subjectClinMed));

        List<SubjectResponseDto> result = subjectService.findAll(1L, true);

        assertThat(result).hasSize(1);
        verify(subjectRepository, times(1)).findByCourseIdAndActive(1L, true);
    }

    @Test
    @DisplayName("Deve buscar matéria por ID com sucesso")
    void shouldFindById() {
        when(subjectRepository.findById(10L)).thenReturn(Optional.of(subjectClinMed));

        SubjectResponseDto result = subjectService.findById(10L);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.name()).isEqualTo("Clínica Médica");
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao buscar matéria com ID inexistente")
    void shouldThrowWhenSubjectNotFound() {
        when(subjectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subjectService.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Disciplina com ID 999 não encontrada");
    }

    @Test
    @DisplayName("Deve criar matéria com sucesso")
    void shouldCreateSubjectSuccessfully() {
        SubjectRequestDto request = new SubjectRequestDto(1L, "CIR_GERAL", "Cirurgia Geral", "Desc", true);
        Subject saved = Subject.builder()
                .id(11L)
                .course(course)
                .code("CIR_GERAL")
                .name("Cirurgia Geral")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(subjectRepository.existsByCourseIdAndCode(1L, "CIR_GERAL")).thenReturn(false);
        when(subjectRepository.save(any(Subject.class))).thenReturn(saved);

        SubjectResponseDto result = subjectService.create(request);

        assertThat(result.id()).isEqualTo(11L);
        assertThat(result.code()).isEqualTo("CIR_GERAL");
    }

    @Test
    @DisplayName("Deve lançar BusinessException ao tentar criar matéria com código duplicado no mesmo curso")
    void shouldThrowWhenCodeExistsInCourse() {
        SubjectRequestDto request = new SubjectRequestDto(1L, "CLIN_MED", "Clínica Médica 2", "Desc", true);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(subjectRepository.existsByCourseIdAndCode(1L, "CLIN_MED")).thenReturn(true);

        assertThatThrownBy(() -> subjectService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Já existe uma matéria com o código 'CLIN_MED'");
    }

    @Test
    @DisplayName("Deve atualizar status da matéria com sucesso")
    void shouldUpdateStatusSuccessfully() {
        SubjectStatusDto statusDto = new SubjectStatusDto(false);
        when(subjectRepository.findById(10L)).thenReturn(Optional.of(subjectClinMed));
        when(subjectRepository.save(any(Subject.class))).thenReturn(subjectClinMed);

        SubjectResponseDto result = subjectService.updateStatus(10L, statusDto);

        assertThat(result.active()).isFalse();
    }
}
