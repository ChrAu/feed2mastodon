package com.hexix.ai.bot.telegram;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "telegram_message_log")
public class MessageLog extends PanacheEntity {

    @ManyToOne
    @JoinColumn(name = "subscriber_id", nullable = false)
    private Subscriber subscriber;

    @Column(name = "message_content", length = 4096)
    private String messageContent;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "successfully_sent")
    private boolean successfullySent = false;

    @Column(name = "delivery_timestamp")
    private LocalDateTime deliveryTimestamp;

    // Constructors
    public MessageLog() {
    }

    public MessageLog(Subscriber subscriber, String messageContent) {
        this.subscriber = subscriber;
        this.messageContent = messageContent;
        this.sentAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Subscriber getSubscriber() {
        return subscriber;
    }

    public void setSubscriber(Subscriber subscriber) {
        this.subscriber = subscriber;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public boolean isSuccessfullySent() {
        return successfullySent;
    }

    public void setSuccessfullySent(boolean successfullySent) {
        this.successfullySent = successfullySent;
    }

    public LocalDateTime getDeliveryTimestamp() {
        return deliveryTimestamp;
    }

    public void setDeliveryTimestamp(LocalDateTime deliveryTimestamp) {
        this.deliveryTimestamp = deliveryTimestamp;
    }
}
