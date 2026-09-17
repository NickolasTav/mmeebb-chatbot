package br.edu.unipam.tcc.session;

import br.edu.unipam.tcc.config.RedisConfig;
import br.edu.unipam.tcc.entity.enums.ChatState;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.SerializationException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verificação da ida e volta do estado conversacional por um Redis real.
 * Executado apenas quando há um Redis acessível em REDIS_IT_PORT (padrão 6399);
 * caso contrário os testes são ignorados, mantendo a suíte padrão sem dependência externa.
 */
class RedisChatSessionStoreTest {

    private static final int PORT = Integer.parseInt(System.getenv().getOrDefault("REDIS_IT_PORT", "6399"));

    private static LettuceConnectionFactory connectionFactory;

    private static Optional<RedisChatSessionStore> buildStore() {
        try {
            LettuceConnectionFactory factory =
                    new LettuceConnectionFactory(new RedisStandaloneConfiguration("localhost", PORT));
            factory.afterPropertiesSet();
            factory.getConnection().ping();
            connectionFactory = factory;
            return Optional.of(new RedisChatSessionStore(
                    new RedisConfig().chatSessionRedisTemplate(factory), 30));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @AfterAll
    static void tearDown() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    @DisplayName("Deve persistir e recuperar o estado conversacional completo no Redis")
    void shouldRoundTripSessionState() {
        Optional<RedisChatSessionStore> maybeStore = buildStore();
        org.junit.jupiter.api.Assumptions.assumeTrue(maybeStore.isPresent(),
                "Redis indisponível na porta " + PORT + " — teste ignorado.");

        RedisChatSessionStore store = maybeStore.get();
        String phone = "5534900000001";
        UUID studentId = UUID.randomUUID();

        store.save(ChatSessionState.builder()
                .phoneNumber(phone)
                .studentId(studentId)
                .currentState(ChatState.REVIEW_MODE)
                .selectedCourseId(1L)
                .selectedSubjectId(10L)
                .currentFlashcardId(100L)
                .draftFullName("Maria Silva")
                .build());

        ChatSessionState loaded = store.find(phone).orElseThrow();

        assertEquals(phone, loaded.getPhoneNumber());
        assertEquals(studentId, loaded.getStudentId());
        assertEquals(ChatState.REVIEW_MODE, loaded.getCurrentState());
        assertEquals(10L, loaded.getSelectedSubjectId());
        assertEquals(100L, loaded.getCurrentFlashcardId());
        assertEquals("Maria Silva", loaded.getDraftFullName());
        assertTrue(loaded.isRegistered());
        assertNotNull(loaded.getLastInteractionAt(), "LocalDateTime deve sobreviver à serialização JSON");

        store.delete(phone);
        assertTrue(store.find(phone).isEmpty());
    }

    @Test
    @DisplayName("Deve descartar sessão gravada com um estado que não existe mais e devolver vazio")
    @SuppressWarnings("unchecked")
    void shouldDiscardIncompatibleSession() {
        String key = "mmeebb:session:5534900000009";
        RedisTemplate<String, ChatSessionState> template = mock(RedisTemplate.class);
        ValueOperations<String, ChatSessionState> operations = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(operations);
        when(operations.get(key)).thenThrow(new SerializationException(
                "Cannot deserialize value of type ChatState from String \"SELECTING_COURSE\""));

        RedisChatSessionStore store = new RedisChatSessionStore(template, 30);

        assertTrue(store.find("5534900000009").isEmpty());
        verify(template).delete(key);
    }
}
