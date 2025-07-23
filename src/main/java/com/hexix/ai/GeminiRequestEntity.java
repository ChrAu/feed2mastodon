package com.hexix.ai;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Table(indexes = {
        @Index(name = "idx_RequestEntity_uuid", columnList = "uuid", unique = true),
        @Index(name = "idx_RequestEntity_model", columnList = "model"),
        @Index(name = "idx_RequestEntity_timestamp", columnList = "timestamp")
})
public class GeminiRequestEntity extends PanacheEntity {

    String uuid = UUID.randomUUID().toString();

    String model;

    LocalDateTime timestamp = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    String text;


    Integer totalTokenCount = 0;
    @Column(columnDefinition = "TEXT")
    String response;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(final String uuid) {
        this.uuid = uuid;
    }

    public String getModel() {
        return model;
    }

    public void setModel(final String model) {
        this.model = model;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    private void setTimestamp(final LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getText() {
        return text;
    }

    public void setText(final String text) {
        this.text = text;
    }

    public static long countLast10Minutes(String model){
        return find("model = ?1 and timestamp > ?2", model, LocalDateTime.now().minusMinutes(10)).count();
    }

    public void setTotalTokenCount(final int totalTokenCount) {
        this.totalTokenCount = totalTokenCount;
    }

    public void setResponse(final String response) {
        this.response = response;
    }

    public String getResponse() {
        return response;
    }
}
