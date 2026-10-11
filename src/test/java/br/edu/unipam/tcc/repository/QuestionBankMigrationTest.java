package br.edu.unipam.tcc.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Garante o banco de questões da V10: só múltipla escolha ativa (decisão com a orientadora), metas
 * mínimas por curso e por disciplina, gabarito presente entre as alternativas, nenhuma questão
 * duplicada e todas as migrations aplicando do zero. Sem Docker disponível, a classe é ignorada.
 */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class QuestionBankMigrationTest {

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
    }

    private static final String ACTIVE_QUESTIONS = """
            FROM tb_flashcard f
            JOIN tb_subject s ON s.id = f.subject_id AND s.active = TRUE
            JOIN tb_course c ON c.id = s.course_id AND c.active = TRUE
            WHERE f.active = TRUE
            """;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Todas as migrations devem aplicar do zero, até a V10")
    void shouldApplyAllMigrationsFromScratch() {
        Integer failed = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = FALSE", Integer.class);
        Integer latest = jdbcTemplate.queryForObject(
                "SELECT MAX(CAST(version AS INTEGER)) FROM flyway_schema_history WHERE version IS NOT NULL", Integer.class);

        assertThat(failed).isZero();
        assertThat(latest).isGreaterThanOrEqualTo(10);
    }

    @Test
    @DisplayName("Medicina deve ter ao menos 300 questões ativas e SI ao menos 200")
    void shouldMeetMinimumQuestionsPerCourse() {
        Map<String, Integer> byCourse = countBy("c.code");

        assertThat(byCourse.getOrDefault("MEDICINA", 0)).isGreaterThanOrEqualTo(300);
        assertThat(byCourse.getOrDefault("SIS_INFO", 0)).isGreaterThanOrEqualTo(200);
    }

    @Test
    @DisplayName("Cada disciplina deve atingir a sua meta mínima de questões ativas")
    void shouldMeetMinimumQuestionsPerSubject() {
        Map<String, Integer> minimums = Map.ofEntries(
                Map.entry("CLIN_MED", 90), Map.entry("CIR_GERAL", 50), Map.entry("PEDIATRIA", 50),
                Map.entry("GINEC_OBST", 50), Map.entry("MED_FAM_COM", 40), Map.entry("URG_EMERG", 20),
                Map.entry("SAUDE_MENTAL", 20), Map.entry("ENG_SOFT", 35), Map.entry("BD_PERSIST", 35),
                Map.entry("ESTRUT_ALG", 35), Map.entry("SEG_INFO", 30), Map.entry("REDES_DISTRIB", 30),
                Map.entry("PROG_OO", 25), Map.entry("GER_PROJ", 25));

        Map<String, Integer> bySubject = countBy("s.code");

        minimums.forEach((code, minimum) ->
                assertThat(bySubject.getOrDefault(code, 0)).as(code).isGreaterThanOrEqualTo(minimum));
    }

    @Test
    @DisplayName("Só questões de múltipla escolha devem continuar ativas")
    void shouldKeepOnlyMultipleChoiceActive() {
        Integer openActive = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) " + ACTIVE_QUESTIONS + " AND f.question_type <> 'MULTIPLE_CHOICE'", Integer.class);

        assertThat(openActive).isZero();
    }

    @Test
    @DisplayName("As 19 questões abertas (FLASHCARD) das seeds antigas devem ficar desativadas, sem serem apagadas")
    void shouldDeactivateOpenQuestionsWithoutDeletingThem() {
        Integer open = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_flashcard WHERE question_type = 'FLASHCARD'", Integer.class);
        Integer openActive = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_flashcard WHERE question_type = 'FLASHCARD' AND active = TRUE", Integer.class);

        assertThat(open).isEqualTo(19);
        assertThat(openActive).isZero();
    }

    @Test
    @DisplayName("O gabarito deve ser uma letra presente entre 4 ou 5 alternativas rotuladas em ordem")
    void shouldHaveAnswerAmongOrderedOptions() throws Exception {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT f.id, f.answer, f.options_json::text AS options " + ACTIVE_QUESTIONS);

        assertThat(rows).isNotEmpty();
        for (Map<String, Object> row : rows) {
            String id = String.valueOf(row.get("id"));
            List<String> options = objectMapper.readValue((String) row.get("options"), new TypeReference<>() {});
            String answer = ((String) row.get("answer")).trim();

            assertThat(options).as("alternativas da questão %s", id).hasSizeBetween(4, 5);
            for (int i = 0; i < options.size(); i++) {
                assertThat(options.get(i)).as("rótulo da alternativa %d da questão %s", i, id)
                        .startsWith((char) ('A' + i) + ") ");
            }
            assertThat(answer).as("gabarito da questão %s", id).matches("[A-E]");
            assertThat(answer.charAt(0) - 'A').as("gabarito da questão %s", id).isLessThan(options.size());
        }
    }

    @Test
    @DisplayName("Nenhuma questão ativa deve repetir o enunciado de outra")
    void shouldHaveNoDuplicateActiveQuestions() {
        List<String> questions = jdbcTemplate.queryForList("SELECT f.question " + ACTIVE_QUESTIONS, String.class);

        Map<String, Long> repeated = questions.stream()
                .collect(Collectors.groupingBy(QuestionBankMigrationTest::normalize, Collectors.counting()))
                .entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        assertThat(repeated).isEmpty();
    }

    @Test
    @DisplayName("Toda questão ativa deve ter explicação curta para o feedback no WhatsApp")
    void shouldHaveExplanationForEveryActiveQuestion() {
        Integer missing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) " + ACTIVE_QUESTIONS + " AND (f.explanation IS NULL OR btrim(f.explanation) = '')",
                Integer.class);

        assertThat(missing).isZero();
    }

    @Test
    @DisplayName("Cada disciplina com questões deve misturar EASY, MEDIUM e HARD")
    void shouldMixDifficultiesInEverySubject() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT s.code, COUNT(DISTINCT f.difficulty) AS levels " + ACTIVE_QUESTIONS + " GROUP BY s.code");

        assertThat(rows).isNotEmpty();
        rows.forEach(row -> assertThat(((Number) row.get("levels")).intValue()).as((String) row.get("code")).isEqualTo(3));
    }

    @Test
    @DisplayName("Nenhuma letra deve concentrar mais de 40% dos gabaritos de um curso")
    void shouldSpreadAnswerLettersWithinEachCourse() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT c.code, f.answer, COUNT(*) AS total " + ACTIVE_QUESTIONS + " GROUP BY c.code, f.answer");

        Map<String, Integer> totalByCourse = new HashMap<>();
        rows.forEach(row -> totalByCourse.merge((String) row.get("code"), ((Number) row.get("total")).intValue(), Integer::sum));

        rows.forEach(row -> {
            double share = ((Number) row.get("total")).doubleValue() / totalByCourse.get((String) row.get("code"));
            assertThat(share).as("letra %s em %s", row.get("answer"), row.get("code")).isLessThanOrEqualTo(0.40);
        });
    }

    @Test
    @DisplayName("A V10 deve criar as disciplinas de internato e de SI que faltavam")
    void shouldCreateNewSubjects() {
        Set<String> active = Set.copyOf(jdbcTemplate.queryForList(
                "SELECT c.code || ':' || s.code FROM tb_subject s JOIN tb_course c ON c.id = s.course_id WHERE s.active = TRUE",
                String.class));

        assertThat(active).contains("MEDICINA:URG_EMERG", "MEDICINA:SAUDE_MENTAL", "SIS_INFO:PROG_OO", "SIS_INFO:GER_PROJ");
    }

    private Map<String, Integer> countBy(String column) {
        Map<String, Integer> counts = new HashMap<>();
        jdbcTemplate.queryForList("SELECT " + column + " AS code, COUNT(*) AS total " + ACTIVE_QUESTIONS + " GROUP BY " + column)
                .forEach(row -> counts.put((String) row.get("code"), ((Number) row.get("total")).intValue()));
        return counts;
    }

    private static String normalize(String text) {
        String ascii = Normalizer.normalize(text, Normalizer.Form.NFKD).replaceAll("\\p{M}", "").toLowerCase();
        return ascii.replaceAll("[^a-z0-9 ]+", " ").replaceAll("\\s+", " ").trim();
    }
}
