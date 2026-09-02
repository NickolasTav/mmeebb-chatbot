package br.edu.unipam.tcc.controller;

import br.edu.unipam.tcc.config.AdminApiKeyInterceptor;
import br.edu.unipam.tcc.exception.GlobalExceptionHandler;
import br.edu.unipam.tcc.service.KnowledgeIngestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminRagControllerTest {

    private MockMvc mockMvc;

    @Mock
    private KnowledgeIngestionService knowledgeIngestionService;

    @InjectMocks
    private AdminRagController adminRagController;

    private static final String API_KEY = "test-admin-key";

    @BeforeEach
    void setUp() {
        AdminApiKeyInterceptor interceptor = new AdminApiKeyInterceptor(API_KEY);

        mockMvc = MockMvcBuilders.standaloneSetup(adminRagController)
                .addInterceptors(interceptor)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Deve sincronizar flashcards para o RAG com sucesso retornando 200 OK")
    void deveSincronizarFlashcardsComSucesso() throws Exception {
        when(knowledgeIngestionService.ingestFlashcardsAsKnowledge(1L, 10L)).thenReturn(15);

        mockMvc.perform(post("/api/admin/rag/sync-flashcards")
                        .header("X-API-KEY", API_KEY)
                        .param("courseId", "1")
                        .param("subjectId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIngested").value(15))
                .andExpect(jsonPath("$.message").isNotEmpty());

        verify(knowledgeIngestionService).ingestFlashcardsAsKnowledge(1L, 10L);
    }

    @Test
    @DisplayName("Deve rejeitar sincronização quando API key for ausente retornando 401 Unauthorized")
    void deveRejeitarQuandoApiKeyAusente() throws Exception {
        mockMvc.perform(post("/api/admin/rag/sync-flashcards"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deve ingerir diretório com sucesso retornando 200 OK")
    void deveIngerirDiretorioComSucesso() throws Exception {
        when(knowledgeIngestionService.ingestSubjectDocuments(any(Path.class), eq(1L), eq(10L), eq("Cardiologia")))
                .thenReturn(8);

        mockMvc.perform(post("/api/admin/rag/ingest-directory")
                        .header("X-API-KEY", API_KEY)
                        .param("directoryPath", "c:/docs/cardiologia")
                        .param("courseId", "1")
                        .param("subjectId", "10")
                        .param("topic", "Cardiologia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIngested").value(8))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
