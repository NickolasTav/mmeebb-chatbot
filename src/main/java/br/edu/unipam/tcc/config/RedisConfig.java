package br.edu.unipam.tcc.config;

import br.edu.unipam.tcc.session.ChatSessionState;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Configuração do Redis como repositório do estado conversacional (FSM) do chatbot.
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, ChatSessionState> chatSessionRedisTemplate(RedisConnectionFactory connectionFactory) {
        ObjectMapper redisObjectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        RedisTemplate<String, ChatSessionState> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(redisObjectMapper, ChatSessionState.class));
        template.afterPropertiesSet();
        return template;
    }
}
