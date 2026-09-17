package br.edu.unipam.tcc.controller;

import br.edu.unipam.tcc.config.AdminApiKeyInterceptor;
import br.edu.unipam.tcc.dto.*;
import br.edu.unipam.tcc.entity.SystemConfiguration;
import br.edu.unipam.tcc.exception.GlobalExceptionHandler;
import br.edu.unipam.tcc.repository.SystemConfigurationRepository;
import br.edu.unipam.tcc.service.NgrokTunnelService;
import br.edu.unipam.tcc.service.UazapiInstanceService;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UazapiSetupControllerTest {

    private static final String API_KEY = "test-admin-key";

    private MockMvc mockMvc;

    @Mock
    private NgrokTunnelService ngrokTunnelService;

    @Mock
    private UazapiInstanceService uazapiInstanceService;

    @Mock
    private SystemConfigurationRepository configRepository;

    @InjectMocks
    private UazapiSetupController setupController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        AdminApiKeyInterceptor interceptor = new AdminApiKeyInterceptor(API_KEY);

        mockMvc = MockMvcBuilders.standaloneSetup(setupController)
                .addInterceptors(interceptor)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Smoke Test: Deve retornar status consolidado via GET /api/v1/setup/status com api_key válida")
    void shouldReturnConsolidatedStatus() throws Exception {
        when(configRepository.findByConfigKey("webhook.last-synced-url")).thenReturn(Optional.empty());
        when(ngrokTunnelService.getTunnelStatus()).thenReturn(
                NgrokTunnelDto.online("https://med-tcc.ngrok-free.app", "http://localhost:8080", "http://127.0.0.1:4040"));
        when(uazapiInstanceService.getInstanceStatus()).thenReturn(new UazapiSetupStatusDto.UazapiStatus(
                "https://free.uazapi.com", "med-instance-01", true, "sec***89ab", "open", true, "MMEEBB Bot", null));

        mockMvc.perform(get("/api/v1/setup/status").header("api_key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ngrok.online").value(true))
                .andExpect(jsonPath("$.uazapi.connected").value(true))
                .andExpect(jsonPath("$.uazapi.maskedApiKey").value("sec***89ab"));
    }

    @Test
    @DisplayName("Deve retornar status consolidado com api key mascarada e nunca a chave crua")
    void deveRetornarStatusConsolidadoComApiKeyMascarada() throws Exception {
        when(configRepository.findByConfigKey("webhook.last-synced-url")).thenReturn(Optional.empty());
        when(ngrokTunnelService.getTunnelStatus()).thenReturn(NgrokTunnelDto.offline("Ngrok não detectado."));
        when(uazapiInstanceService.getInstanceStatus()).thenReturn(new UazapiSetupStatusDto.UazapiStatus(
                "https://free.uazapi.com", "med-instance-01", true, "sec***89ab", "close", false, null, null));

        mockMvc.perform(get("/api/v1/setup/status").header("api_key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uazapi.maskedApiKey").value("sec***89ab"))
                .andExpect(jsonPath("$.uazapi.hasApiKey").value(true));
    }

    @Test
    @DisplayName("Deve retornar 401 quando api_key estiver ausente")
    void shouldReturn401WhenApiKeyIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/setup/status"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(ngrokTunnelService, uazapiInstanceService);
    }

    @Test
    @DisplayName("Deve retornar QR Code com sucesso via POST /api/v1/setup/instance/connect")
    void deveRetornarQrCodeComSucesso() throws Exception {
        UazapiConnectRequestDto request = new UazapiConnectRequestDto("https://free.uazapi.com", "med-instance-01", "chave");
        UazapiQrCodeDto response = new UazapiQrCodeDto("CONNECTING", "med-instance-01",
                "data:image/png;base64,iVBORw0KGgo=", "1234-5678", 45, "QR Code gerado.");

        when(uazapiInstanceService.requestQrCode(any(UazapiConnectRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/setup/instance/connect")
                        .header("api_key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONNECTING"))
                .andExpect(jsonPath("$.qrcodeBase64").value("data:image/png;base64,iVBORw0KGgo="));

        verify(uazapiInstanceService, times(1)).requestQrCode(any(UazapiConnectRequestDto.class));
    }

    @Test
    @DisplayName("Deve provisionar nova instância via POST /api/v1/setup/instance/provision")
    void deveProvisionarNovaInstancia() throws Exception {
        when(uazapiInstanceService.provisionInstance()).thenReturn(
                new UazapiProvisionResponseDto(true, "mmeebb-abc123", "nov***gado",
                        "Instância provisionada automaticamente. No servidor demo gratuito ela expira em 1h."));

        mockMvc.perform(post("/api/v1/setup/instance/provision").header("api_key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.instance").value("mmeebb-abc123"));

        verify(uazapiInstanceService, times(1)).provisionInstance();
    }

    @Test
    @DisplayName("Deve sincronizar webhook com a URL do Ngrok quando nenhuma URL for informada")
    void deveSincronizarWebhookComUrlDoNgrok() throws Exception {
        when(ngrokTunnelService.getTunnelStatus()).thenReturn(
                NgrokTunnelDto.online("https://med-tcc.ngrok-free.app", "http://localhost:8080", "http://127.0.0.1:4040"));
        when(uazapiInstanceService.configureWebhook("https://med-tcc.ngrok-free.app/webhook/uazapi")).thenReturn(true);

        mockMvc.perform(post("/api/v1/setup/webhook/sync")
                        .header("api_key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.configuredUrl").value("https://med-tcc.ngrok-free.app/webhook/uazapi"))
                .andExpect(jsonPath("$.events[0]").value("messages.upsert"));

        verify(uazapiInstanceService, times(1)).configureWebhook("https://med-tcc.ngrok-free.app/webhook/uazapi");
    }

    @Test
    @DisplayName("Deve enviar mensagem de teste com status 200")
    void deveEnviarMensagemDeTesteComStatus200() throws Exception {
        SmokeTestRequestDto request = new SmokeTestRequestDto("5534999998888", "Teste de conexão MMEEBB");

        mockMvc.perform(post("/api/v1/setup/test-message")
                        .header("api_key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.targetPhone").value("5534999998888"));

        verify(uazapiInstanceService, times(1)).sendSmokeTestMessage("5534999998888", "Teste de conexão MMEEBB");
    }

    @Test
    @DisplayName("Deve auto-configurar webhook quando o polling detectar instância conectada")
    void deveAutoConfigurarWebhookQuandoInstanciaConectar() throws Exception {
        when(uazapiInstanceService.getInstanceStatus()).thenReturn(new UazapiSetupStatusDto.UazapiStatus(
                "https://free.uazapi.com", "med-instance-01", true, "sec***89ab", "open", true, "MMEEBB Bot", null));
        when(ngrokTunnelService.getTunnelStatus()).thenReturn(
                NgrokTunnelDto.online("https://med-tcc.ngrok-free.app", "http://localhost:8080", "http://127.0.0.1:4040"));
        when(configRepository.findByConfigKey("webhook.last-synced-url")).thenReturn(Optional.empty());
        when(uazapiInstanceService.configureWebhook("https://med-tcc.ngrok-free.app/webhook/uazapi")).thenReturn(true);

        mockMvc.perform(get("/api/v1/setup/instance/status").header("api_key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(true))
                .andExpect(jsonPath("$.webhookAutoConfigured").value(true));

        verify(uazapiInstanceService, times(1)).configureWebhook("https://med-tcc.ngrok-free.app/webhook/uazapi");
    }

    @Test
    @DisplayName("Não deve reconfigurar webhook já sincronizado para a mesma URL")
    void naoDeveReconfigurarWebhookJaSincronizado() throws Exception {
        String syncedUrl = "https://med-tcc.ngrok-free.app/webhook/uazapi";

        when(uazapiInstanceService.getInstanceStatus()).thenReturn(new UazapiSetupStatusDto.UazapiStatus(
                "https://free.uazapi.com", "med-instance-01", true, "sec***89ab", "open", true, "MMEEBB Bot", null));
        when(ngrokTunnelService.getTunnelStatus()).thenReturn(
                NgrokTunnelDto.online("https://med-tcc.ngrok-free.app", "http://localhost:8080", "http://127.0.0.1:4040"));
        when(configRepository.findByConfigKey("webhook.last-synced-url")).thenReturn(Optional.of(
                SystemConfiguration.builder()
                        .configKey("webhook.last-synced-url")
                        .configValue(syncedUrl)
                        .updatedAt(LocalDateTime.now())
                        .build()));

        mockMvc.perform(get("/api/v1/setup/instance/status").header("api_key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.webhookAutoConfigured").value(true));

        verify(uazapiInstanceService, never()).configureWebhook(anyString());
    }
}
