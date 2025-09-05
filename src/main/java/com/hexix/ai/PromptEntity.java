package com.hexix.ai;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "prompts")
public class PromptEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_generator")
    @SequenceGenerator(
            name = "id_generator",
            sequenceName = "prompts_id_seq", // WICHTIG: Passe dies an den Namen deiner DB-Sequenz an
            allocationSize = 1
    )
    @Column(name = "id")
    private Long id;

    @Column(name = "uuid")
    private UUID uuid = UUID.randomUUID(); // Der Typ kann direkt UUID sein

    @Column(name = "prompt", columnDefinition = "TEXT")
    private String prompt;

    // @CreationTimestamp sorgt dafür, dass die DB oder Hibernate den Zeitstempel beim Erstellen setzt
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Der Konstruktor benötigt keine UUID-Zuweisung mehr
    public PromptEntity(final String prompt) {
        this.prompt = prompt;
    }

    // Der leere Konstruktor bleibt für JPA erforderlich
    public PromptEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public UUID getUuid() {
        return uuid;
    }

    private void setUuid(final UUID uuid) {
        this.uuid = uuid;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(final String prompt) {
        this.prompt = prompt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static PromptEntity findLatest() {
        return find("ORDER BY createdAt DESC").firstResult();
    }

    @Override
    public String toString() {
        return "PromptEntity{" +
                "id=" + id +
                ", createdAt=" + createdAt +
                ", prompt='" + prompt + '\'' +
                ", uuid='" + uuid + '\'' +
                '}';
    }
}
