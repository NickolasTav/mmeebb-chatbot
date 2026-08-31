package br.edu.unipam.tcc.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração de mensageria assíncrona com RabbitMQ (Padrão Direct Exchange).
 * Define filas de entrada/saída, bindings e conversor JSON Jackson.
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "whatsapp.exchange";
    public static final String INCOMING_QUEUE = "whatsapp.incoming.queue";
    public static final String INCOMING_ROUTING_KEY = "whatsapp.incoming.key";
    public static final String OUTGOING_QUEUE = "whatsapp.outgoing.queue";
    public static final String OUTGOING_ROUTING_KEY = "whatsapp.outgoing.key";

    @Bean
    public DirectExchange whatsappExchange() {
        return new DirectExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue incomingQueue() {
        return QueueBuilder.durable(INCOMING_QUEUE).build();
    }

    @Bean
    public Queue outgoingQueue() {
        return QueueBuilder.durable(OUTGOING_QUEUE).build();
    }

    @Bean
    public Binding incomingBinding(Queue incomingQueue, DirectExchange whatsappExchange) {
        return BindingBuilder.bind(incomingQueue)
                .to(whatsappExchange)
                .with(INCOMING_ROUTING_KEY);
    }

    @Bean
    public Binding outgoingBinding(Queue outgoingQueue, DirectExchange whatsappExchange) {
        return BindingBuilder.bind(outgoingQueue)
                .to(whatsappExchange)
                .with(OUTGOING_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}
