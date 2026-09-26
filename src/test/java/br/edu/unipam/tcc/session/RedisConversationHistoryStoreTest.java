package br.edu.unipam.tcc.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisConversationHistoryStoreTest {

    private static final String PHONE = "5534999998888";
    private static final String KEY = "mmeebb:history:" + PHONE;

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ListOperations<String, String> listOperations;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private RedisConversationHistoryStore store;

    @BeforeEach
    void setUp() {
        store = new RedisConversationHistoryStore(redisTemplate, objectMapper, 6, 30);
    }

    @Test
    @DisplayName("Deve gravar a troca no fim da lista, manter só as 6 mais recentes e renovar o TTL")
    void shouldAppendTrimAndRenewTtl() throws Exception {
        when(redisTemplate.opsForList()).thenReturn(listOperations);

        store.append(PHONE, "o que é sepse?", "Sepse é uma disfunção orgânica...");

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(listOperations).rightPush(eq(KEY), json.capture());
        assertThat(objectMapper.readValue(json.getValue(), ConversationTurn.class))
                .isEqualTo(new ConversationTurn("o que é sepse?", "Sepse é uma disfunção orgânica..."));
        verify(listOperations).trim(KEY, -6, -1);
        verify(redisTemplate).expire(KEY, Duration.ofMinutes(30));
    }

    @Test
    @DisplayName("Deve devolver as trocas na ordem em que foram gravadas")
    void shouldReturnTurnsInInsertionOrder() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range(KEY, 0, -1)).thenReturn(List.of(
                "{\"question\":\"q1\",\"answer\":\"a1\"}",
                "{\"question\":\"q2\",\"answer\":\"a2\"}"));

        assertThat(store.recent(PHONE)).containsExactly(
                new ConversationTurn("q1", "a1"),
                new ConversationTurn("q2", "a2"));
    }

    @Test
    @DisplayName("Deve devolver lista vazia quando não houver conversa")
    void shouldReturnEmptyWhenThereIsNoHistory() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range(KEY, 0, -1)).thenReturn(null);

        assertThat(store.recent(PHONE)).isEmpty();
    }

    @Test
    @DisplayName("Deve ignorar uma troca ilegível sem perder as demais")
    void shouldSkipUnreadableTurn() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range(KEY, 0, -1)).thenReturn(List.of(
                "isso não é json",
                "{\"question\":\"q2\",\"answer\":\"a2\"}"));

        assertThat(store.recent(PHONE)).containsExactly(new ConversationTurn("q2", "a2"));
    }

    @Test
    @DisplayName("Deve apagar toda a conversa do telefone")
    void shouldClearHistory() {
        store.clear(PHONE);

        verify(redisTemplate).delete(KEY);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    @DisplayName("Deve ignorar telefone ausente sem tocar no Redis")
    void shouldIgnoreBlankPhone(String phone) {
        assertThat(store.recent(phone)).isEmpty();
        store.append(phone, "q", "a");
        store.clear(phone);

        verifyNoInteractions(redisTemplate);
    }
}
