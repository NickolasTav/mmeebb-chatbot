package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.entity.Course;
import br.edu.unipam.tcc.entity.Student;
import br.edu.unipam.tcc.entity.StudentCourse;
import br.edu.unipam.tcc.repository.CourseRepository;
import br.edu.unipam.tcc.repository.StudentCourseRepository;
import br.edu.unipam.tcc.repository.StudentRepository;
import br.edu.unipam.tcc.service.impl.StudentOnboardingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentOnboardingServiceImplTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 17);

    @Mock private StudentRepository studentRepository;
    @Mock private StudentCourseRepository studentCourseRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private ScheduleSeedingService scheduleSeedingService;

    private StudentOnboardingServiceImpl service;
    private Course course;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(LocalDateTime.of(TODAY, LocalTime.of(10, 0)).atZone(ZONE).toInstant(), ZONE);
        service = new StudentOnboardingServiceImpl(
                studentRepository, studentCourseRepository, courseRepository, scheduleSeedingService, clock);
        course = Course.builder().id(1L).code("MED").name("Medicina").build();

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(studentRepository.findByPhoneNumber("5534999998888")).thenReturn(Optional.empty());
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(studentCourseRepository.findByStudentIdAndCourseId(any(), any())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("Deve marcar o dia do cadastro como avaliado para não disparar lembrete logo em seguida")
    void deveMarcarDiaDoCadastroComoAvaliado() {
        Student student = service.register("5534999998888", "Maria Silva", null, 1L, 8);

        assertEquals(TODAY, student.getLastReviewNotificationOn());
    }

    @Test
    @DisplayName("Deve vincular o curso com o período e delegar a semeadura dos agendamentos")
    void deveVincularCursoEDelegarSemeadura() {
        Student student = service.register("5534999998888", "Maria Silva", "123", 1L, 8);

        ArgumentCaptor<StudentCourse> link = ArgumentCaptor.forClass(StudentCourse.class);
        verify(studentCourseRepository).save(link.capture());
        assertEquals(course, link.getValue().getCourse());
        assertEquals(8, link.getValue().getAcademicPeriod());
        verify(scheduleSeedingService).seedMissingSchedules(student, course);
    }
}
