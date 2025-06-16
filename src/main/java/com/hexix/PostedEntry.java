package com.hexix;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(uniqueConstraints = {
        // Stellt sicher, dass eine Eintrags-GUID nur einmal pro Feed existieren kann
        @UniqueConstraint(columnNames = {"feed_id", "entryGuid"})}, indexes = {@Index(name = "idx_PostedEntry_entryguid_feedid", columnList = "entryGuid,feed_id")})
public class PostedEntry extends PanacheEntity {

    // Welcher Eintrag wurde gepostet?
    @Column(nullable = false)
    public String entryGuid; // Die eindeutige ID des Eintrags aus dem Feed

    // Wohin wurde er gepostet?
    @Column(nullable = false)
    public String mastodonStatusId; // Die ID des Toots von Mastodon

    // Wann wurde er gepostet?
    public Instant postedAt;

    // Zu welchem Feed gehört dieser Eintrag?
    @ManyToOne(optional = false)
    @JoinColumn(name = "feed_id")
    public MonitoredFeed feed;

    public Boolean aiToot;

    // Leerer Konstruktor für JPA
    public PostedEntry() {
    }
}
