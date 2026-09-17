package br.edu.unipam.tcc.repository;

import br.edu.unipam.tcc.dto.AttemptTotalsDto;
import br.edu.unipam.tcc.dto.SubjectAttemptAggregateDto;
import br.edu.unipam.tcc.dto.TopicAttemptAggregateDto;
import br.edu.unipam.tcc.entity.Course;
import br.edu.unipam.tcc.entity.Flashcard;
import br.edu.unipam.tcc.entity.ReviewAttempt;
import br.edu.unipam.tcc.entity.Student;
import br.edu.unipam.tcc.entity.Subject;
import br.edu.unipam.tcc.entity.enums.TopicMastery;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Valida as agregações de desempenho contra um Postgres real com as migrations Flyway aplicadas.
 * O H2 não serve aqui: não conhece o tipo {@code jsonb} usado em {@code tb_flashcard}.
 * Sem Docker disponível, a classe é ignorada.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class ReviewAttemptRepositoryTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 9, 17, 10, 0);
    private static final LocalDateTime INICIO_DA_JANELA = AGORA.minusDays(90);

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
    @Autowired private ReviewAttemptRepository reviewAttemptRepository;

    private Student maria;
    private Student outroAluno;
    private Flashcard antibiotico;
    private Flashcard arritmia;
    private Subject farmacologia;

    @BeforeEach
    void setUp() {
        maria = studentRepository.save(Student.builder()
                .phoneNumber("5534900001111").fullName("Maria Silva").build());
        outroAluno = studentRepository.save(Student.builder()
                .phoneNumber("5534900002222").fullName("Joao Souza").build());

        Course medicina = courseRepository.save(Course.builder().code("IT_MED").name("Medicina IT").build());
        farmacologia = subjectRepository.save(Subject.builder()
                .course(medicina).code("IT_FARMACO").name("Farmacologia Clinica").build());
        Subject cardiologia = subjectRepository.save(Subject.builder()
                .course(medicina).code("IT_CARDIO").name("Cardiologia").build());

        antibiotico = flashcardRepository.save(Flashcard.builder()
                .subject(farmacologia).topic("Antibioticoterapia")
                .question("Qual o espectro da amoxicilina?").answer("Gram-positivos").build());
        arritmia = flashcardRepository.save(Flashcard.builder()
                .subject(cardiologia).topic("Arritmias")
                .question("Qual a conduta na FA aguda?").answer("Controle de frequencia").build());
    }

    @Test
    @DisplayName("Deve agregar tentativas e erros por tópico dentro da janela")
    void deveAgregarTentativasPorTopico() {
        registrar(maria, antibiotico, false, AGORA.minusDays(3));
        registrar(maria, antibiotico, false, AGORA.minusDays(2));
        registrar(maria, antibiotico, true, AGORA.minusDays(1));
        registrar(maria, arritmia, true, AGORA.minusDays(1));

        List<TopicAttemptAggregateDto> agregados =
                reviewAttemptRepository.aggregateByTopic(maria.getId(), INICIO_DA_JANELA);

        assertThat(agregados).hasSize(2);

        TopicAttemptAggregateDto antibioticoterapia = agregados.stream()
                .filter(a -> a.topic().equals("Antibioticoterapia")).findFirst().orElseThrow();
        assertThat(antibioticoterapia.subjectId()).isEqualTo(farmacologia.getId());
        assertThat(antibioticoterapia.subjectName()).isEqualTo("Farmacologia Clinica");
        assertThat(antibioticoterapia.attempts()).isEqualTo(3);
        assertThat(antibioticoterapia.errors()).isEqualTo(2);
    }

    @Test
    @DisplayName("Deve ignorar tentativas anteriores à janela de análise")
    void deveIgnorarTentativasForaDaJanela() {
        registrar(maria, antibiotico, false, AGORA.minusDays(120));
        registrar(maria, antibiotico, false, AGORA.minusDays(91));
        registrar(maria, antibiotico, true, AGORA.minusDays(10));

        List<TopicAttemptAggregateDto> agregados =
                reviewAttemptRepository.aggregateByTopic(maria.getId(), INICIO_DA_JANELA);

        assertThat(agregados).singleElement().satisfies(agregado -> {
            assertThat(agregado.attempts()).isEqualTo(1);
            assertThat(agregado.errors()).isZero();
        });
    }

    @Test
    @DisplayName("Deve isolar o histórico de cada estudante")
    void deveIsolarHistoricoPorEstudante() {
        registrar(maria, antibiotico, true, AGORA.minusDays(1));
        registrar(outroAluno, antibiotico, false, AGORA.minusDays(1));
        registrar(outroAluno, antibiotico, false, AGORA.minusDays(2));

        assertThat(reviewAttemptRepository.aggregateByTopic(maria.getId(), INICIO_DA_JANELA))
                .singleElement()
                .satisfies(agregado -> assertThat(agregado.errors()).isZero());

        assertThat(reviewAttemptRepository.aggregateByTopic(outroAluno.getId(), INICIO_DA_JANELA))
                .singleElement()
                .satisfies(agregado -> assertThat(agregado.errors()).isEqualTo(2));
    }

    @Test
    @DisplayName("Deve totalizar tentativas e acertos do estudante na janela")
    void deveTotalizarTentativasDoEstudante() {
        registrar(maria, antibiotico, true, AGORA.minusDays(1));
        registrar(maria, antibiotico, false, AGORA.minusDays(2));
        registrar(maria, arritmia, true, AGORA.minusDays(3));

        AttemptTotalsDto totais = reviewAttemptRepository.totalsByStudent(maria.getId(), INICIO_DA_JANELA);

        assertThat(totais.attempts()).isEqualTo(3);
        assertThat(totais.correctAttempts()).isEqualTo(2);
    }

    @Test
    @DisplayName("Deve devolver totais zerados quando o estudante ainda não respondeu nada")
    void deveDevolverTotaisZeradosSemHistorico() {
        AttemptTotalsDto totais = reviewAttemptRepository.totalsByStudent(maria.getId(), INICIO_DA_JANELA);

        assertThat(totais.attempts()).isZero();
        assertThat(totais.correctAttempts()).isZero();
    }

    @Test
    @DisplayName("Deve agregar o desempenho global por disciplina somando todos os estudantes")
    void deveAgregarDesempenhoGlobalPorDisciplina() {
        registrar(maria, antibiotico, true, AGORA.minusDays(1));
        registrar(outroAluno, antibiotico, false, AGORA.minusDays(1));
        registrar(outroAluno, arritmia, true, AGORA.minusDays(1));

        List<SubjectAttemptAggregateDto> agregados =
                reviewAttemptRepository.aggregateBySubject(INICIO_DA_JANELA);

        assertThat(agregados).hasSize(2);
        assertThat(agregados.get(0).attempts()).isEqualTo(2);
        assertThat(agregados.get(0).subjectName()).isEqualTo("Farmacologia Clinica");
        assertThat(agregados.get(0).correctAttempts()).isEqualTo(1);

        assertThat(reviewAttemptRepository.countDistinctStudents(INICIO_DA_JANELA)).isEqualTo(2);
    }

    private void registrar(Student student, Flashcard flashcard, boolean correct, LocalDateTime answeredAt) {
        reviewAttemptRepository.save(ReviewAttempt.builder()
                .student(student)
                .flashcard(flashcard)
                .correct(correct)
                .nIndexBefore(0)
                .nIndexAfter(correct ? 1 : 0)
                .intervalDaysAfter(correct ? 2 : 1)
                .topicMastery(TopicMastery.SEM_DADOS)
                .answeredAt(answeredAt)
                .build());
    }
}
