package br.edu.unipam.tcc.entity;

import br.edu.unipam.tcc.entity.enums.ChatState;
import br.edu.unipam.tcc.entity.enums.DifficultyLevel;
import br.edu.unipam.tcc.entity.enums.QuestionType;
import br.edu.unipam.tcc.entity.enums.ScheduleStatus;
import br.edu.unipam.tcc.entity.enums.TopicMastery;
import br.edu.unipam.tcc.service.MmeebbService;
import br.edu.unipam.tcc.session.ChatSessionState;
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
    @DisplayName("Deve usar o apelido como nome de exibição quando informado")
    void deveUsarApelidoComoNomeDeExibicao() {
        Student student = Student.builder().fullName("Maria Silva Andrade").preferredName("Mari").build();

        assertEquals("Mari", student.displayName());
    }

    @Test
    @DisplayName("Deve usar o primeiro nome quando não houver apelido")
    void deveUsarPrimeiroNomeQuandoNaoHouverApelido() {
        Student student = Student.builder().fullName("  Maria   Silva Andrade ").preferredName("  ").build();

        assertEquals("Maria", student.displayName());
    }

    @Test
    @DisplayName("Deve usar 'Estudante' quando não houver nome nem apelido")
    void deveUsarNomeGenericoQuandoNaoHouverNome() {
        assertEquals("Estudante", Student.builder().build().displayName());
    }

    @Test
    @DisplayName("Deve criar estudante com lembretes ativos às 08:00 e sem avaliação registrada")
    void deveCriarStudentComPreferenciasPadraoDeLembrete() {
        Student student = Student.builder().fullName("Maria Silva").build();

        assertEquals(LocalTime.of(8, 0), student.getPreferredStudyTime());
        assertEquals(Boolean.TRUE, student.getReviewNotificationsEnabled());
        assertNull(student.getLastReviewNotificationOn());
    }

    @Test
    @DisplayName("Deve instanciar ReviewAttempt registrando o N antes e depois da decisão")
    void deveInstanciarReviewAttemptCorretamente() {
        Student student = Student.builder().id(UUID.randomUUID()).fullName("Aluno").build();
        Flashcard flashcard = Flashcard.builder().id(100L).topic("Antibioticoterapia")
                .question("Q").answer("A").build();

        ReviewAttempt attempt = ReviewAttempt.builder()
                .student(student)
                .flashcard(flashcard)
                .correct(false)
                .nIndexBefore(6)
                .nIndexAfter(4)
                .intervalDaysAfter(16)
                .topicMastery(TopicMastery.DOMINADO)
                .answeredAt(LocalDateTime.of(2026, 9, 17, 10, 0))
                .build();

        assertEquals(6, attempt.getNIndexBefore());
        assertEquals(4, attempt.getNIndexAfter());
        assertEquals(16, attempt.getIntervalDaysAfter());
        assertEquals(TopicMastery.DOMINADO, attempt.getTopicMastery());
        assertFalse(attempt.getCorrect());
    }

    @Test
    @DisplayName("Deve nascer como SEM_DADOS quando o domínio do tópico não for informado")
    void deveUsarDominioSemDadosPorPadrao() {
        assertEquals(TopicMastery.SEM_DADOS, ReviewAttempt.builder().build().getTopicMastery());
    }

    @Test
    @DisplayName("Teto clássico do enum deve espelhar o MAX_N_INDEX do motor MMEEBB")
    void tetoClassicoDeveEspelharOMotorMmeebb() {
        assertEquals(MmeebbService.MAX_N_INDEX, TopicMastery.CLASSIC_MAX_N_INDEX);
        assertEquals(MmeebbService.MAX_N_INDEX, TopicMastery.SEM_DADOS.maxNIndex());
        assertEquals(MmeebbService.MAX_N_INDEX, TopicMastery.DOMINADO.maxNIndex());
        assertFalse(TopicMastery.DOMINADO.limitsInterval());
        assertTrue(TopicMastery.FRAGIL.limitsInterval());
        assertEquals(3, TopicMastery.FRAGIL.maxNIndex());
        assertEquals(6, TopicMastery.EM_CONSOLIDACAO.maxNIndex());
    }

    @Test
    @DisplayName("Deve instanciar ChatSessionState com estado inicial NEW e sem cadastro")
    void deveInstanciarChatSessionStateCorretamente() {
        ChatSessionState session = ChatSessionState.builder()
                .phoneNumber("5534999998888")
                .lastInteractionAt(LocalDateTime.now())
                .build();

        assertEquals(ChatState.NEW, session.getCurrentState());
        assertEquals("5534999998888", session.getPhoneNumber());
        assertNull(session.getSelectedCourseId());
        assertFalse(session.isRegistered());
    }
}
