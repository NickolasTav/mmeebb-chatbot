package br.edu.unipam.tcc.controller;

import br.edu.unipam.tcc.dto.CourseRequestDto;
import br.edu.unipam.tcc.dto.CourseResponseDto;
import br.edu.unipam.tcc.dto.CourseStatusDto;
import br.edu.unipam.tcc.exception.GlobalExceptionHandler;
import br.edu.unipam.tcc.service.CourseService;
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
class AdminCourseControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CourseService courseService;

    @InjectMocks
    private AdminCourseController courseController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CourseResponseDto courseResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(courseController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        courseResponse = new CourseResponseDto(
                1L,
                "MEDICINA",
                "Medicina",
                "Graduação em Medicina",
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("Smoke Test: Deve listar cursos via GET /api/admin/courses")
    void shouldListAllCourses() throws Exception {
        when(courseService.findAll(null)).thenReturn(List.of(courseResponse));

        mockMvc.perform(get("/api/admin/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].code").value("MEDICINA"))
                .andExpect(jsonPath("$[0].name").value("Medicina"));

        verify(courseService, times(1)).findAll(null);
    }

    @Test
    @DisplayName("Deve buscar curso por ID via GET /api/admin/courses/{id}")
    void shouldFindCourseById() throws Exception {
        when(courseService.findById(1L)).thenReturn(courseResponse);

        mockMvc.perform(get("/api/admin/courses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.code").value("MEDICINA"));

        verify(courseService, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Deve criar curso via POST /api/admin/courses com status 201 Created")
    void shouldCreateCourse() throws Exception {
        CourseRequestDto request = new CourseRequestDto("MEDICINA", "Medicina", "Desc", true);
        when(courseService.create(any(CourseRequestDto.class))).thenReturn(courseResponse);

        mockMvc.perform(post("/api/admin/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.code").value("MEDICINA"));

        verify(courseService, times(1)).create(any(CourseRequestDto.class));
    }

    @Test
    @DisplayName("Deve atualizar curso via PUT /api/admin/courses/{id}")
    void shouldUpdateCourse() throws Exception {
        CourseRequestDto request = new CourseRequestDto("MEDICINA", "Medicina UNIPAM", "Desc", true);
        when(courseService.update(eq(1L), any(CourseRequestDto.class))).thenReturn(courseResponse);

        mockMvc.perform(put("/api/admin/courses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(courseService, times(1)).update(eq(1L), any(CourseRequestDto.class));
    }

    @Test
    @DisplayName("Deve atualizar status via PATCH /api/admin/courses/{id}/status")
    void shouldUpdateCourseStatus() throws Exception {
        CourseStatusDto statusDto = new CourseStatusDto(false);
        CourseResponseDto inactiveResponse = new CourseResponseDto(1L, "MEDICINA", "Medicina", "Desc", false, LocalDateTime.now(), LocalDateTime.now());
        when(courseService.updateStatus(eq(1L), any(CourseStatusDto.class))).thenReturn(inactiveResponse);

        mockMvc.perform(patch("/api/admin/courses/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        verify(courseService, times(1)).updateStatus(eq(1L), any(CourseStatusDto.class));
    }

    @Test
    @DisplayName("Deve ativar curso explicitamente via PATCH /api/admin/courses/{id}/activate")
    void shouldActivateCourse() throws Exception {
        when(courseService.activate(1L)).thenReturn(courseResponse);

        mockMvc.perform(patch("/api/admin/courses/1/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        verify(courseService, times(1)).activate(1L);
    }

    @Test
    @DisplayName("Deve desativar curso explicitamente via PATCH /api/admin/courses/{id}/deactivate")
    void shouldDeactivateCourse() throws Exception {
        CourseResponseDto inactiveResponse = new CourseResponseDto(1L, "MEDICINA", "Medicina", "Desc", false, LocalDateTime.now(), LocalDateTime.now());
        when(courseService.deactivate(1L)).thenReturn(inactiveResponse);

        mockMvc.perform(patch("/api/admin/courses/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        verify(courseService, times(1)).deactivate(1L);
    }
}
