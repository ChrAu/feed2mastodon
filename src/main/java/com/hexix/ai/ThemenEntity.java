package com.hexix.ai;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.inject.Inject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "themes")
public class ThemenEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_generator")
    @SequenceGenerator(
            name = "id_generator",
            sequenceName = "themes_id_seq", // WICHTIG: Passe dies an den Namen deiner DB-Sequenz an
            allocationSize = 1
    )
    @Column(name = "id")
    private Long id;

    @Column(name = "uuid")
    private UUID uuid = UUID.randomUUID();

    @Column(name = "theme", columnDefinition = "TEXT", unique = true, nullable = false)
    private String thema;

    @Column(name = "last_post")
    private LocalDate lastPost;

    public UUID getUuid() {
        return uuid;
    }

    private void setUuid(final UUID uuid) {
        this.uuid = uuid;
    }

    public String getThema() {
        return thema;
    }

    public void setThema(final String thema) {
        this.thema = thema;
    }

    public LocalDate getLastPost() {
        return lastPost;
    }

    public void setLastPost(final LocalDate lastPost) {
        this.lastPost = lastPost;
    }


    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "ThemenEntity{" +
                "id=" + id +
                ", uuid=" + uuid +
                ", thema='" + thema + '\'' +
                ", lastPost=" + lastPost +
                '}';
    }
}
