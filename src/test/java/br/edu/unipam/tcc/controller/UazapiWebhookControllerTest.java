package br.edu.unipam.tcc.controller;

import br.edu.unipam.tcc.config.RabbitMQConfig;
import br.edu.unipam.tcc.dto.UazapiWebhookDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UazapiWebhookControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private UazapiWebhookController webhookController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(webhookController).build();
    }

    @Test
    @DisplayName("Smoke Test: Deve responder HTTP 200 e publicar evento válido na fila do RabbitMQ")
    void devePublicarEventoValidoNoRabbitMQERetornarOk() throws Exception {
        UazapiWebhookDto payload = new UazapiWebhookDto(
                "5534999998888@s.whatsapp.net",
                false,
                "Quero estudar Cardiologia",
                "instancia-tcc",
                "msg-001"
        );

        mockMvc.perform(post("/webhook/uazapi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"));

        ArgumentCaptor<UazapiWebhookDto> captor = ArgumentCaptor.forClass(UazapiWebhookDto.class);
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.INCOMING_ROUTING_KEY),
                captor.capture()
        );

        UazapiWebhookDto sentDto = captor.getValue();
        assertEquals("5534999998888@s.whatsapp.net", sentDto.remoteJid());
        assertEquals("Quero estudar Cardiologia", sentDto.text());
        assertEquals("5534999998888", sentDto.getCleanPhoneNumber());
    }

    @Test
    @DisplayName("Deve ignorar mensagens enviadas pelo próprio bot (fromMe == true) e retornar 200 OK")
    void deveIgnorarMensagensFromMe() throws Exception {
        UazapiWebhookDto payload = new UazapiWebhookDto(
                "5534999998888@s.whatsapp.net",
                true,
                "Mensagem disparada pelo bot",
                "instancia-tcc",
                "msg-002"
        );

        mockMvc.perform(post("/webhook/uazapi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IGNORED_FROM_ME"));

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    @DisplayName("Deve ignorar mensagens com texto nulo ou vazio e retornar 200 OK")
    void deveIgnorarMensagensSemTexto() throws Exception {
        UazapiWebhookDto payloadSemTexto = new UazapiWebhookDto(
                "5534999998888@s.whatsapp.net",
                false,
                "   ",
                "instancia-tcc",
                "msg-003"
        );

        mockMvc.perform(post("/webhook/uazapi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payloadSemTexto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IGNORED_EMPTY_TEXT"));

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    @DisplayName("Deve tratar payload nulo com retorno 200 OK sem erro de servidor")
    void deveTratarPayloadNulo() throws Exception {
        mockMvc.perform(post("/webhook/uazapi")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }
}
