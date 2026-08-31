package br.edu.unipam.tcc.controller;

import br.edu.unipam.tcc.config.AdminApiKeyInterceptor;
import br.edu.unipam.tcc.dto.SeedScheduleRequestDto;
import br.edu.unipam.tcc.dto.SeedScheduleResponseDto;
import br.edu.unipam.tcc.exception.GlobalExceptionHandler;
import br.edu.unipam.tcc.service.StudentAdminService;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminStudentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private StudentAdminService studentAdminService;

    @InjectMocks
    private AdminStudentController studentController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String API_KEY = "test-admin-key";

    @BeforeEach
    void setUp() {
        AdminApiKeyInterceptor interceptor = new AdminApiKeyInterceptor(API_KEY);

        mockMvc = MockMvcBuilders.standaloneSetup(studentController)
                .addInterceptors(interceptor)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Smoke Test: Deve inicializar agendamentos de teste via POST /api/admin/students/by-phone/{phoneNumber}/seed-schedules com api_key")
    void shouldSeedSchedulesForStudent() throws Exception {
        SeedScheduleRequestDto request = new SeedScheduleRequestDto(10L);
        SeedScheduleResponseDto response = new SeedScheduleResponseDto(
                UUID.randomUUID(),
                "5534999998888",
                "Estudante Teste",
                10L,
                "Clínica Médica",
                5
        );

        when(studentAdminService.seedSchedulesForStudent(eq("5534999998888"), any(SeedScheduleRequestDto.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/admin/students/by-phone/5534999998888/seed-schedules")
                        .header("api_key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber").value("5534999998888"))
                .andExpect(jsonPath("$.subjectName").value("Clínica Médica"))
                .andExpect(jsonPath("$.schedulesCreated").value(5));

        verify(studentAdminService, times(1)).seedSchedulesForStudent(eq("5534999998888"), any(SeedScheduleRequestDto.class));
    }

    @Test
    @DisplayName("Deve retornar 401 quando api_key estiver ausente")
    void shouldReturn401WhenApiKeyMissing() throws Exception {
        SeedScheduleRequestDto request = new SeedScheduleRequestDto(10L);

        mockMvc.perform(post("/api/admin/students/by-phone/5534999998888/seed-schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        verify(studentAdminService, never()).seedSchedulesForStudent(any(), any());
    }
}
