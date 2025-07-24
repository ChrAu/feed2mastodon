package com.hexix.mastodon;

import com.hexix.util.VektorUtil;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "public_mastodon_posts", indexes = {@Index(name = "idx_Embedding_cosDistance", columnList = "cosinus_distance"), @Index(name = "idx_Embedding_create_at", columnList = "create_at")})
public class PublicMastodonPostEntity extends PanacheEntity {

    @Column(name = "mastodon_id", columnDefinition = "TEXT")
    String mastodonId;

    @Column(name = "post_text", columnDefinition = "TEXT")
    String postText;

    @Column(name = "url_text", columnDefinition = "TEXT")
    String urlText;

    @Column(name = "cosinus_distance")
    Double cosDistance;

    @Column(name = "embedding_vector_string", columnDefinition = "TEXT")
    String embeddingVectorString;

    @Transient
    double[] embeddingVector;

    @Column(name = "create_at")
    LocalDateTime create_at = LocalDateTime.now();

    @Column(name = "status_original_url", columnDefinition = "TEXT")
    private String statusOriginalUrl;



    public String getMastodonId() {
        return mastodonId;
    }

    public void setMastodonId(final String mastodonId) {
        this.mastodonId = mastodonId;
    }

    public String getPostText() {
        return postText;
    }

    public void setPostText(final String postText) {
        this.postText = postText;
    }

    public String getUrlText() {
        return urlText;
    }

    public void setUrlText(final String urlText) {
        this.urlText = urlText;
    }

    public Double getCosDistance() {
        return cosDistance;
    }

    public void setCosDistance(final Double cosDistance) {
        this.cosDistance = cosDistance;
    }

    String getEmbeddingVectorString() {
        return embeddingVectorString;
    }

    void setEmbeddingVectorString(final String embeddingVectorString) {
        this.embeddingVectorString = embeddingVectorString;
    }

    public double[] getEmbeddingVector() {
        if (embeddingVector == null && embeddingVectorString != null) {
            embeddingVector = VektorUtil.DoubleArrayConverter.stringToArray(embeddingVectorString);
        }

        return embeddingVector;
    }

    public void setEmbeddingVector(final double[] embeddingVector) {
        this.embeddingVector = embeddingVector;
        embeddingVectorString = VektorUtil.DoubleArrayConverter.arrayToString(embeddingVector);
    }

    public LocalDateTime getCreate_at() {
        return create_at;
    }

    public void setCreate_at(final LocalDateTime create_at) {
        this.create_at = create_at;
    }

    public String getStatusOriginalUrl() {
        return statusOriginalUrl;
    }

    public void setStatusOriginalUrl(final String statusOriginalUrl) {
        this.statusOriginalUrl = statusOriginalUrl;
    }

    public static List<PublicMastodonPostEntity> findNextPublicMastodonPost() {
        return find("embeddingVectorString is null").range(0, 10).list();
    }

    public static List<PublicMastodonPostEntity> findAllComparable() {
        return find("embeddingVectorString is not null and cosDistance is null").list();
    }
}
