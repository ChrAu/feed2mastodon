package com.hexix.mastodon;

import com.hexix.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "text_contents")
public class TextEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_generator")
    @SequenceGenerator(
            name = "id_generator",
            sequenceName = "text_contents_id_seq", // WICHTIG: Passe dies an den Namen deiner DB-Sequenz an
            allocationSize = 1
    )
    public Long id;

    @Column(name = "content", columnDefinition = "TEXT")
    public String text;

    /**
     * Standard-Konstruktor für JPA.
     */
    public TextEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * Convenience-Konstruktor zur einfachen Erstellung.
     * @param text Der zu speichernde Text.
     */
    public TextEntity(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "TextEntity{" +
                "id=" + id +
                ", text='" + text + '\'' +
                '}';
    }
}
