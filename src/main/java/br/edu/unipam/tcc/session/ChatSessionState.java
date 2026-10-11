package br.edu.unipam.tcc.session;

import br.edu.unipam.tcc.entity.enums.ChatState;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Estado conversacional volátil mantido no Redis, chaveado pelo número de WhatsApp.
 * Guarda apenas identificadores: as entidades são recarregadas do Postgres quando necessário.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatSessionState implements Serializable {

    private String phoneNumber;
    private UUID studentId;

    @Builder.Default
    private ChatState currentState = ChatState.NEW;

    private Long selectedCourseId;
    private Long selectedSubjectId;
    private Long currentFlashcardId;

    /**
     * Ordem em que as alternativas da questão ativa foram exibidas: para cada posição mostrada, o
     * índice da alternativa original. A correção, o tutor e o reenvio depois de uma dúvida usam esta
     * ordem. Nula em sessão antiga ou questão sem alternativas, e então vale a ordem original.
     */
    private List<Integer> currentOptionOrder;

    private String draftFullName;
    private String draftRa;
    private Long draftCourseId;
    private Integer draftAcademicPeriod;

    @Builder.Default
    private LocalDateTime lastInteractionAt = LocalDateTime.now();

    @JsonIgnore
    public boolean isRegistered() {
        return studentId != null;
    }

    public void clearReviewContext() {
        this.currentFlashcardId = null;
        this.currentOptionOrder = null;
    }

    public void clearOnboardingDraft() {
        this.draftFullName = null;
        this.draftRa = null;
        this.draftCourseId = null;
        this.draftAcademicPeriod = null;
    }
}
