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
    }

    public void clearOnboardingDraft() {
        this.draftFullName = null;
        this.draftRa = null;
        this.draftCourseId = null;
        this.draftAcademicPeriod = null;
    }
}
