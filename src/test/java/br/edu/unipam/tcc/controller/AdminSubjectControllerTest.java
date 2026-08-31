package br.edu.unipam.tcc.controller;

import br.edu.unipam.tcc.dto.SubjectRequestDto;
import br.edu.unipam.tcc.dto.SubjectResponseDto;
import br.edu.unipam.tcc.dto.SubjectStatusDto;
import br.edu.unipam.tcc.exception.GlobalExceptionHandler;
import br.edu.unipam.tcc.service.SubjectService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminSubjectControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SubjectService subjectService;

    @InjectMocks
    private AdminSubjectController subjectController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private SubjectResponseDto subjectResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(subjectController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        subjectResponse = new SubjectResponseDto(
                10L,
                1L,
                "Medicina",
                "CLIN_MED",
                "Clínica Médica",
                "Disciplina de Clínica Médica",
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("Smoke Test: Deve listar matérias via GET /api/admin/subjects")
    void shouldListSubjects() throws Exception {
        when(subjectService.findAll(1L, true)).thenReturn(List.of(subjectResponse));

        mockMvc.perform(get("/api/admin/subjects?courseId=1&active=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10L))
                .andExpect(jsonPath("$[0].code").value("CLIN_MED"))
                .andExpect(jsonPath("$[0].courseName").value("Medicina"));

        verify(subjectService, times(1)).findAll(1L, true);
    }

    @Test
    @DisplayName("Deve buscar matéria por ID via GET /api/admin/subjects/{id}")
    void shouldFindSubjectById() throws Exception {
        when(subjectService.findById(10L)).thenReturn(subjectResponse);

        mockMvc.perform(get("/api/admin/subjects/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.name").value("Clínica Médica"));

        verify(subjectService, times(1)).findById(10L);
    }

    @Test
    @DisplayName("Deve criar matéria via POST /api/admin/subjects com status 201 Created")
    void shouldCreateSubject() throws Exception {
        SubjectRequestDto request = new SubjectRequestDto(1L, "CLIN_MED", "Clínica Médica", "Desc", true);
        when(subjectService.create(any(SubjectRequestDto.class))).thenReturn(subjectResponse);

        mockMvc.perform(post("/api/admin/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L));

        verify(subjectService, times(1)).create(any(SubjectRequestDto.class));
    }

    @Test
    @DisplayName("Deve atualizar matéria via PUT /api/admin/subjects/{id}")
    void shouldUpdateSubject() throws Exception {
        SubjectRequestDto request = new SubjectRequestDto(1L, "CLIN_MED", "Clínica Médica UNIPAM", "Desc", true);
        when(subjectService.update(eq(10L), any(SubjectRequestDto.class))).thenReturn(subjectResponse);

        mockMvc.perform(put("/api/admin/subjects/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));

        verify(subjectService, times(1)).update(eq(10L), any(SubjectRequestDto.class));
    }

    @Test
    @DisplayName("Deve atualizar status via PATCH /api/admin/subjects/{id}/status")
    void shouldUpdateSubjectStatus() throws Exception {
        SubjectStatusDto statusDto = new SubjectStatusDto(false);
        SubjectResponseDto inactiveResponse = new SubjectResponseDto(10L, 1L, "Medicina", "CLIN_MED", "Clínica Médica", "Desc", false, LocalDateTime.now(), LocalDateTime.now());
        when(subjectService.updateStatus(eq(10L), any(SubjectStatusDto.class))).thenReturn(inactiveResponse);

        mockMvc.perform(patch("/api/admin/subjects/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        verify(subjectService, times(1)).updateStatus(eq(10L), any(SubjectStatusDto.class));
    }
}
