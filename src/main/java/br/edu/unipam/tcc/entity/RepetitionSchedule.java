package br.edu.unipam.tcc.entity;

import br.edu.unipam.tcc.entity.enums.ScheduleStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_repetition_schedule", uniqueConstraints = {
    @UniqueConstraint(name = "uk_schedule_student_flashcard", columnNames = {"student_id", "flashcard_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class RepetitionSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flashcard_id", nullable = false)
    private Flashcard flashcard;

    @Builder.Default
    @Column(name = "n_index", nullable = false)
    private Integer nIndex = 0;

    @Builder.Default
    @Column(name = "interval_days", nullable = false)
    private Integer intervalDays = 1;

    @Builder.Default
    @Column(name = "repetition_count", nullable = false)
    private Integer repetitionCount = 0;

    @Builder.Default
    @Column(name = "consecutive_correct", nullable = false)
    private Integer consecutiveCorrect = 0;

    @Column(name = "last_reviewed_at")
    private LocalDateTime lastReviewedAt;

    @Builder.Default
    @Column(name = "next_review_date", nullable = false)
    private LocalDate nextReviewDate = LocalDate.now();

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ScheduleStatus status = ScheduleStatus.PENDING;

    @Version
    @Column(nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
