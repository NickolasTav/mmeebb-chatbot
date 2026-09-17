package br.edu.unipam.tcc.repository;

import br.edu.unipam.tcc.entity.Course;
import br.edu.unipam.tcc.entity.Flashcard;
import br.edu.unipam.tcc.entity.RepetitionSchedule;
import br.edu.unipam.tcc.entity.Student;
import br.edu.unipam.tcc.entity.StudentCourse;
import br.edu.unipam.tcc.entity.Subject;
import br.edu.unipam.tcc.entity.enums.ScheduleStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Valida o filtro de matrícula ativa nas consultas de pendência contra um Postgres real com as
 * migrations Flyway aplicadas. O H2 não serve aqui: não conhece o tipo {@code jsonb} usado em
 * {@code tb_flashcard}. Sem Docker disponível, a classe é ignorada.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class RepetitionScheduleRepositoryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 17);

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired private StudentRepository studentRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private FlashcardRepository flashcardRepository;
    @Autowired private StudentCourseRepository studentCourseRepository;
    @Autowired private RepetitionScheduleRepository repetitionScheduleRepository;

    private Student student;
    private Course medicina;
    private Course sistemas;

    @BeforeEach
    void setUp() {
        student = studentRepository.save(Student.builder()
                .phoneNumber("5534900001111").fullName("Maria Silva").build());
        medicina = courseRepository.save(Course.builder().code("IT_MED").name("Medicina IT").build());
        sistemas = courseRepository.save(Course.builder().code("IT_SI").name("Sistemas IT").build());
        scheduleDueCardOf(medicina);
        scheduleDueCardOf(sistemas);
    }

    private void scheduleDueCardOf(Course course) {
        Subject subject = subjectRepository.save(Subject.builder()
                .course(course).code(course.getCode() + "_DISC").name("Disciplina " + course.getName()).build());
        Flashcard card = flashcardRepository.save(Flashcard.builder()
                .subject(subject).topic("Tópico").question("Pergunta?").answer("Resposta").build());
        repetitionScheduleRepository.save(RepetitionSchedule.builder()
                .student(student).flashcard(card).nextReviewDate(TODAY).build());
    }

    private StudentCourse enroll(Course course, boolean active) {
        return studentCourseRepository.save(StudentCourse.builder()
                .student(student).course(course).academicPeriod(8).active(active).build());
    }

    private List<String> pendingCourseCodes() {
        return repetitionScheduleRepository
                .findPendingReviewsByStudent(student.getId(), TODAY, ScheduleStatus.PENDING)
                .stream()
                .map(schedule -> schedule.getFlashcard().getSubject().getCourse().getCode())
                .toList();
    }

    @Test
    @DisplayName("Deve considerar apenas cards de cursos com matrícula ativa em todas as consultas de pendência")
    void deveContarApenasCardsDeMatriculaAtiva() {
        enroll(medicina, true);
        enroll(sistemas, false);

        assertThat(repetitionScheduleRepository
                .countByStudentIdAndNextReviewDateLessThanEqualAndIsActiveTrue(student.getId(), TODAY)).isEqualTo(1);
        assertThat(repetitionScheduleRepository
                .countPendingReviewsByStudent(student.getId(), TODAY, List.of(ScheduleStatus.PENDING))).isEqualTo(1);
        assertThat(pendingCourseCodes()).containsExactly("IT_MED");
    }

    @Test
    @DisplayName("Não deve haver pendências quando o estudante não tiver matrícula ativa")
    void naoDeveTerPendenciasSemMatriculaAtiva() {
        enroll(medicina, false);
        enroll(sistemas, false);

        assertThat(repetitionScheduleRepository
                .countByStudentIdAndNextReviewDateLessThanEqualAndIsActiveTrue(student.getId(), TODAY)).isZero();
        assertThat(pendingCourseCodes()).isEmpty();
    }

    @Test
    @DisplayName("Deve trazer de volta o progresso do curso antigo quando a matrícula for reativada")
    void deveRestaurarCursoAntigoQuandoMatriculaReativada() {
        StudentCourse medicinaLink = enroll(medicina, false);
        StudentCourse sistemasLink = enroll(sistemas, true);
        assertThat(pendingCourseCodes()).containsExactly("IT_SI");

        medicinaLink.setActive(true);
        sistemasLink.setActive(false);
        studentCourseRepository.saveAll(List.of(medicinaLink, sistemasLink));
        studentCourseRepository.flush();

        assertThat(pendingCourseCodes()).containsExactly("IT_MED");
    }
}
