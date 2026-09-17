package br.edu.unipam.tcc.messaging;

import br.edu.unipam.tcc.config.RabbitMQConfig;
import br.edu.unipam.tcc.dto.OutgoingMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Porta única de saída de mensagens para o WhatsApp. Todo envio (resposta de chat,
 * configurações e lembrete agendado) passa pela mesma fila sequencial, que aplica o
 * jitter e a presença de digitação da proteção anti-ban.
 */
@Component
@RequiredArgsConstructor
public class OutgoingMessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(String phoneNumber, String messageText) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.OUTGOING_ROUTING_KEY,
                new OutgoingMessageDto(phoneNumber, messageText));
    }
}
