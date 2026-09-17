package br.edu.unipam.tcc.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_configurations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "configKey")
public class SystemConfiguration {

    @Id
    @Column(name = "config_key", nullable = false, length = 100)
    private String configKey;

    @Column(name = "config_value", nullable = false, columnDefinition = "TEXT")
    private String configValue;

    @Column(length = 255)
    private String description;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
