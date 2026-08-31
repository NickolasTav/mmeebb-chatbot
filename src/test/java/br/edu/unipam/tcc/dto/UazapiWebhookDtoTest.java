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
    @DisplayName("Deve deserializar JSON da Uazapi ignorando propriedades desconhecidas")
    void deveDeserializarJsonComPropriedadesDesconhecidas() throws Exception {
        String json = """
                {
                    "remoteJid": "5534999998888@s.whatsapp.net",
                    "fromMe": false,
                    "text": "Quero revisar Cardiologia",
                    "instance": "med-prod",
                    "messageId": "ABCD-1234",
                    "unknownField1": "valor-aleatorio",
                    "metadata": { "status": "DELIVERED" }
                }
                """;

        UazapiWebhookDto dto = objectMapper.readValue(json, UazapiWebhookDto.class);

        assertNotNull(dto);
        assertEquals("5534999998888@s.whatsapp.net", dto.remoteJid());
        assertFalse(dto.fromMe());
        assertEquals("Quero revisar Cardiologia", dto.text());
        assertEquals("med-prod", dto.instance());
        assertEquals("ABCD-1234", dto.messageId());
        assertEquals("5534999998888", dto.getCleanPhoneNumber());
    }
}
