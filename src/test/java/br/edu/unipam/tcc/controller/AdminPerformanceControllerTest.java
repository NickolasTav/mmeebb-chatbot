package br.edu.unipam.tcc.controller;

import br.edu.unipam.tcc.config.AdminApiKeyInterceptor;
import br.edu.unipam.tcc.dto.PerformanceOverviewDto;
import br.edu.unipam.tcc.dto.StudentPerformanceReportDto;
import br.edu.unipam.tcc.dto.SubjectAttemptAggregateDto;
import br.edu.unipam.tcc.dto.TopicPerformanceDto;
import br.edu.unipam.tcc.entity.Student;
import br.edu.unipam.tcc.entity.enums.TopicMastery;
import br.edu.unipam.tcc.exception.GlobalExceptionHandler;
import br.edu.unipam.tcc.repository.StudentRepository;
import br.edu.unipam.tcc.service.PerformanceAnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminPerformanceControllerTest {

    private static final String API_KEY = "test-admin-key";
    private static final UUID ALUNO = UUID.randomUUID();

    private MockMvc mockMvc;

    @Mock private PerformanceAnalysisService performanceAnalysisService;
    @Mock private StudentRepository studentRepository;

    @InjectMocks private AdminPerformanceController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(new AdminApiKeyInterceptor(API_KEY))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Smoke Test: deve devolver o relatório de desempenho do estudante")
    void deveDevolverORelatorioDoEstudante() throws Exception {
        Student student = Student.builder().fullName("Maria Silva").preferredName("Mari").build();
        student.setId(ALUNO);
        when(studentRepository.findById(ALUNO)).thenReturn(Optional.of(student));
        when(performanceAnalysisService.buildStudentReport(student)).thenReturn(
                new StudentPerformanceReportDto(ALUNO, "Mari", 90, 48L, 31L, 0.6458, true, List.of(
                        new TopicPerformanceDto(4L, "Farmacologia Clinica", "Antibioticoterapia",
                                9L, 6L, 0.6667, TopicMastery.FRAGIL))));

        mockMvc.perform(get("/api/admin/performance/students/{id}", ALUNO).header("api_key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentName").value("Mari"))
                .andExpect(jsonPath("$.totalAttempts").value(48))
                .andExpect(jsonPath("$.windowDays").value(90))
                .andExpect(jsonPath("$.topics[0].topic").value("Antibioticoterapia"))
                .andExpect(jsonPath("$.topics[0].mastery").value("FRAGIL"))
                .andExpect(jsonPath("$.topics[0].maxIntervalDays").value(8));
    }

    @Test
    @DisplayName("Deve devolver 404 quando o estudante não existir")
    void deveDevolver404QuandoEstudanteNaoExistir() throws Exception {
        when(studentRepository.findById(ALUNO)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/admin/performance/students/{id}", ALUNO).header("api_key", API_KEY))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve devolver a visão agregada de todos os estudantes")
    void deveDevolverAVisaoAgregada() throws Exception {
        when(performanceAnalysisService.buildOverview()).thenReturn(
                new PerformanceOverviewDto(90, 7L, 312L, 222L, 0.7115, List.of(
                        new SubjectAttemptAggregateDto(4L, "Farmacologia Clinica", 88L, 52L))));

        mockMvc.perform(get("/api/admin/performance/overview").header("api_key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.students").value(7))
                .andExpect(jsonPath("$.totalAttempts").value(312))
                .andExpect(jsonPath("$.bySubject[0].subjectName").value("Farmacologia Clinica"))
                .andExpect(jsonPath("$.bySubject[0].accuracy").value(52d / 88d));
    }

    @Test
    @DisplayName("Deve bloquear o acesso sem a chave administrativa")
    void deveBloquearSemChaveAdministrativa() throws Exception {
        mockMvc.perform(get("/api/admin/performance/overview"))
                .andExpect(status().isUnauthorized());
    }
}
