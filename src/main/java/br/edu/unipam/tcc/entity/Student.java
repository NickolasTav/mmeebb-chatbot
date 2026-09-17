package br.edu.unipam.tcc.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "tb_student")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "phone_number", nullable = false, unique = true, length = 30)
    private String phoneNumber;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(unique = true, length = 30)
    private String ra;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "preferred_name", length = 30)
    private String preferredName;

    @Builder.Default
    @Column(name = "preferred_study_time", nullable = false)
    private LocalTime preferredStudyTime = LocalTime.of(8, 0);

    @Builder.Default
    @Column(name = "review_notifications_enabled", nullable = false)
    private Boolean reviewNotificationsEnabled = true;

    @Column(name = "last_review_notification_on")
    private LocalDate lastReviewNotificationOn;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Nome usado nas mensagens do bot: o apelido escolhido nas Configurações, senão o
     * primeiro nome do cadastro. O nome completo fica reservado à identificação acadêmica.
     */
    public String displayName() {
        if (preferredName != null && !preferredName.isBlank()) {
            return preferredName.trim();
        }
        if (fullName != null && !fullName.isBlank()) {
            return fullName.trim().split("\\s+")[0];
        }
        return "Estudante";
    }
}
