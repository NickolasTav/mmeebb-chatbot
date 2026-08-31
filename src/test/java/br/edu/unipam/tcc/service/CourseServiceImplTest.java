package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.dto.CourseRequestDto;
import br.edu.unipam.tcc.dto.CourseResponseDto;
import br.edu.unipam.tcc.dto.CourseStatusDto;
import br.edu.unipam.tcc.entity.Course;
import br.edu.unipam.tcc.exception.BusinessException;
import br.edu.unipam.tcc.exception.ResourceNotFoundException;
import br.edu.unipam.tcc.repository.CourseRepository;
import br.edu.unipam.tcc.service.impl.CourseServiceImpl;
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
class CourseServiceImplTest {

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseServiceImpl courseService;

    private Course courseMedicina;
    private Course courseSisInfo;

    @BeforeEach
    void setUp() {
        courseMedicina = Course.builder()
                .id(1L)
                .code("MEDICINA")
                .name("Medicina")
                .description("Graduação em Medicina")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        courseSisInfo = Course.builder()
                .id(2L)
                .code("SIS_INFO")
                .name("Sistemas de Informação")
                .description("Bacharelado em Sistemas de Informação")
                .active(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Deve listar todos os cursos sem filtro")
    void shouldFindAllCoursesWithoutFilter() {
        when(courseRepository.findAll()).thenReturn(List.of(courseMedicina, courseSisInfo));

        List<CourseResponseDto> result = courseService.findAll(null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).code()).isEqualTo("MEDICINA");
        assertThat(result.get(1).code()).isEqualTo("SIS_INFO");
        verify(courseRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Deve listar cursos filtrando por status ativo")
    void shouldFindAllCoursesWithActiveFilter() {
        when(courseRepository.findByActive(true)).thenReturn(List.of(courseMedicina));

        List<CourseResponseDto> result = courseService.findAll(true);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("MEDICINA");
        assertThat(result.get(0).active()).isTrue();
        verify(courseRepository, times(1)).findByActive(true);
    }

    @Test
    @DisplayName("Deve buscar curso por ID com sucesso")
    void shouldFindCourseById() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(courseMedicina));

        CourseResponseDto result = courseService.findById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Medicina");
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao buscar ID inexistente")
    void shouldThrowWhenCourseNotFoundById() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Curso com ID 99 não encontrado");
    }

    @Test
    @DisplayName("Deve criar um curso com sucesso")
    void shouldCreateCourseSuccessfully() {
        CourseRequestDto request = new CourseRequestDto("DIREITO", "Direito", "Curso de Direito", true);
        Course savedCourse = Course.builder()
                .id(3L)
                .code("DIREITO")
                .name("Direito")
                .description("Curso de Direito")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(courseRepository.findByCode("DIREITO")).thenReturn(Optional.empty());
        when(courseRepository.save(any(Course.class))).thenReturn(savedCourse);

        CourseResponseDto result = courseService.create(request);

        assertThat(result.id()).isEqualTo(3L);
        assertThat(result.code()).isEqualTo("DIREITO");
        assertThat(result.active()).isTrue();
    }

    @Test
    @DisplayName("Deve lançar BusinessException ao tentar criar curso com código duplicado")
    void shouldThrowWhenCreatingCourseWithDuplicateCode() {
        CourseRequestDto request = new CourseRequestDto("MEDICINA", "Medicina Nova", "Desc", true);
        when(courseRepository.findByCode("MEDICINA")).thenReturn(Optional.of(courseMedicina));

        assertThatThrownBy(() -> courseService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Já existe um curso com o código 'MEDICINA'");
    }

    @Test
    @DisplayName("Deve atualizar dados do curso com sucesso")
    void shouldUpdateCourseSuccessfully() {
        CourseRequestDto request = new CourseRequestDto("MEDICINA_ATUALIZADA", "Medicina UNIPAM", "Nova desc", true);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(courseMedicina));
        when(courseRepository.findByCode("MEDICINA_ATUALIZADA")).thenReturn(Optional.empty());
        when(courseRepository.save(any(Course.class))).thenReturn(courseMedicina);

        CourseResponseDto result = courseService.update(1L, request);

        assertThat(result.code()).isEqualTo("MEDICINA_ATUALIZADA");
        assertThat(result.name()).isEqualTo("Medicina UNIPAM");
    }

    @Test
    @DisplayName("Deve atualizar status do curso com sucesso")
    void shouldUpdateCourseStatus() {
        CourseStatusDto statusDto = new CourseStatusDto(false);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(courseMedicina));
        when(courseRepository.save(any(Course.class))).thenReturn(courseMedicina);

        CourseResponseDto result = courseService.updateStatus(1L, statusDto);

        assertThat(result.active()).isFalse();
    }

    @Test
    @DisplayName("Deve ativar curso explicitamente")
    void shouldActivateCourse() {
        when(courseRepository.findById(2L)).thenReturn(Optional.of(courseSisInfo));
        when(courseRepository.save(any(Course.class))).thenReturn(courseSisInfo);

        CourseResponseDto result = courseService.activate(2L);

        assertThat(result.active()).isTrue();
    }

    @Test
    @DisplayName("Deve desativar curso explicitamente")
    void shouldDeactivateCourse() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(courseMedicina));
        when(courseRepository.save(any(Course.class))).thenReturn(courseMedicina);

        CourseResponseDto result = courseService.deactivate(1L);

        assertThat(result.active()).isFalse();
    }
}
