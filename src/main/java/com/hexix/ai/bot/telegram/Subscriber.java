package com.hexix.ai.bot.telegram;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.util.List;

@Entity
@Table(name = "telegram_subscribers")
public class Subscriber extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_generator")
    @SequenceGenerator(
            name = "id_generator",
            sequenceName = "telegram_subscribers_id_seq", // WICHTIG: Passe dies an den Namen deiner DB-Sequenz an
            allocationSize = 1
    )
    @Column(name = "id")
    private Long id;

    @Column(name = "chat_id", nullable = false, unique = true, columnDefinition = "TEXT")
    private String chatId;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    // Standard-Konstruktor für JPA
    public Subscriber() {
    }

    public Subscriber(String chatId) {
        this.chatId = chatId;
        this.active = true;
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

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

    @Override
    public String toString() {
        return "Subscriber{" +
                "id=" + id +
                ", chatId='" + chatId + '\'' +
                ", active=" + active +
                '}';
    }
}
