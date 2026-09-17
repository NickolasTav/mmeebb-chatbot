package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.dto.UazapiConnectRequestDto;
import br.edu.unipam.tcc.dto.UazapiProvisionResponseDto;
import br.edu.unipam.tcc.dto.UazapiQrCodeDto;
import br.edu.unipam.tcc.dto.UazapiSetupStatusDto;
import br.edu.unipam.tcc.entity.SystemConfiguration;
import br.edu.unipam.tcc.repository.SystemConfigurationRepository;
import br.edu.unipam.tcc.service.impl.UazapiInstanceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@ExtendWith(MockitoExtension.class)
class UazapiInstanceServiceImplTest {

    private static final String BASE_URL = "https://free.uazapi.com";
    private static final String API_KEY = "secret-key-1234";
    private static final String INSTANCE = "med-instance-01";

    @Mock
    private SystemConfigurationRepository configRepository;

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private UazapiInstanceService uazapiInstanceService;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();

        uazapiInstanceService = new UazapiInstanceServiceImpl(
                restClientBuilder, configRepository, BASE_URL, API_KEY, INSTANCE, "");
    }

    @Test
    @DisplayName("Deve obter QR Code base64 com sucesso")
    void deveObterQrCodeBase64ComSucesso() {
        when(configRepository.findByConfigKey(any())).thenReturn(java.util.Optional.empty());

        mockServer.expect(requestTo(BASE_URL + "/instance/connect"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("apikey", API_KEY))
                .andExpect(header("token", API_KEY))
                .andRespond(withSuccess("""
                        {"qrcode": "data:image/png;base64,iVBORw0KGgo=", "pairingCode": "1234-5678"}
                        """, MediaType.APPLICATION_JSON));

        UazapiQrCodeDto result = uazapiInstanceService.requestQrCode(
                new UazapiConnectRequestDto(BASE_URL, INSTANCE, API_KEY));

        assertEquals("CONNECTING", result.status());
        assertEquals("data:image/png;base64,iVBORw0KGgo=", result.qrcodeBase64());
        assertEquals("1234-5678", result.pairingCode());
        mockServer.verify();
    }

    @Test
    @DisplayName("Deve consultar status da instância conectada como open")
    void deveConsultarStatusInstanciaConectadaComoOpen() {
        when(configRepository.findByConfigKey(any())).thenReturn(java.util.Optional.empty());

        mockServer.expect(requestTo(BASE_URL + "/instance/status"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"state": "open", "profileName": "MMEEBB Chatbot"}
                        """, MediaType.APPLICATION_JSON));

        UazapiSetupStatusDto.UazapiStatus result = uazapiInstanceService.getInstanceStatus();

        assertTrue(result.connected());
        assertEquals("open", result.connectionState());
        assertEquals("MMEEBB Chatbot", result.profileName());
        mockServer.verify();
    }

    @Test
    @DisplayName("Deve configurar webhook com URL do Ngrok e eventos upsert")
    void deveConfigurarWebhookComUrlDoNgrokEEvemtosUpsert() {
        when(configRepository.findByConfigKey(any())).thenReturn(java.util.Optional.empty());

        String webhookUrl = "https://med-tcc.ngrok-free.app/webhook/uazapi";

        mockServer.expect(requestTo(BASE_URL + "/webhook"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.url").value(webhookUrl))
                .andExpect(jsonPath("$.events[0]").value("messages.upsert"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andRespond(withSuccess("{\"status\":\"SUCCESS\"}", MediaType.APPLICATION_JSON));

        boolean result = uazapiInstanceService.configureWebhook(webhookUrl);

        assertTrue(result);
        mockServer.verify();
    }

    @Test
    @DisplayName("Deve tratar falha de autenticação com api key inválida sem lançar exceção")
    void deveTratarFalhaDeAutenticacaoComApiKeyInvalida() {
        when(configRepository.findByConfigKey(any())).thenReturn(java.util.Optional.empty());

        mockServer.expect(requestTo(BASE_URL + "/instance/connect"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        UazapiQrCodeDto result = assertDoesNotThrow(() ->
                uazapiInstanceService.requestQrCode(new UazapiConnectRequestDto(BASE_URL, INSTANCE, "chave-invalida")));

        assertEquals("ERROR", result.status());
        assertNull(result.qrcodeBase64());
        mockServer.verify();
    }

    @Test
    @DisplayName("Deve provisionar automaticamente quando não há instância/api-key configuradas")
    void deveProvisionarAutomaticamenteQuandoCredenciaisEstiveremVazias() {
        UazapiInstanceService serviceWithAdminToken = new UazapiInstanceServiceImpl(
                restClientBuilder, configRepository, BASE_URL, "", "", "admin-token-abc");

        java.util.Map<String, SystemConfiguration> store = new java.util.HashMap<>();
        when(configRepository.findByConfigKey(any())).thenAnswer(inv ->
                java.util.Optional.ofNullable(store.get((String) inv.getArgument(0))));
        when(configRepository.save(any(SystemConfiguration.class))).thenAnswer(inv -> {
            SystemConfiguration config = inv.getArgument(0);
            store.put(config.getConfigKey(), config);
            return config;
        });

        mockServer.expect(requestTo(BASE_URL + "/instance/init"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("admintoken", "admin-token-abc"))
                .andRespond(withSuccess("""
                        {"token": "novo-token-gerado", "instance": {"name": "mmeebb-abc123"}}
                        """, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo(BASE_URL + "/instance/connect"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("token", "novo-token-gerado"))
                .andRespond(withSuccess("""
                        {"instance": {"qrcode": "data:image/png;base64,abc=", "paircode": ""}}
                        """, MediaType.APPLICATION_JSON));

        UazapiQrCodeDto result = serviceWithAdminToken.requestQrCode(null);

        assertEquals("CONNECTING", result.status());
        assertEquals("data:image/png;base64,abc=", result.qrcodeBase64());
        verify(configRepository, times(1)).save(argThat(c -> "uazapi.instance".equals(c.getConfigKey())));
        verify(configRepository, times(1)).save(argThat(c -> "uazapi.api-key".equals(c.getConfigKey())
                && "novo-token-gerado".equals(c.getConfigValue())));
        mockServer.verify();
    }

    @Test
    @DisplayName("provisionInstance deve falhar com mensagem clara quando UAZAPI_ADMIN_TOKEN não está configurado")
    void provisionInstanceDeveFalharSemAdminTokenConfigurado() {
        UazapiProvisionResponseDto result = uazapiInstanceService.provisionInstance();

        assertFalse(result.success());
        assertNull(result.instance());
        mockServer.verify();
    }

    @Test
    @DisplayName("provisionInstance deve tratar falha da Uazapi sem lançar exceção")
    void provisionInstanceDeveTratarFalhaDaUazapi() {
        UazapiInstanceService serviceWithAdminToken = new UazapiInstanceServiceImpl(
                restClientBuilder, configRepository, BASE_URL, "", "", "admin-token-invalido");

        when(configRepository.findByConfigKey(any())).thenReturn(java.util.Optional.empty());

        mockServer.expect(requestTo(BASE_URL + "/instance/init"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        UazapiProvisionResponseDto result = assertDoesNotThrow(serviceWithAdminToken::provisionInstance);

        assertFalse(result.success());
        mockServer.verify();
    }
}
