package br.edu.unipam.tcc.entity;

import br.edu.unipam.tcc.entity.enums.TopicMastery;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Registro imutável de uma tentativa de revisão. É a memória de desempenho do sistema: sem ela,
 * a correção semântica feita pela IA se perderia assim que o índice N fosse atualizado.
 *
 * <p>Guardar o N antes e depois junto com o domínio do tópico permite reconstruir, na monografia,
 * por que cada intervalo foi escolhido — inclusive depois que os limiares de configuração mudarem.
 */
@Entity
@Table(name = "tb_review_attempt")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class ReviewAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flashcard_id", nullable = false)
    private Flashcard flashcard;

    @Column(nullable = false)
    private Boolean correct;

    @Column(name = "n_index_before", nullable = false)
    private Integer nIndexBefore;

    @Column(name = "n_index_after", nullable = false)
    private Integer nIndexAfter;

    @Column(name = "interval_days_after", nullable = false)
    private Integer intervalDaysAfter;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "topic_mastery", nullable = false, length = 20)
    private TopicMastery topicMastery = TopicMastery.SEM_DADOS;

    @Column(name = "answered_at", nullable = false)
    private LocalDateTime answeredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
