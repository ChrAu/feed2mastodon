package de.hexix.homeassistant.ignoredentity;

import jakarta.persistence.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "ignored_entity")
public class IgnoredEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_id", unique = true, nullable = false)
    private String entityId;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    // Constructors
    public IgnoredEntity() {
        this.createdAt = ZonedDateTime.now();
    }

    public IgnoredEntity(String entityId) {
        this.entityId = entityId;
        this.createdAt = ZonedDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
