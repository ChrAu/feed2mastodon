package com.hexix.urlshortener;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.time.LocalDateTime;

@Entity
public class UrlMapping extends PanacheEntity {

    @Column(unique = true, nullable = false)
    public String shortKey;

    @Column(length = 2048, nullable = false)
    public String originalUrl;

    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(length = 2048, nullable = false)
    public String shortUrl;

    // Methode, um eine Entität über den Short-Key zu finden
    public static UrlMapping findByShortKey(String shortKey) {
        return find("shortKey", shortKey).firstResult();
    }
}
