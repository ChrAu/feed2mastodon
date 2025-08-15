package com.hexix.ai;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.inject.Inject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(indexes = {
        @Index(name = "idx_ThemenEntity_uuid", columnList = "uuid", unique = true),
        @Index(name = "idx_ThemenEntity_thema", columnList = "thema", unique = true),
})
public class ThemenEntity extends PanacheEntity {

    @Column(name = "uuid", unique = true, nullable = false)
    String uuid = UUID.randomUUID().toString();

    @Column(name = "thema", columnDefinition = "TEXT", unique = true, nullable = false)
    String thema;

    @Column(name = "last")
    LocalDate lastPost;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(final String uuid) {
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
}
