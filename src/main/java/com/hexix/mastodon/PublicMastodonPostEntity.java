package com.hexix.mastodon;

import com.hexix.util.VektorUtil;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.panache.common.Sort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "public_mastodon_posts", indexes = {@Index(name = "idx_Embedding_cosDistance", columnList = "cosinus_distance"), @Index(name = "idx_Embedding_create_at", columnList = "create_at"), @Index(name = "idx_Embedding_mastodonId", columnList = "mastodon_id", unique = true)})
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
    LocalDateTime createAt = LocalDateTime.now();

    @Column(name = "status_original_url", columnDefinition = "TEXT")
    private String statusOriginalUrl;

    @Column(name = "negative_weight")
    private Double negativeWeight;

    @Column(name = "no_url")
    private Boolean noURL;

    // Dieses Feld speichert, ob Viki bereits einen Kommentar zu diesem Post generiert hat.
    @Column(name = "viki_commented")
    private Boolean vikiCommented = false;

    @Column(name = "embedding_model", columnDefinition = "TEXT")
    private String localModel;


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

    public LocalDateTime getCreateAt() {
        return createAt;
    }

    public void setCreate_at(final LocalDateTime createAt) {
        this.createAt = createAt;
    }

    public String getStatusOriginalUrl() {
        return statusOriginalUrl;
    }

    public void setStatusOriginalUrl(final String statusOriginalUrl) {
        this.statusOriginalUrl = statusOriginalUrl;
    }

    public void setNegativeWeight(final Double negativeWeight) {
        this.negativeWeight = negativeWeight;
    }

    public Double getNegativeWeight() {
        return negativeWeight;
    }

    public boolean isVikiCommented() {
        return vikiCommented;
    }

    public void setVikiCommented(final boolean vikiCommented) {
        this.vikiCommented = vikiCommented;
    }

    /**
     * Finds the next 10 PublicMastodonPostEntity objects that do not have an embedding vector string.
     * @return A list of PublicMastodonPostEntity objects matching the criteria.
     */
    public static List<PublicMastodonPostEntity> findNextPublicMastodonPost() {
        return find("embeddingVectorString is null and ( postText is not null or urlText is not null)", Sort.by("createAt").descending()).range(0, 10).list();
    }

    /**
     * Finds all PublicMastodonPostEntity objects that have an embedding vector string but no cosine distance.
     * @return A list of PublicMastodonPostEntity objects matching the criteria.
     */
    public static List<PublicMastodonPostEntity> findAllComparable() {
        return find("embeddingVectorString is not null and cosDistance is null").list();
    }

    /**
     * Finds a PublicMastodonPostEntity object by its mastodonId.
     * @param id The mastodonId of the PublicMastodonPostEntity to find.
     * @return The PublicMastodonPostEntity object with the specified mastodonId, or null if not found.
     */
    public static PublicMastodonPostEntity findByMastodonId(final String id) {
        return find("mastodonId", id).firstResult();
    }

    /**
     * Finds all PublicMastodonPostEntity objects that have a negative weight and an embedding vector string.
     * @return A list of PublicMastodonPostEntity objects matching the criteria.
     */
    public static List<PublicMastodonPostEntity> findAllNegativPosts() {
        return find("negativeWeight is not null and embeddingVectorString is not null").list();
    }


    /**
     * Finds all PublicMastodonPostEntity objects that have a calculated embedding and are older than 2 days.
     * @return A list of PublicMastodonPostEntity objects matching the criteria.
     */
    public static List<PublicMastodonPostEntity> findAllCalcedEmbeddings(){
        return find("embeddingVectorString is not null and (postText is not null or urlText is not null) and createAt< ?1", LocalDateTime.now().minusDays(2)).list();
    }


    /**
     * Finds all PublicMastodonPostEntity objects that have no embedding vector string,
     * no post text, and no URL text.
     * @return A list of PublicMastodonPostEntity objects matching the criteria.
     */

    public static List<PublicMastodonPostEntity> findAllNoEmbeddingAndText() {
        return find("embeddingVectorString is null and (postText is null or postText = '') and urlText is null").<PublicMastodonPostEntity>stream().limit(15).toList();
    }



    public void removeEmbeddingVektor() {
        embeddingVector = null;
        embeddingVectorString = null;
    }

    public void setNoURL(final boolean noURL) {
        this.noURL = noURL;
    }

    public Boolean isNoURL() {
        return noURL;
    }

    public void setEmbeddingModel(final String localModel) {
        this.localModel = localModel;
    }

    public String getEmbeddingModel() {
        return localModel;
    }
}
