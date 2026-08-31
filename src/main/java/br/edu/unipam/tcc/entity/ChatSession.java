package br.edu.unipam.tcc.entity;

import br.edu.unipam.tcc.entity.enums.ChatState;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_chat_session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @Column(name = "phone_number", nullable = false, unique = true, length = 30)
    private String phoneNumber;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "current_state", nullable = false, length = 50)
    private ChatState currentState = ChatState.NEW;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_course_id")
    private Course selectedCourse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_subject_id")
    private Subject selectedSubject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_flashcard_id")
    private Flashcard currentFlashcard;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context_data", columnDefinition = "jsonb")
    private String contextData;

    @Builder.Default
    @Column(name = "last_interaction_at", nullable = false)
    private LocalDateTime lastInteractionAt = LocalDateTime.now();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
