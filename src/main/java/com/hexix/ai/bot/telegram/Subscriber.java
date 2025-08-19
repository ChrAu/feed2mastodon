package com.hexix.ai.bot.telegram;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.List;

@Entity
@Table(name = "telegram_subscribers")
public class Subscriber extends PanacheEntity {

    @Column(name = "chat_id", nullable = false, unique = true)
    private String chatId;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    // Standard-Konstruktor für JPA
    public Subscriber() {
    }

    public Subscriber(String chatId) {
        this.chatId = chatId;
        this.active = true;
    }

    // Getter und Setter
    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    // Panache-Methoden
    public static Subscriber findByChatId(String chatId) {
        return find("chatId", chatId).firstResult();
    }

    public static List<Subscriber> findActive() {
        return list("active", true);
    }

    public static List<String> getAllChatIds() {
        return findAll().<Subscriber>stream().map(Subscriber::getChatId).toList();
    }

    public static boolean existsByChatId(String chatId) {
        return count("chatId", chatId) > 0;
    }
}
