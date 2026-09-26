package br.edu.unipam.tcc.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Histórico do tutor numa lista Redis por telefone, um JSON por troca. A lista é aparada a cada
 * gravação para as trocas mais recentes, e o TTL renovado faz a conversa expirar sozinha depois
 * de um período de silêncio.
 */
@Slf4j
@Component
public class RedisConversationHistoryStore implements ConversationHistoryStore {

    private static final String KEY_PREFIX = "mmeebb:history:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final int maxTurns;
    private final Duration ttl;

    public RedisConversationHistoryStore(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${mmeebb.tutor.history-max-turns:6}") int maxTurns,
            @Value("${mmeebb.tutor.history-ttl-minutes:30}") long ttlMinutes
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.maxTurns = maxTurns;
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    @Override
    public List<ConversationTurn> recent(String phoneNumber) {
        if (isBlank(phoneNumber)) {
            return List.of();
        }

        List<String> raw = redisTemplate.opsForList().range(buildKey(phoneNumber), 0, -1);
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }

        List<ConversationTurn> turns = new ArrayList<>(raw.size());
        for (String json : raw) {
            try {
                // Um item "null" é lido pelo Jackson como null, sem exceção: descartá-lo aqui.
                ConversationTurn turn = objectMapper.readValue(json, ConversationTurn.class);
                if (turn != null) {
                    turns.add(turn);
                }
            } catch (JsonProcessingException e) {
                // Uma troca ilegível não pode derrubar a dúvida do aluno: o histórico é só contexto.
                log.warn("[ConversationHistory] Troca ilegível ignorada para [{}]: {}", phoneNumber, e.getMessage());
            }
        }
        return turns;
    }

    @Override
    public void append(String phoneNumber, String question, String answer) {
        if (isBlank(phoneNumber)) {
            return;
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(new ConversationTurn(question, answer));
        } catch (JsonProcessingException e) {
            log.warn("[ConversationHistory] Falha ao serializar a troca de [{}]: {}", phoneNumber, e.getMessage());
            return;
        }

        String key = buildKey(phoneNumber);
        redisTemplate.opsForList().rightPush(key, json);
        redisTemplate.opsForList().trim(key, -maxTurns, -1);
        redisTemplate.expire(key, ttl);
    }

    @Override
    public void clear(String phoneNumber) {
        if (isBlank(phoneNumber)) {
            return;
        }
        redisTemplate.delete(buildKey(phoneNumber));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String buildKey(String phoneNumber) {
        return KEY_PREFIX + phoneNumber;
    }
}
