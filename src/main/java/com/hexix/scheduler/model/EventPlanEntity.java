package com.hexix.scheduler.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(indexes = {
        @Index(name = "idx_EventPlanEntity_uuid", columnList = "uuid", unique = true),
        @Index(name = "idx_EventPlanEntity_createdAt", columnList = "createdAt"),
        @Index(name = "idx_EventPlanEntity_eventType", columnList = "eventType"),
        @Index(name = "idx_EventPlanEntity_scheduledTime", columnList = "scheduledTime"),
        @Index(name = "idx_EventPlanEntity_executed", columnList = "executed")
})
public class EventPlanEntity extends PanacheEntity {

    public String uuid;
    @Enumerated(EnumType.STRING)
    public EventTypeEnum eventType; // e.g., "POST_TO_MASTODON", "CHECK_FEED"
    public LocalDateTime scheduledTime;
    public boolean executed;
    public LocalDateTime createdAt; // Timestamp when the plan was created
    public LocalDateTime executedAt; // Timestamp when the plan was executed
    public String result; // e.g., "SUCCESS", "FAILURE", "SKIPPED"
    public String details; // Additional details, e.g., error message, post ID

    public EventPlanEntity() {
        this.uuid = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.executed = false;
    }

    public EventPlanEntity(EventTypeEnum eventType, LocalDateTime scheduledTime) {
        this();
        this.eventType = eventType;
        this.scheduledTime = scheduledTime;
    }

    public static EventPlanEntity findLatestByEventType(String eventType) {
        return find("eventType = ?1 ORDER BY scheduledTime DESC", eventType).firstResult();
    }

    public static List<EventPlanEntity> findPendingEvents(LocalDateTime now) {
        return list("executed = false AND scheduledTime <= ?1 ORDER BY scheduledTime ASC", now);
    }


}
