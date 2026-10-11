package br.edu.unipam.tcc.session;

import br.edu.unipam.tcc.config.RedisConfig;
import br.edu.unipam.tcc.entity.enums.ChatState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * A ordem das alternativas exibidas vive na sessão do Redis. Sessões gravadas antes do
 * embaralhamento (sem o campo) continuam válidas e caem na ordem original.
 */
class ChatSessionStateSerializationTest {

    @SuppressWarnings("unchecked")
    private final RedisSerializer<ChatSessionState> serializer = (RedisSerializer<ChatSessionState>)
            new RedisConfig().chatSessionRedisTemplate(mock(RedisConnectionFactory.class)).getValueSerializer();

    @Test
    @DisplayName("Sessão gravada antes do embaralhamento deve ser lida, com a ordem das alternativas nula")
    void oldSessionWithoutOptionOrderIsStillReadable() {
        String oldJson = """
                {"phoneNumber":"5534999998888","studentId":"6f1c2a52-6d1f-4c43-9f3b-0b9a4f1d2e11",
                 "currentState":"REVIEW_MODE","selectedCourseId":1,"selectedSubjectId":null,
                 "currentFlashcardId":100,"draftFullName":null,"draftRa":null,"draftCourseId":null,
                 "draftAcademicPeriod":null,"lastInteractionAt":[2026,10,11,20,0,0]}""";

        ChatSessionState state = serializer.deserialize(oldJson.getBytes(StandardCharsets.UTF_8));

        assertThat(state).isNotNull();
        assertThat(state.getCurrentFlashcardId()).isEqualTo(100L);
        assertThat(state.getCurrentState()).isEqualTo(ChatState.REVIEW_MODE);
        assertThat(state.getCurrentOptionOrder()).isNull();
    }

    @Test
    @DisplayName("A ordem das alternativas exibidas deve sobreviver à ida e volta pelo Redis")
    void optionOrderSurvivesRoundTrip() {
        ChatSessionState state = ChatSessionState.builder()
                .phoneNumber("5534999998888")
                .currentState(ChatState.REVIEW_MODE)
                .currentFlashcardId(100L)
                .currentOptionOrder(List.of(2, 0, 3, 1))
                .build();

        ChatSessionState read = serializer.deserialize(serializer.serialize(state));

        assertThat(read.getCurrentOptionOrder()).containsExactly(2, 0, 3, 1);
    }

    @Test
    @DisplayName("Limpar o contexto de revisão deve esquecer a questão e a ordem exibida")
    void clearReviewContextForgetsOptionOrder() {
        ChatSessionState state = ChatSessionState.builder()
                .currentFlashcardId(100L)
                .currentOptionOrder(List.of(1, 0, 2, 3))
                .build();

        state.clearReviewContext();

        assertThat(state.getCurrentFlashcardId()).isNull();
        assertThat(state.getCurrentOptionOrder()).isNull();
    }
}
