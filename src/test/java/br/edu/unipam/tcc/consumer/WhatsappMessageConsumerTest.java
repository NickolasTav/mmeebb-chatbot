package br.edu.unipam.tcc.consumer;

import br.edu.unipam.tcc.dto.OutgoingMessageDto;
import br.edu.unipam.tcc.dto.UazapiWebhookDto;
import br.edu.unipam.tcc.service.ChatFlowOrchestrator;
import br.edu.unipam.tcc.service.UazapiClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WhatsappMessageConsumerTest {

    @Mock
    private UazapiClientService uazapiClientService;

    @Mock
    private ChatFlowOrchestrator chatFlowOrchestrator;

    private WhatsappMessageConsumer consumer;

    @BeforeEach
    void setUp() {
        // Em ambiente de teste, typingDelayMs padrão é zero para execução instantânea
        consumer = new WhatsappMessageConsumer(uazapiClientService, chatFlowOrchestrator, 0L);
    }

    @Test
    @DisplayName("Smoke Test: Deve consumir mensagem de entrada, simular presença e delegar ao orquestrador")
    void deveConsumirMensagemDeEntradaSimularPresencaEDelegarAoOrquestrador() {
        UazapiWebhookDto webhookDto = new UazapiWebhookDto(
                "5534999998888@s.whatsapp.net",
                false,
                "A",
                "instancia-tcc",
                "msg-999"
        );

        consumer.consumeIncomingMessage(webhookDto);

        InOrder inOrder = inOrder(uazapiClientService, chatFlowOrchestrator);
        inOrder.verify(uazapiClientService).sendPresence("5534999998888", "composing");
        inOrder.verify(chatFlowOrchestrator).processIncomingMessage(webhookDto);
        inOrder.verify(uazapiClientService).sendPresence("5534999998888", "paused");
    }

    @Test
    @DisplayName("Deve ignorar mensagem de entrada nula ou sem telefone")
    void deveIgnorarMensagemDeEntradaInvalida() {
        consumer.consumeIncomingMessage(null);
        consumer.consumeIncomingMessage(new UazapiWebhookDto("", false, "Texto", "inst", "msg-1"));

        verifyNoInteractions(uazapiClientService, chatFlowOrchestrator);
    }

    @Test
    @DisplayName("Deve tratar exceção no orquestrador sem propagar erro e garantir envio de presença paused")
    void deveTratarExcecaoNoOrquestradorSemDerrubarConsumer() {
        UazapiWebhookDto webhookDto = new UazapiWebhookDto(
                "5534999998888@s.whatsapp.net",
                false,
                "1",
                "instancia-tcc",
                "msg-1000"
        );

        doThrow(new RuntimeException("Falha de banco de dados simulada"))
                .when(chatFlowOrchestrator).processIncomingMessage(webhookDto);

        assertDoesNotThrow(() -> consumer.consumeIncomingMessage(webhookDto));

        verify(uazapiClientService).sendPresence("5534999998888", "composing");
        verify(uazapiClientService).sendPresence("5534999998888", "paused");
    }
}
