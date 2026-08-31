package br.edu.unipam.tcc.consumer;

import br.edu.unipam.tcc.dto.OutgoingMessageDto;
import br.edu.unipam.tcc.dto.UazapiWebhookDto;
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

    private WhatsappMessageConsumer consumer;

    @BeforeEach
    void setUp() {
        // Em ambiente de teste, typingDelayMs padrão é zero para execução instantânea
        consumer = new WhatsappMessageConsumer(uazapiClientService, 0L);
    }

    @Test
    @DisplayName("Smoke Test: Deve executar ciclo completo de saída (presença composing -> delay -> envio -> presença paused)")
    void deveProcessarMensagemDeSaidaComPresencaEDelay() {
        OutgoingMessageDto outgoingDto = new OutgoingMessageDto(
                "5534999998888",
                "Olá! Esta é a sua questão do MMEEBB hoje.",
                0L
        );

        consumer.consumeOutgoingMessage(outgoingDto);

        InOrder inOrder = inOrder(uazapiClientService);
        inOrder.verify(uazapiClientService).sendPresence("5534999998888", "composing");
        inOrder.verify(uazapiClientService).sendTextMessage("5534999998888", "Olá! Esta é a sua questão do MMEEBB hoje.");
        inOrder.verify(uazapiClientService).sendPresence("5534999998888", "paused");
    }

    @Test
    @DisplayName("Deve ignorar mensagem de saída nula ou com telefone nulo/em branco")
    void deveIgnorarMensagemDeSaidaInvalida() {
        assertDoesNotThrow(() -> consumer.consumeOutgoingMessage(null));
        assertDoesNotThrow(() -> consumer.consumeOutgoingMessage(new OutgoingMessageDto(null, "Texto", 0L)));
        assertDoesNotThrow(() -> consumer.consumeOutgoingMessage(new OutgoingMessageDto("   ", "Texto", 0L)));

        verifyNoInteractions(uazapiClientService);
    }

    @Test
    @DisplayName("Deve consumir mensagem da fila de entrada sem lançar exceção")
    void deveConsumirMensagemDeEntradaComSucesso() {
        UazapiWebhookDto webhookDto = new UazapiWebhookDto(
                "5534999998888@s.whatsapp.net",
                false,
                "A",
                "instancia-tcc",
                "msg-999"
        );

        assertDoesNotThrow(() -> consumer.consumeIncomingMessage(webhookDto));
    }
}
