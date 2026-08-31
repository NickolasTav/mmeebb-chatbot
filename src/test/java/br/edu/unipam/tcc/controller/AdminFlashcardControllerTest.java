package br.edu.unipam.tcc.controller;

import br.edu.unipam.tcc.config.AdminApiKeyInterceptor;
import br.edu.unipam.tcc.dto.FlashcardRequestDto;
import br.edu.unipam.tcc.dto.FlashcardResponseDto;
import br.edu.unipam.tcc.dto.FlashcardStatusDto;
import br.edu.unipam.tcc.entity.enums.DifficultyLevel;
import br.edu.unipam.tcc.entity.enums.QuestionType;
import br.edu.unipam.tcc.exception.GlobalExceptionHandler;
import br.edu.unipam.tcc.service.FlashcardService;
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
class AdminFlashcardControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FlashcardService flashcardService;

    @InjectMocks
    private AdminFlashcardController flashcardController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private FlashcardResponseDto flashcardResponse;

    private static final String API_KEY = "test-admin-key";

    @BeforeEach
    void setUp() {
        AdminApiKeyInterceptor interceptor = new AdminApiKeyInterceptor(API_KEY);

        mockMvc = MockMvcBuilders.standaloneSetup(flashcardController)
                .addInterceptors(interceptor)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        flashcardResponse = new FlashcardResponseDto(
                100L,
                10L,
                "Clínica Médica",
                "Cardiologia",
                QuestionType.MULTIPLE_CHOICE,
                "Qual a conduta inicial no infarto com supra de ST?",
                "A",
                "[\"A) Dupla antiagregação + Reperfusão imediata\"]",
                "Explicação detalhada",
                DifficultyLevel.HARD,
                "SBC",
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("Smoke Test: Deve listar flashcards via GET /api/admin/flashcards com api_key")
    void shouldListFlashcards() throws Exception {
        when(flashcardService.findAll(10L, true)).thenReturn(List.of(flashcardResponse));

        mockMvc.perform(get("/api/admin/flashcards?subjectId=10&active=true")
                        .header("api_key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100L))
                .andExpect(jsonPath("$[0].topic").value("Cardiologia"));

        verify(flashcardService, times(1)).findAll(10L, true);
    }

    @Test
    @DisplayName("Deve retornar 401 ao buscar flashcards sem api_key")
    void shouldReturn401WhenApiKeyMissing() throws Exception {
        mockMvc.perform(get("/api/admin/flashcards"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        verify(flashcardService, never()).findAll(any(), any());
    }

    @Test
    @DisplayName("Deve buscar flashcard por ID via GET /api/admin/flashcards/{id}")
    void shouldFindFlashcardById() throws Exception {
        when(flashcardService.findById(100L)).thenReturn(flashcardResponse);

        mockMvc.perform(get("/api/admin/flashcards/100")
                        .header("api_key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.topic").value("Cardiologia"));

        verify(flashcardService, times(1)).findById(100L);
    }

    @Test
    @DisplayName("Deve criar flashcard via POST /api/admin/flashcards com status 201 Created")
    void shouldCreateFlashcard() throws Exception {
        FlashcardRequestDto request = new FlashcardRequestDto(
                10L,
                "Cardiologia",
                QuestionType.MULTIPLE_CHOICE,
                "Qual a conduta inicial no infarto com supra de ST?",
                "A",
                "[\"A) Dupla antiagregação + Reperfusão imediata\"]",
                "Explicação detalhada",
                DifficultyLevel.HARD,
                "SBC",
                true
        );

        when(flashcardService.create(any(FlashcardRequestDto.class))).thenReturn(flashcardResponse);

        mockMvc.perform(post("/api/admin/flashcards")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100L));

        verify(flashcardService, times(1)).create(any(FlashcardRequestDto.class));
    }

    @Test
    @DisplayName("Deve atualizar status via PATCH /api/admin/flashcards/{id}/status")
    void shouldUpdateStatus() throws Exception {
        FlashcardStatusDto statusDto = new FlashcardStatusDto(false);
        FlashcardResponseDto inactive = new FlashcardResponseDto(
                100L, 10L, "Clínica Médica", "Cardiologia", QuestionType.MULTIPLE_CHOICE, "Q", "A", null, null, DifficultyLevel.HARD, "SBC", false, LocalDateTime.now(), LocalDateTime.now()
        );
        when(flashcardService.updateStatus(eq(100L), any(FlashcardStatusDto.class))).thenReturn(inactive);

        mockMvc.perform(patch("/api/admin/flashcards/100/status")
                        .header("api_key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        verify(flashcardService, times(1)).updateStatus(eq(100L), any(FlashcardStatusDto.class));
    }

    @Test
    @DisplayName("Deve ativar flashcard via PATCH /api/admin/flashcards/{id}/activate")
    void shouldActivateFlashcard() throws Exception {
        when(flashcardService.activate(100L)).thenReturn(flashcardResponse);

        mockMvc.perform(patch("/api/admin/flashcards/100/activate")
                        .header("api_key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        verify(flashcardService, times(1)).activate(100L);
    }

    @Test
    @DisplayName("Deve desativar flashcard via PATCH /api/admin/flashcards/{id}/deactivate")
    void shouldDeactivateFlashcard() throws Exception {
        FlashcardResponseDto inactive = new FlashcardResponseDto(
                100L, 10L, "Clínica Médica", "Cardiologia", QuestionType.MULTIPLE_CHOICE, "Q", "A", null, null, DifficultyLevel.HARD, "SBC", false, LocalDateTime.now(), LocalDateTime.now()
        );
        when(flashcardService.deactivate(100L)).thenReturn(inactive);

        mockMvc.perform(patch("/api/admin/flashcards/100/deactivate")
                        .header("api_key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        verify(flashcardService, times(1)).deactivate(100L);
    }
}
