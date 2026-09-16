package br.edu.unipam.tcc.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Implementação Redis do estado conversacional. A sessão expira sozinha após o TTL
 * configurado, dispensando rotina de limpeza de sessões abandonadas.
 */
@Slf4j
@Component
public class RedisChatSessionStore implements ChatSessionStore {

    private static final String KEY_PREFIX = "mmeebb:session:";

    private final RedisTemplate<String, ChatSessionState> redisTemplate;
    private final Duration ttl;

    public RedisChatSessionStore(
            RedisTemplate<String, ChatSessionState> redisTemplate,
            @Value("${mmeebb.session.ttl-minutes:60}") long ttlMinutes
    ) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    @Override
    public Optional<ChatSessionState> find(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(redisTemplate.opsForValue().get(buildKey(phoneNumber)));
    }

    @Override
    public void save(ChatSessionState state) {
        if (state == null || state.getPhoneNumber() == null || state.getPhoneNumber().isBlank()) {
            throw new IllegalArgumentException("Estado de sessão sem número de telefone não pode ser persistido.");
        }
        state.setLastInteractionAt(LocalDateTime.now());
        redisTemplate.opsForValue().set(buildKey(state.getPhoneNumber()), state, ttl);
    }

    @Override
    public void delete(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return;
        }
        redisTemplate.delete(buildKey(phoneNumber));
    }

    private String buildKey(String phoneNumber) {
        return KEY_PREFIX + phoneNumber;
    }
}
