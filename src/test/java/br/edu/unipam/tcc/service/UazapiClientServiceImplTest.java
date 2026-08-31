package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.service.impl.UazapiClientServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class UazapiClientServiceImplTest {

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private UazapiClientService uazapiClientService;

    private static final String BASE_URL = "https://free.uazapi.com";
    private static final String API_KEY = "test-secret-key";
    private static final String INSTANCE = "med-instance";

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();

        uazapiClientService = new UazapiClientServiceImpl(
                restClientBuilder,
                BASE_URL,
                API_KEY,
                INSTANCE
        );
    }

    @Test
    @DisplayName("Smoke Test: Deve disparar POST /send/text com payload e headers corretos")
    void deveEnviarMensagemDeTextoComSucesso() {
        mockServer.expect(requestTo("https://free.uazapi.com/send/text"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("apikey", API_KEY))
                .andExpect(header("token", API_KEY))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.number").value("5534999998888"))
                .andExpect(jsonPath("$.text").value("Sua questão MMEEBB de hoje: ..."))
                .andRespond(withSuccess("{\"status\":\"SUCCESS\"}", MediaType.APPLICATION_JSON));

        assertDoesNotThrow(() ->
                uazapiClientService.sendTextMessage("5534999998888", "Sua questão MMEEBB de hoje: ...")
        );

        mockServer.verify();
    }

    @Test
    @DisplayName("Deve disparar POST /send/presence com status 'composing' e headers corretos")
    void deveEnviarPresencaComposingComSucesso() {
        mockServer.expect(requestTo("https://free.uazapi.com/send/presence"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("apikey", API_KEY))
                .andExpect(header("token", API_KEY))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.number").value("5534999998888"))
                .andExpect(jsonPath("$.presence").value("composing"))
                .andRespond(withSuccess("{\"status\":\"SUCCESS\"}", MediaType.APPLICATION_JSON));

        assertDoesNotThrow(() ->
                uazapiClientService.sendPresence("5534999998888", "composing")
        );

        mockServer.verify();
    }

    @Test
    @DisplayName("Deve tratar erro do servidor da Uazapi de forma resiliente sem propagar exception não tratada")
    void deveTratarErroHttpResiliente() {
        mockServer.expect(requestTo("https://free.uazapi.com/send/text"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        mockServer.expect(requestTo("https://free.uazapi.com/message/sendText/med-instance"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        mockServer.expect(requestTo("https://free.uazapi.com/message/sendText"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        assertDoesNotThrow(() ->
                uazapiClientService.sendTextMessage("5534999998888", "Mensagem de teste")
        );

        mockServer.verify();
    }

    @Test
    @DisplayName("Deve ignorar envio quando número ou mensagem forem nulos/em branco")
    void deveIgnorarEnvioComParametrosInvalidos() {
        assertDoesNotThrow(() -> uazapiClientService.sendTextMessage(null, "Texto"));
        assertDoesNotThrow(() -> uazapiClientService.sendTextMessage("5534999998888", null));
        assertDoesNotThrow(() -> uazapiClientService.sendTextMessage("  ", "Texto"));
        assertDoesNotThrow(() -> uazapiClientService.sendTextMessage("5534999998888", "  "));
        assertDoesNotThrow(() -> uazapiClientService.sendPresence(null, "composing"));
        assertDoesNotThrow(() -> uazapiClientService.sendPresence("5534999998888", null));

        mockServer.verify();
    }
}
