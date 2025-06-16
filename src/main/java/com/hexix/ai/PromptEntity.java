package com.hexix.ai;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(indexes = {
        @Index(name = "idx_PromptEntity_uuid", columnList = "uuid", unique = true),
        @Index(name = "idx_PromptEntity_createdAt", columnList = "createdAt")
})public class PromptEntity extends PanacheEntity {

    @Column(unique = true, nullable = false)
    public String uuid = UUID.randomUUID().toString(); // Der Typ kann direkt UUID sein

    @Column(columnDefinition = "TEXT")
    public String prompt;

    // @CreationTimestamp sorgt dafür, dass die DB oder Hibernate den Zeitstempel beim Erstellen setzt
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    public LocalDateTime createdAt;

    // Der Konstruktor benötigt keine UUID-Zuweisung mehr
    public PromptEntity(final String prompt) {
        this.prompt = prompt;
    }

    // Der leere Konstruktor bleibt für JPA erforderlich
    public PromptEntity() {
    }

    public static PromptEntity findLatest() {
        return find("ORDER BY createdAt DESC").firstResult();
    }
}
