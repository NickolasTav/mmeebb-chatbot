package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.dto.SeedScheduleRequestDto;
import br.edu.unipam.tcc.dto.SeedScheduleResponseDto;
import br.edu.unipam.tcc.entity.Course;
import br.edu.unipam.tcc.entity.Flashcard;
import br.edu.unipam.tcc.entity.RepetitionSchedule;
import br.edu.unipam.tcc.entity.Student;
import br.edu.unipam.tcc.entity.Subject;
import br.edu.unipam.tcc.exception.ResourceNotFoundException;
import br.edu.unipam.tcc.repository.FlashcardRepository;
import br.edu.unipam.tcc.repository.RepetitionScheduleRepository;
import br.edu.unipam.tcc.repository.StudentRepository;
import br.edu.unipam.tcc.repository.SubjectRepository;
import br.edu.unipam.tcc.service.impl.StudentAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentAdminServiceImplTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private FlashcardRepository flashcardRepository;

    @Mock
    private RepetitionScheduleRepository repetitionScheduleRepository;

    @Mock
    private MmeebbService mmeebbService;

    @InjectMocks
    private StudentAdminServiceImpl studentAdminService;

    private Student student;
    private Subject subject;
    private Flashcard flashcard;

    @BeforeEach
    void setUp() {
        student = Student.builder()
                .id(UUID.randomUUID())
                .phoneNumber("5534999998888")
                .fullName("Estudante Teste")
                .active(true)
                .build();

        Course course = Course.builder().id(1L).code("MEDICINA").name("Medicina").build();
        subject = Subject.builder().id(10L).course(course).code("CLIN_MED").name("Clínica Médica").build();
        flashcard = Flashcard.builder().id(100L).subject(subject).topic("Cardio").question("Q").answer("A").build();
    }

    @Test
    @DisplayName("Deve inicializar agendamentos de teste para estudante com sucesso")
    void shouldSeedSchedulesForStudent() {
        SeedScheduleRequestDto request = new SeedScheduleRequestDto(10L);

        when(studentRepository.findByPhoneNumber("5534999998888")).thenReturn(Optional.of(student));
        when(subjectRepository.findById(10L)).thenReturn(Optional.of(subject));
        when(flashcardRepository.findBySubjectIdAndActiveTrue(10L)).thenReturn(List.of(flashcard));
        when(repetitionScheduleRepository.findByStudentIdAndFlashcardId(student.getId(), 100L)).thenReturn(Optional.empty());

        RepetitionSchedule schedule = RepetitionSchedule.builder()
                .student(student)
                .flashcard(flashcard)
                .nIndex(0)
                .intervalDays(1)
                .nextReviewDate(LocalDate.now())
                .build();

        when(mmeebbService.initializeSchedule(eq(student), eq(flashcard), any(LocalDate.class))).thenReturn(schedule);
        when(repetitionScheduleRepository.save(any(RepetitionSchedule.class))).thenReturn(schedule);

        SeedScheduleResponseDto result = studentAdminService.seedSchedulesForStudent("5534999998888", request);

        assertThat(result.phoneNumber()).isEqualTo("5534999998888");
        assertThat(result.subjectName()).isEqualTo("Clínica Médica");
        assertThat(result.schedulesCreated()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve criar estudante caso não exista ao fazer seed de agendamentos")
    void shouldCreateStudentIfNotFoundWhenSeeding() {
        SeedScheduleRequestDto request = new SeedScheduleRequestDto(10L);

        when(studentRepository.findByPhoneNumber("5534999997777")).thenReturn(Optional.empty());
        when(studentRepository.save(any(Student.class))).thenReturn(student);
        when(subjectRepository.findById(10L)).thenReturn(Optional.of(subject));
        when(flashcardRepository.findBySubjectIdAndActiveTrue(10L)).thenReturn(List.of(flashcard));
        when(repetitionScheduleRepository.findByStudentIdAndFlashcardId(any(), eq(100L))).thenReturn(Optional.empty());

        RepetitionSchedule schedule = RepetitionSchedule.builder()
                .student(student)
                .flashcard(flashcard)
                .nIndex(0)
                .intervalDays(1)
                .nextReviewDate(LocalDate.now())
                .build();

        when(mmeebbService.initializeSchedule(any(), eq(flashcard), any())).thenReturn(schedule);

        SeedScheduleResponseDto result = studentAdminService.seedSchedulesForStudent("5534999997777", request);

        assertThat(result.schedulesCreated()).isEqualTo(1);
        verify(studentRepository, times(1)).save(any(Student.class));
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando disciplina não existir")
    void shouldThrowWhenSubjectNotFound() {
        SeedScheduleRequestDto request = new SeedScheduleRequestDto(999L);
        when(studentRepository.findByPhoneNumber("5534999998888")).thenReturn(Optional.of(student));
        when(subjectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentAdminService.seedSchedulesForStudent("5534999998888", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Disciplina com ID 999 não encontrada");
    }
}
