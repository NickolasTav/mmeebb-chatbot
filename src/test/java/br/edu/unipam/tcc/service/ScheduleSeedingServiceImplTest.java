package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.entity.Course;
import br.edu.unipam.tcc.entity.Flashcard;
import br.edu.unipam.tcc.entity.RepetitionSchedule;
import br.edu.unipam.tcc.entity.Student;
import br.edu.unipam.tcc.entity.Subject;
import br.edu.unipam.tcc.repository.FlashcardRepository;
import br.edu.unipam.tcc.repository.RepetitionScheduleRepository;
import br.edu.unipam.tcc.repository.SubjectRepository;
import br.edu.unipam.tcc.service.impl.ScheduleSeedingServiceImpl;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleSeedingServiceImplTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 17);

    @Mock private SubjectRepository subjectRepository;
    @Mock private FlashcardRepository flashcardRepository;
    @Mock private RepetitionScheduleRepository repetitionScheduleRepository;
    @Mock private MmeebbService mmeebbService;

    private ScheduleSeedingServiceImpl service;

    @BeforeEach
    void setUp() {
        // 22:30 em Brasília já é o dia seguinte em UTC: o Clock injetado mantém a data correta.
        Clock clock = Clock.fixed(LocalDateTime.of(TODAY, LocalTime.of(22, 30)).atZone(ZONE).toInstant(), ZONE);
        service = new ScheduleSeedingServiceImpl(
                subjectRepository, flashcardRepository, repetitionScheduleRepository, mmeebbService, clock);
    }

    @Test
    @DisplayName("Deve criar agendamentos liberados para hoje apenas para cards ainda sem agendamento")
    void deveSemearApenasCardsSemAgendamento() {
        Student student = Student.builder().id(UUID.randomUUID()).fullName("Maria Silva").build();
        Course course = Course.builder().id(1L).code("MED").name("Medicina").build();
        Subject subject = Subject.builder().id(10L).course(course).code("CARD").name("Cardiologia").build();
        Flashcard existente = Flashcard.builder().id(100L).subject(subject).build();
        Flashcard novo = Flashcard.builder().id(101L).subject(subject).build();

        when(subjectRepository.findByCourseIdAndActiveTrue(1L)).thenReturn(List.of(subject));
        when(flashcardRepository.findBySubjectIdAndActiveTrue(10L)).thenReturn(List.of(existente, novo));
        when(repetitionScheduleRepository.findByStudentIdAndFlashcardId(student.getId(), 100L))
                .thenReturn(Optional.of(RepetitionSchedule.builder().build()));
        when(repetitionScheduleRepository.findByStudentIdAndFlashcardId(student.getId(), 101L))
                .thenReturn(Optional.empty());
        when(mmeebbService.initializeSchedule(student, novo, TODAY))
                .thenReturn(RepetitionSchedule.builder()
                        .student(student).flashcard(novo).nextReviewDate(TODAY.plusDays(1)).build());

        int created = service.seedMissingSchedules(student, course);

        assertEquals(1, created);
        ArgumentCaptor<RepetitionSchedule> saved = ArgumentCaptor.forClass(RepetitionSchedule.class);
        verify(repetitionScheduleRepository, times(1)).save(saved.capture());
        assertEquals(novo, saved.getValue().getFlashcard());
        assertEquals(TODAY, saved.getValue().getNextReviewDate());
    }
}
