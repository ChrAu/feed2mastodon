package com.hexix;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
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

@Entity
@Table(name = "monitored_feeds")
public class MonitoredFeed extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_generator")
    @SequenceGenerator(
            name = "id_generator",
            sequenceName = "monitored_feeds_id_seq", // WICHTIG: Passe dies an den Namen deiner DB-Sequenz an
            allocationSize = 1
    )
    @Column(name = "id")
    private Long id;

    @Column(name = "feed_url", unique = true, nullable = false, columnDefinition = "TEXT")
    private String feedUrl; // Die URL des RSS-Feeds

    @Column(name = "is_active")
    private boolean isActive = true; // Um Feeds einfach zu (de)aktivieren

    @Column(name = "added_at", nullable = false)
    private LocalDateTime addDate = LocalDateTime.now();

    @Column(name = "title", columnDefinition = "TEXT")
    private String title;

    @Column(name = "default_text", columnDefinition = "TEXT")
    private String defaultText;

    @Column(name = "try_ai")
    private Boolean tryAi;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getFeedUrl() {
        return feedUrl;
    }

    public void setFeedUrl(final String feedUrl) {
        this.feedUrl = feedUrl;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(final boolean active) {
        isActive = active;
    }

    public LocalDateTime getAddDate() {
        return addDate;
    }

    public void setAddDate(final LocalDateTime addDate) {
        this.addDate = addDate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getDefaultText() {
        return defaultText;
    }

    public void setDefaultText(final String defaultText) {
        this.defaultText = defaultText;
    }

    public Boolean getTryAi() {
        return tryAi;
    }

    public void setTryAi(final Boolean tryAi) {
        this.tryAi = tryAi;
    }

    // Hilfsmethode, um einen Feed anhand seiner URL zu finden oder null zurückzugeben
    public static MonitoredFeed findByUrl(String url) {
        return find("feedUrl", url).firstResult();
    }

    @Override
    public String toString() {
        return "MonitoredFeed{" +
                "id=" + id + '\'' +
                ", feedUrl='" + feedUrl + '\'' +
                ", isActive=" + isActive +
                ", addDate=" + addDate +
                ", title='" + title + '\'' +
                ", defaultText='" + defaultText + '\'' +
                ", tryAi=" + tryAi +
                '}';
    }
}
