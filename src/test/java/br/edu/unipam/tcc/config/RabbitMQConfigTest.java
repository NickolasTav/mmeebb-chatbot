package br.edu.unipam.tcc.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class RabbitMQConfigTest {

    private final RabbitMQConfig rabbitMQConfig = new RabbitMQConfig();

    @Test
    @DisplayName("Smoke Test: Constantes de Filas, Exchange e Routing Keys devem ser válidas")
    void deveConterConstantesCorretas() {
        assertEquals("whatsapp.exchange", RabbitMQConfig.EXCHANGE_NAME);
        assertEquals("whatsapp.incoming.queue", RabbitMQConfig.INCOMING_QUEUE);
        assertEquals("whatsapp.incoming.key", RabbitMQConfig.INCOMING_ROUTING_KEY);
        assertEquals("whatsapp.outgoing.queue", RabbitMQConfig.OUTGOING_QUEUE);
        assertEquals("whatsapp.outgoing.key", RabbitMQConfig.OUTGOING_ROUTING_KEY);
    }

    @Test
    @DisplayName("Deve configurar DirectExchange com nome whatsapp.exchange")
    void deveCriarExchangeDireta() {
        DirectExchange exchange = rabbitMQConfig.whatsappExchange();

        assertNotNull(exchange);
        assertEquals("whatsapp.exchange", exchange.getName());
        assertTrue(exchange.isDurable());
    }

    @Test
    @DisplayName("Deve criar e configurar Fila de Entrada durable")
    void deveCriarFilaDeEntrada() {
        Queue queue = rabbitMQConfig.incomingQueue();

        assertNotNull(queue);
        assertEquals("whatsapp.incoming.queue", queue.getName());
        assertTrue(queue.isDurable());
    }

    @Test
    @DisplayName("Deve criar e configurar Fila de Saída durable")
    void deveCriarFilaDeSaida() {
        Queue queue = rabbitMQConfig.outgoingQueue();

        assertNotNull(queue);
        assertEquals("whatsapp.outgoing.queue", queue.getName());
        assertTrue(queue.isDurable());
    }

    @Test
    @DisplayName("Deve criar Binding para a Fila de Entrada com a routing key whatsapp.incoming.key")
    void deveCriarBindingDeEntrada() {
        Queue queue = rabbitMQConfig.incomingQueue();
        DirectExchange exchange = rabbitMQConfig.whatsappExchange();
        Binding binding = rabbitMQConfig.incomingBinding(queue, exchange);

        assertNotNull(binding);
        assertEquals("whatsapp.incoming.queue", binding.getDestination());
        assertEquals("whatsapp.exchange", binding.getExchange());
        assertEquals("whatsapp.incoming.key", binding.getRoutingKey());
    }

    @Test
    @DisplayName("Deve criar Binding para a Fila de Saída com a routing key whatsapp.outgoing.key")
    void deveCriarBindingDeSaida() {
        Queue queue = rabbitMQConfig.outgoingQueue();
        DirectExchange exchange = rabbitMQConfig.whatsappExchange();
        Binding binding = rabbitMQConfig.outgoingBinding(queue, exchange);

        assertNotNull(binding);
        assertEquals("whatsapp.outgoing.queue", binding.getDestination());
        assertEquals("whatsapp.exchange", binding.getExchange());
        assertEquals("whatsapp.outgoing.key", binding.getRoutingKey());
    }

    @Test
    @DisplayName("Deve instanciar Jackson2JsonMessageConverter usando ObjectMapper")
    void deveConfigurarJackson2JsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        MessageConverter converter = rabbitMQConfig.jsonMessageConverter(objectMapper);

        assertNotNull(converter);
        assertInstanceOf(Jackson2JsonMessageConverter.class, converter);
    }

    @Test
    @DisplayName("Deve configurar RabbitTemplate com o Jackson2JsonMessageConverter")
    void deveConfigurarRabbitTemplate() {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        ObjectMapper objectMapper = new ObjectMapper();
        MessageConverter converter = rabbitMQConfig.jsonMessageConverter(objectMapper);

        RabbitTemplate template = rabbitMQConfig.rabbitTemplate(connectionFactory, converter);

        assertNotNull(template);
        assertEquals(connectionFactory, template.getConnectionFactory());
        assertEquals(converter, template.getMessageConverter());
    }
}
