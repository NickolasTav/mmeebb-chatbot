package br.edu.unipam.tcc.consumer;

import br.edu.unipam.tcc.dto.OutgoingMessageDto;
import br.edu.unipam.tcc.messaging.PerRecipientDispatcher;
import br.edu.unipam.tcc.observability.MmeebbMetrics;
import br.edu.unipam.tcc.service.UazapiClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WhatsappOutgoingConsumerTest {

    @Mock
    private UazapiClientService uazapiClientService;

    @Mock
    private MmeebbMetrics mmeebbMetrics;

    private WhatsappOutgoingConsumer consumer;

    @BeforeEach
    void setUp() {
        // Inicializa com delays zerados para testes unitários rápidos
        consumer = new WhatsappOutgoingConsumer(uazapiClientService, mmeebbMetrics, 0L, 0L, 0L);
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
                uazapiClientService, mmeebbMetrics, 1L, 2L, 1L
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

    // ---- uazapi.anti-ban.mode (ritmo por destinatário, PR draft #5) ----

    @Test
    @DisplayName("per-recipient: agenda no despachante com jitter sorteado e não chama a Uazapi na thread do listener")
    void perRecipientAgendaNoDespachante() {
        PerRecipientDispatcher dispatcher = mock(PerRecipientDispatcher.class);
        WhatsappOutgoingConsumer perRecipient = new WhatsappOutgoingConsumer(
                uazapiClientService, mmeebbMetrics, 1_000L, 2_000L, 0L, "per-recipient", dispatcher);

        perRecipient.consumeOutgoingMessage(new OutgoingMessageDto(" 5534999995555 ", "Questão 1"));

        verifyNoInteractions(uazapiClientService);
        ArgumentCaptor<Long> jitter = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Runnable> entrega = ArgumentCaptor.forClass(Runnable.class);
        verify(dispatcher).dispatch(eq("5534999995555"), jitter.capture(), entrega.capture());
        assertTrue(jitter.getValue() >= 1_000L && jitter.getValue() <= 2_000L, "jitter " + jitter.getValue());

        entrega.getValue().run();

        InOrder inOrder = inOrder(uazapiClientService);
        inOrder.verify(uazapiClientService).sendPresence("5534999995555", "composing");
        inOrder.verify(uazapiClientService).sendTextMessage("5534999995555", "Questão 1");
        inOrder.verify(uazapiClientService).sendPresence("5534999995555", "paused");
        verify(mmeebbMetrics).recordUazapiMessage("OUTBOUND");
    }

    @Test
    @DisplayName("per-recipient: o listener não dorme o jitter de produção (3–6 s)")
    void perRecipientNaoDormeNoListener() {
        PerRecipientDispatcher dispatcher = mock(PerRecipientDispatcher.class);
        WhatsappOutgoingConsumer perRecipient = new WhatsappOutgoingConsumer(
                uazapiClientService, mmeebbMetrics, 3_000L, 6_000L, 1_500L, "per-recipient", dispatcher);

        long inicio = System.nanoTime();
        perRecipient.consumeOutgoingMessage(new OutgoingMessageDto("5534999994444", "Oi"));
        long decorridoMs = (System.nanoTime() - inicio) / 1_000_000;

        assertTrue(decorridoMs < 1_000, "listener ficou " + decorridoMs + " ms");
        verify(dispatcher).dispatch(eq("5534999994444"), anyLong(), any(Runnable.class));
    }

    @Test
    @DisplayName("global (padrão): envia na própria thread, como hoje, e nunca usa o despachante")
    void globalNaoUsaDespachante() {
        PerRecipientDispatcher dispatcher = mock(PerRecipientDispatcher.class);
        WhatsappOutgoingConsumer global = new WhatsappOutgoingConsumer(
                uazapiClientService, mmeebbMetrics, 0L, 0L, 0L, "global", dispatcher);

        global.consumeOutgoingMessage(new OutgoingMessageDto("5534999993333", "Menu"));

        verifyNoInteractions(dispatcher);
        InOrder inOrder = inOrder(uazapiClientService);
        inOrder.verify(uazapiClientService).sendPresence("5534999993333", "composing");
        inOrder.verify(uazapiClientService).sendTextMessage("5534999993333", "Menu");
        inOrder.verify(uazapiClientService).sendPresence("5534999993333", "paused");
    }

    @Test
    @DisplayName("Configuração inválida falha na subida: modo desconhecido ou per-recipient sem despachante")
    void configuracaoInvalidaFalhaNaSubida() {
        assertThrows(IllegalArgumentException.class, () -> new WhatsappOutgoingConsumer(
                uazapiClientService, mmeebbMetrics, 0L, 0L, 0L, "turbo", mock(PerRecipientDispatcher.class)));
        assertThrows(IllegalStateException.class, () -> new WhatsappOutgoingConsumer(
                uazapiClientService, mmeebbMetrics, 0L, 0L, 0L, "per-recipient", null));
    }
}
