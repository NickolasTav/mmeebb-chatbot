package br.edu.unipam.tcc.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class UazapiWebhookDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Smoke Test: Deve instanciar DTO com campos básicos")
    void deveInstanciarDtoComCamposBasicos() {
        UazapiWebhookDto dto = new UazapiWebhookDto("5534999998888@s.whatsapp.net", false, "Olá bot", "instancia-1", "msg-123");

        assertEquals("5534999998888@s.whatsapp.net", dto.remoteJid());
        assertFalse(dto.fromMe());
        assertEquals("Olá bot", dto.text());
        assertEquals("instancia-1", dto.instance());
        assertEquals("msg-123", dto.messageId());
    }

    @ParameterizedTest(name = "remoteJid ''{0}'' deve extrair telefone limpo ''{1}''")
    @CsvSource({
            "5534999998888@s.whatsapp.net, 5534999998888",
            "5534999998888@c.us, 5534999998888",
            "+55 (34) 99999-8888, 5534999998888",
            "5534999998888, 5534999998888",
            "5534999998888:1@s.whatsapp.net, 5534999998888"
    })
    @DisplayName("Deve extrair apenas dígitos numéricos no método getCleanPhoneNumber")
    void deveExtrairTelefoneLimpoCorretamente(String remoteJid, String expectedCleanPhone) {
        UazapiWebhookDto dto = new UazapiWebhookDto(remoteJid, false, "Teste", null, null);
        assertEquals(expectedCleanPhone, dto.getCleanPhoneNumber());
    }

    @Test
    @DisplayName("Deve retornar string vazia quando remoteJid for nulo ou vazio")
    void deveRetornarStringVaziaQuandoRemoteJidNuloOuVazio() {
        UazapiWebhookDto dtoNulo = new UazapiWebhookDto(null, false, "Teste", null, null);
        assertEquals("", dtoNulo.getCleanPhoneNumber());

        UazapiWebhookDto dtoVazio = new UazapiWebhookDto("   ", false, "Teste", null, null);
        assertEquals("", dtoVazio.getCleanPhoneNumber());
    }

    @Test
    @DisplayName("Smoke Test: Deve instanciar DTO com todos os campos incluindo event e pushName")
    void deveInstanciarDtoComCamposCompletos() {
        UazapiWebhookDto dto = new UazapiWebhookDto(
                "messages.upsert",
                "5534999998888@s.whatsapp.net",
                false,
                "Olá bot",
                "Níckolas Tavares",
                "instancia-1",
                "msg-123"
        );

        assertEquals("messages.upsert", dto.event());
        assertEquals("5534999998888@s.whatsapp.net", dto.remoteJid());
        assertFalse(dto.fromMe());
        assertEquals("Olá bot", dto.text());
        assertEquals("Níckolas Tavares", dto.pushName());
        assertEquals("instancia-1", dto.instance());
        assertEquals("msg-123", dto.messageId());
        assertEquals("5534999998888", dto.getCleanPhoneNumber());
    }

    @Test
    @DisplayName("Deve deserializar JSON da Uazapi com event e pushName ignorando propriedades desconhecidas")
    void deveDeserializarJsonComPropriedadesDesconhecidas() throws Exception {
        String json = """
                {
                    "event": "messages.upsert",
                    "remoteJid": "5534999998888@s.whatsapp.net",
                    "fromMe": false,
                    "text": "Quero revisar Cardiologia",
                    "pushName": "Dr. Lucas",
                    "instance": "med-prod",
                    "messageId": "ABCD-1234",
                    "unknownField1": "valor-aleatorio",
                    "metadata": { "status": "DELIVERED" }
                }
                """;

        UazapiWebhookDto dto = objectMapper.readValue(json, UazapiWebhookDto.class);

        assertNotNull(dto);
        assertEquals("messages.upsert", dto.event());
        assertEquals("5534999998888@s.whatsapp.net", dto.remoteJid());
        assertFalse(dto.fromMe());
        assertEquals("Quero revisar Cardiologia", dto.text());
        assertEquals("Dr. Lucas", dto.pushName());
        assertEquals("med-prod", dto.instance());
        assertEquals("ABCD-1234", dto.messageId());
        assertEquals("5534999998888", dto.getCleanPhoneNumber());
    }

    @Test
    @DisplayName("Deve deserializar payload aninhado nativo da UazapiGO extraindo campos polimorficamente")
    void deveDeserializarPayloadAninhadoUazapiGo() throws Exception {
        String jsonUazapiGo = """
                {
                    "BaseUrl": "https://free.uazapi.com",
                    "EventType": "messages",
                    "chat": {
                        "owner": "5511999990000",
                        "phone": "5511988887777",
                        "wa_chatid": "5511988887777@s.whatsapp.net",
                        "wa_name": "Aluno Teste"
                    },
                    "chatSource": "updated",
                    "instanceName": "instancia-teste",
                    "message": {
                        "chatid": "5511988887777@s.whatsapp.net",
                        "content": "Oi",
                        "fromMe": false,
                        "id": "5511999990000:MSG_ID_TESTE_999",
                        "isGroup": false,
                        "messageid": "MSG_ID_TESTE_999",
                        "senderName": "Aluno Teste",
                        "sender_pn": "5511988887777@s.whatsapp.net",
                        "text": "Oi"
                    },
                    "owner": "5511999990000",
                    "token": "token-fake-123456"
                }
                """;

        UazapiWebhookDto dto = objectMapper.readValue(jsonUazapiGo, UazapiWebhookDto.class);

        assertNotNull(dto);
        assertEquals("messages", dto.event());
        assertEquals("5511988887777@s.whatsapp.net", dto.remoteJid());
        assertFalse(dto.fromMe());
        assertEquals("Oi", dto.text());
        assertEquals("Aluno Teste", dto.pushName());
        assertEquals("instancia-teste", dto.instance());
        assertEquals("MSG_ID_TESTE_999", dto.messageId());
        assertEquals("5511988887777", dto.getCleanPhoneNumber());
    }
}
