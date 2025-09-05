package com.hexix.scheduler.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "event_plans")
public class EventPlanEntity extends PanacheEntityBase {


    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_generator")
    @SequenceGenerator(
            name = "id_generator",
            sequenceName = "event_plans_id_seq", // WICHTIG: Passe dies an den Namen deiner DB-Sequenz an
            allocationSize = 1
    )
    @Column(name = "id")
    private Long id;

    @Column(name = "uuid")
    private UUID uuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", columnDefinition = "event_type")
    private EventTypeEnum eventType; // e.g., "POST_TO_MASTODON", "CHECK_FEED"

    @Column(name = "scheduled_time")
    private LocalDateTime scheduledTime;

    @Column(name = "is_executed")
    private boolean executed;

    @Column(name = "created_at")
    private LocalDateTime createdAt; // Timestamp when the plan was created

    @Column(name = "executed_at")
    private LocalDateTime executedAt; // Timestamp when the plan was executed

    @Column(name = "result")
    private String result; // e.g., "SUCCESS", "FAILURE", "SKIPPED"

    @Column(name = "details")
    private String details; // Additional details, e.g., error message, post ID

    public EventPlanEntity() {
        this.uuid = UUID.randomUUID();
        this.createdAt = LocalDateTime.now();
        this.executed = false;
    }

    public EventPlanEntity(EventTypeEnum eventType, LocalDateTime scheduledTime) {
        this();
        this.eventType = eventType;
        this.scheduledTime = scheduledTime;
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

    public EventTypeEnum getEventType() {
        return eventType;
    }

    public void setEventType(final EventTypeEnum eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(final LocalDateTime scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public boolean isExecuted() {
        return executed;
    }

    public void setExecuted(final boolean executed) {
        this.executed = executed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(final LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }

    public String getResult() {
        return result;
    }

    public void setResult(final String result) {
        this.result = result;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(final String details) {
        this.details = details;
    }

    public static EventPlanEntity findLatestByEventType(String eventType) {
        return find("eventType = ?1 ORDER BY scheduledTime DESC", eventType).firstResult();
    }

    public static List<EventPlanEntity> findPendingEvents(LocalDateTime now) {
        return list("executed = false AND scheduledTime <= ?1 ORDER BY scheduledTime ASC", now);
    }


    @Override
    public String toString() {
        return "EventPlanEntity{" +
                "id=" + id +
                ", uuid=" + uuid +
                ", eventType=" + eventType +
                ", scheduledTime=" + scheduledTime +
                ", executed=" + executed +
                ", createdAt=" + createdAt +
                ", executedAt=" + executedAt +
                ", result='" + result + '\'' +
                ", details='" + details + '\'' +
                '}';
    }
}
