package com.hexix.ai;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Table(name = "gemini_requests")
public class GeminiRequestEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_generator")
    @SequenceGenerator(name = "id_generator", sequenceName = "gemini_requests_id_seq", // WICHTIG: Passe dies an den Namen deiner DB-Sequenz an
            allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "uuid")
    private UUID uuid = UUID.randomUUID();

    @Column(name = "model")
    private String model;

    @Column(name = "created_at")
    private LocalDateTime timestamp = LocalDateTime.now();

    @Column(name = "request_text", columnDefinition = "TEXT")
    private String text;

    @Column(name = "total_token_count")
    private Integer totalTokenCount = 0;

    @Column(name = "response_text", columnDefinition = "TEXT")
    private String response;

    public static long countLast10Minutes(String model) {
        return find("model = ?1 and timestamp > ?2", model, LocalDateTime.now().minusMinutes(10)).count();
    }

    public UUID getUuid() {
        return uuid;
    }

    private void setUuid(final UUID uuid) {
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

    public String getResponse() {
        return response;
    }

    public void setResponse(final String response) {
        this.response = response;
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Integer getTotalTokenCount() {
        return totalTokenCount;
    }

    public void setTotalTokenCount(final int totalTokenCount) {
        this.totalTokenCount = totalTokenCount;
    }

    public void setTotalTokenCount(final Integer totalTokenCount) {
        this.totalTokenCount = totalTokenCount;
    }

    @Override
    public String toString() {
        return "GeminiRequestEntity{" +
                "id=" + id +
                ", uuid=" + uuid +
                ", model='" + model + '\'' +
                ", timestamp=" + timestamp +
                ", text='" + text + '\'' +
                ", totalTokenCount=" + totalTokenCount +
                ", response='" + response + '\'' +
                '}';
    }
}
