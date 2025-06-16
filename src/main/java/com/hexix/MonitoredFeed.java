package com.hexix;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(indexes = {
        @Index(name = "idx_MonitoredFeed_isActive", columnList = "isActive"),
        @Index(name = "idx_MonitoredFeed_feedUrl", columnList = "feedUrl")
})
public class MonitoredFeed extends PanacheEntity {

    @Column(unique = true, nullable = false, columnDefinition = "TEXT")
    public String feedUrl; // Die URL des RSS-Feeds

    public boolean isActive = true; // Um Feeds einfach zu (de)aktivieren

    @Column(nullable = false)
    public LocalDateTime addDate = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    public String title;

    @Column(columnDefinition = "TEXT")
    public String defaultText;

    public Boolean tryAi;

    // Hilfsmethode, um einen Feed anhand seiner URL zu finden oder null zurückzugeben
    public static MonitoredFeed findByUrl(String url) {
        return find("feedUrl", url).firstResult();
    }
}
