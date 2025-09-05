package com.hexix;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "posted_entries")
public class PostedEntry extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_generator")
    @SequenceGenerator(
            name = "id_generator",
            sequenceName = "posted_entries_id_seq", // WICHTIG: Passe dies an den Namen deiner DB-Sequenz an
            allocationSize = 1
    )
    @Column(name = "id")
    private Long id;


    // Welcher Eintrag wurde gepostet?
    @Column(name = "entry_guid", nullable = false, columnDefinition = "TEXT")
    private String entryGuid; // Die eindeutige ID des Eintrags aus dem Feed

    // Wohin wurde er gepostet?
    @Column(name = "mastodon_status_id", nullable = false, columnDefinition = "TEXT")
    private String mastodonStatusId; // Die ID des Toots von Mastodon

    // Wann wurde er gepostet?
    @Column(name = "posted_at", nullable = false)
    private Instant postedAt;

    // Zu welchem Feed gehört dieser Eintrag?
    @ManyToOne(optional = false)
    @JoinColumn(name = "feed_id")
    private MonitoredFeed feed;

    @Column(name = "ai_toot")
    private Boolean aiToot;

    // Leerer Konstruktor für JPA
    public PostedEntry() {
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getEntryGuid() {
        return entryGuid;
    }

    public void setEntryGuid(final String entryGuid) {
        this.entryGuid = entryGuid;
    }

    public String getMastodonStatusId() {
        return mastodonStatusId;
    }

    public void setMastodonStatusId(final String mastodonStatusId) {
        this.mastodonStatusId = mastodonStatusId;
    }

    public Instant getPostedAt() {
        return postedAt;
    }

    public void setPostedAt(final Instant postedAt) {
        this.postedAt = postedAt;
    }

    public MonitoredFeed getFeed() {
        return feed;
    }

    public void setFeed(final MonitoredFeed feed) {
        this.feed = feed;
    }

    public boolean getAiToot() {
        return aiToot != null && aiToot;
    }

    public void setAiToot(final boolean aiToot) {
        this.aiToot = aiToot;
    }

    @Override
    public String toString() {
        return "PostedEntry{" +
                "id=" + id +
                ", aiToot=" + aiToot +
                ", feed=" + feed +
                ", postedAt=" + postedAt +
                ", mastodonStatusId='" + mastodonStatusId + '\'' +
                ", entryGuid='" + entryGuid + '\'' +
                '}';
    }
}
