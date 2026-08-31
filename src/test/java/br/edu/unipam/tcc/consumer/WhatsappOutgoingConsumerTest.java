package br.edu.unipam.tcc.consumer;

import br.edu.unipam.tcc.dto.OutgoingMessageDto;
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
class WhatsappOutgoingConsumerTest {

    @Mock
    private UazapiClientService uazapiClientService;

    private WhatsappOutgoingConsumer consumer;

    @BeforeEach
    void setUp() {
        // Inicializa com delays zerados para testes unitários rápidos
        consumer = new WhatsappOutgoingConsumer(uazapiClientService, 0L, 0L, 0L);
    }

    @Test
    @DisplayName("Smoke Test: Deve processar mensagem de saída executando ciclo anti-ban completo")
    void deveProcessarMensagemDeSaidaComCicloAntiBan() {
        OutgoingMessageDto outgoingDto = new OutgoingMessageDto(
                "5534999998888",
                "Olá! Você tem 3 revisões pendentes."
        );

        consumer.consumeOutgoingMessage(outgoingDto);

        InOrder inOrder = inOrder(uazapiClientService);
        inOrder.verify(uazapiClientService).sendPresence("5534999998888", "composing");
        inOrder.verify(uazapiClientService).sendTextMessage("5534999998888", "Olá! Você tem 3 revisões pendentes.");
        inOrder.verify(uazapiClientService).sendPresence("5534999998888", "paused");
    }

    @Test
    @DisplayName("Deve ignorar mensagem nula ou com número de telefone em branco")
    void deveIgnorarMensagemComTelefoneInvalido() {
        assertDoesNotThrow(() -> consumer.consumeOutgoingMessage(null));
        assertDoesNotThrow(() -> consumer.consumeOutgoingMessage(new OutgoingMessageDto(null, "Mensagem")));
        assertDoesNotThrow(() -> consumer.consumeOutgoingMessage(new OutgoingMessageDto("   ", "Mensagem")));

        verifyNoInteractions(uazapiClientService);
    }

    @Test
    @DisplayName("Deve tratar exceção durante o envio de texto e tentar resetar presença sem quebrar")
    void deveTratarExcecaoDuranteEnvioDeTexto() {
        OutgoingMessageDto outgoingDto = new OutgoingMessageDto(
                "5534999997777",
                "Mensagem de teste"
        );

        doThrow(new RuntimeException("Timeout na API Uazapi"))
                .when(uazapiClientService).sendTextMessage("5534999997777", "Mensagem de teste");

        assertDoesNotThrow(() -> consumer.consumeOutgoingMessage(outgoingDto));

        verify(uazapiClientService).sendPresence("5534999997777", "composing");
        verify(uazapiClientService).sendTextMessage("5534999997777", "Mensagem de teste");
        verify(uazapiClientService).sendPresence("5534999997777", "paused");
    }

    @Test
    @DisplayName("Deve executar com atrasos configurados sem erro")
    void deveExecutarComAtrasosConfigurados() {
        // Consumer com delays mínimos de 1ms para testar a rota com sleeps
        WhatsappOutgoingConsumer consumerWithDelay = new WhatsappOutgoingConsumer(
                uazapiClientService, 1L, 2L, 1L
        );

        OutgoingMessageDto outgoingDto = new OutgoingMessageDto(
                "5534999996666",
                "Mensagem com delay"
        );

        assertDoesNotThrow(() -> consumerWithDelay.consumeOutgoingMessage(outgoingDto));

        verify(uazapiClientService).sendPresence("5534999996666", "composing");
        verify(uazapiClientService).sendTextMessage("5534999996666", "Mensagem com delay");
        verify(uazapiClientService).sendPresence("5534999996666", "paused");
    }
}
