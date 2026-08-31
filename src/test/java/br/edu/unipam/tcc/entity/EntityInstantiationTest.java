package br.edu.unipam.tcc.entity;

import br.edu.unipam.tcc.entity.enums.ChatState;
import br.edu.unipam.tcc.entity.enums.DifficultyLevel;
import br.edu.unipam.tcc.entity.enums.QuestionType;
import br.edu.unipam.tcc.entity.enums.ScheduleStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EntityInstantiationTest {

    @Test
    @DisplayName("Deve instanciar Course com valores padrão corretos")
    void deveInstanciarCourseCorretamente() {
        Course course = Course.builder()
                .code("MEDICINA")
                .name("Medicina")
                .description("Curso de Medicina")
                .build();

        assertNull(course.getId());
        assertEquals("MEDICINA", course.getCode());
        assertEquals("Medicina", course.getName());
        assertTrue(course.getActive());
    }

    @Test
    @DisplayName("Deve instanciar Subject vinculado ao Course")
    void deveInstanciarSubjectCorretamente() {
        Course course = Course.builder().id(1L).code("MEDICINA").name("Medicina").build();
        Subject subject = Subject.builder()
                .course(course)
                .code("CLINICA_MEDICA")
                .name("Clínica Médica")
                .build();

        assertEquals(course, subject.getCourse());
        assertEquals("CLINICA_MEDICA", subject.getCode());
        assertTrue(subject.getActive());
    }

    @Test
    @DisplayName("Deve instanciar Student com horários e RA")
    void deveInstanciarStudentCorretamente() {
        Student student = Student.builder()
                .phoneNumber("5534999998888")
                .fullName("Níckolas Tavares")
                .ra("23000388")
                .preferredStudyTime(LocalTime.of(8, 30))
                .build();

        assertEquals("5534999998888", student.getPhoneNumber());
        assertEquals("23000388", student.getRa());
        assertEquals(LocalTime.of(8, 30), student.getPreferredStudyTime());
        assertTrue(student.getActive());
    }

    @Test
    @DisplayName("Deve instanciar Flashcard com tipos e dificuldade padrão")
    void deveInstanciarFlashcardCorretamente() {
        Subject subject = Subject.builder().id(10L).code("FARMACO").name("Farmacologia").build();
        Flashcard flashcard = Flashcard.builder()
                .subject(subject)
                .topic("Anti-hipertensivos")
                .question("Qual o mecanismo de ação dos IECAs?")
                .answer("Inibem a conversão de angiotensina I em angiotensina II.")
                .difficulty(DifficultyLevel.HARD)
                .build();

        assertEquals(QuestionType.FLASHCARD, flashcard.getQuestionType());
        assertEquals(DifficultyLevel.HARD, flashcard.getDifficulty());
        assertEquals("Anti-hipertensivos", flashcard.getTopic());
        assertTrue(flashcard.getActive());
    }

    @Test
    @DisplayName("Deve instanciar RepetitionSchedule com valores iniciais do MMEEBB")
    void deveInstanciarRepetitionScheduleCorretamente() {
        Student student = Student.builder().id(UUID.randomUUID()).phoneNumber("5534999998888").fullName("Aluno").build();
        Flashcard flashcard = Flashcard.builder().id(100L).topic("Gastro").question("Q").answer("A").build();

        RepetitionSchedule schedule = RepetitionSchedule.builder()
                .student(student)
                .flashcard(flashcard)
                .build();

        assertEquals(0, schedule.getNIndex());
        assertEquals(1, schedule.getIntervalDays());
        assertEquals(0, schedule.getRepetitionCount());
        assertEquals(0, schedule.getConsecutiveCorrect());
        assertEquals(ScheduleStatus.PENDING, schedule.getStatus());
        assertEquals(LocalDate.now(), schedule.getNextReviewDate());
    }

    @Test
    @DisplayName("Deve instanciar ChatSession com estado inicial NEW")
    void deveInstanciarChatSessionCorretamente() {
        ChatSession session = ChatSession.builder()
                .phoneNumber("5534999998888")
                .lastInteractionAt(LocalDateTime.now())
                .build();

        assertEquals(ChatState.NEW, session.getCurrentState());
        assertEquals("5534999998888", session.getPhoneNumber());
        assertNull(session.getSelectedCourse());
    }
}
