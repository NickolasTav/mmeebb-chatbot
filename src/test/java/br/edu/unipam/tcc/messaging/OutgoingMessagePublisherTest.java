package br.edu.unipam.tcc.messaging;

import br.edu.unipam.tcc.config.RabbitMQConfig;
import br.edu.unipam.tcc.dto.OutgoingMessageDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutgoingMessagePublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    @DisplayName("Deve publicar a mensagem na fila de saída anti-ban")
    void devePublicarNaFilaDeSaida() {
        new OutgoingMessagePublisher(rabbitTemplate).publish("5534999998888", "Olá!");

        ArgumentCaptor<OutgoingMessageDto> captor = ArgumentCaptor.forClass(OutgoingMessageDto.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME), eq(RabbitMQConfig.OUTGOING_ROUTING_KEY), captor.capture());

        assertEquals("5534999998888", captor.getValue().phoneNumber());
        assertEquals("Olá!", captor.getValue().messageText());
    }
}
