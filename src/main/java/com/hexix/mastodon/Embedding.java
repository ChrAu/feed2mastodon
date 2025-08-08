package com.hexix.mastodon;

import com.hexix.util.VektorUtil;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


/**
 * Repräsentiert ein Textelement, das in einen Vektor (Embedding) umgewandelt wurde oder werden soll.
 * Diese Entität speichert den Originaltext, die resultierenden Vektor-Embeddings (sowohl von einem externen Dienst als auch lokal generiert)
 * und Metadaten wie Zeitstempel und zugehörige Mastodon-Status-IDs.
 */
@Entity
@Table(indexes = {
        @Index(name = "idx_Embedding_resource", columnList = "resource", unique = true),
        @Index(name = "idx_Embedding_uuid", columnList = "uuid", unique = true),
        @Index(name = "idx_Embedding_mastodon_status_id", columnList = "mastodon_status_id"),
        @Index(name = "idx_Embedding_embedding_created_at", columnList = "embedding_created_at"),
        @Index(name = "idx_Embedding_local_embedding_created_at", columnList = "local_embedding_created_at")
})
public class Embedding extends PanacheEntity {

    /**
     * Ein eindeutiger Identifikator für dieses Embedding-Objekt.
     */
    @Column(name = "uuid", nullable = false, columnDefinition = "TEXT", unique = true)
    String uuid = UUID.randomUUID().toString();

    /**
     * Der Zeitstempel, an dem dieser Entitätseintrag in der Datenbank erstellt wurde.
     */
    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Ein eindeutiger Bezeichner für die Quelle des Textes (z.B. eine URL oder eine ID aus einem RSS-Feed).
     */
    @Column(name = "resource", columnDefinition = "TEXT", nullable = false)
    String resource;

    /**
     * Der eigentliche Textinhalt, der für das Embedding verwendet wird.
     */
    @Column(name = "text", columnDefinition = "TEXT")
    String text;

    /**
     * Die String-Repräsentation des Embedding-Vektors, der von einem externen Dienst generiert wurde.
     * Wird in der Datenbank persistiert.
     */
    @Column(name = "embedding_vector_string", columnDefinition = "TEXT")
    String embeddingVectorString;

    /**
     * Das transiente double-Array des externen Embedding-Vektors.
     * Wird bei Bedarf aus {@link #embeddingVectorString} lazy-geladen.
     */
    @Transient
    double[] embedding;

    /**
     * Die ID des zugehörigen Status, der auf Mastodon veröffentlicht wurde.
     */
    @Column(name = "mastodon_status_id", columnDefinition = "TEXT")
    String mastodonStatusId;

    /**
     * Der Zeitstempel, an dem das externe Embedding generiert wurde.
     */
    @Column(name = "embedding_created_at")
    LocalDateTime embeddingCreatedAt;


    /**
     * Die String-Repräsentation des lokal generierten Embedding-Vektors.
     * Wird in der Datenbank persistiert.
     */
    @Column(name= "local_embedding_vector_string", columnDefinition = "TEXT")
    String localEmbeddingVectorString;

    /**
     * Das transiente double-Array des lokalen Embedding-Vektors.
     * Wird bei Bedarf aus {@link #localEmbeddingVectorString} lazy-geladen.
     */
    @Transient
    double[] localEmbedding;

    /**
     * Der Zeitstempel, an dem das lokale Embedding generiert wurde.
     */
    @Column(name = "local_embedding_created_at")
    LocalDateTime localEmbeddingCreatedAt;

    /**
     * Die URL, die mit der Ressource verknüpft ist.
     */
    @Column(name = "url", columnDefinition = "TEXT")
    String url;

    /**
     * Ein optionales negatives Gewicht, das für Scoring- oder Filterzwecke verwendet werden kann.
     */
    @Column(name = "negative_weight")
    Double negativeWeight = null;

    @Column(name = "status_original_url", columnDefinition = "TEXT")
    private String statusOriginalUrl;

    public static Embedding findByUUID(final String uuid) {
        return find("uuid = ?1", uuid).firstResult();
    }


    public String getUuid() {
        return uuid;
    }

    public void setUuid(final String uuid) {
        this.uuid = uuid;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(final String resource) {
        this.resource = resource;
    }

    public String getText() {
        return text;
    }

    public void setText(final String text) {
        this.text = text;
    }


    public String getMastodonStatusId() {
        return mastodonStatusId;
    }

    public void setMastodonStatusId(final String mastodonStatusId) {
        this.mastodonStatusId = mastodonStatusId;
    }

    public LocalDateTime getEmbeddingCreatedAt() {
        return embeddingCreatedAt;
    }



    void setEmbeddingCreatedAt(final LocalDateTime embeddingCreatedAt) {
        this.embeddingCreatedAt = embeddingCreatedAt;
    }

    public LocalDateTime getLocalEmbeddingCreatedAt() {
        return localEmbeddingCreatedAt;
    }

    void setLocalEmbeddingCreatedAt(final LocalDateTime localEmbeddingCreatedAt) {
        this.localEmbeddingCreatedAt = localEmbeddingCreatedAt;
    }

    String getEmbeddingVectorString() {
        return embeddingVectorString;
    }

    String getLocalEmbeddingVectorString() {
        return localEmbeddingVectorString;
    }

    /**
     * Gibt den externen Embedding-Vektor als double-Array zurück.
     * Wenn der Vektor noch nicht geladen ist, wird er aus der String-Repräsentation {@link #embeddingVectorString} konvertiert.
     *
     * @return Das double-Array des externen Embeddings oder null, wenn keine Daten vorhanden sind.
     */
    public double[] getEmbedding() {
        if(embedding == null && embeddingVectorString != null){
            this.embedding = VektorUtil.DoubleArrayConverter.stringToArray(embeddingVectorString);
        }
        return embedding;
    }

    /**
     * Gibt den lokalen Embedding-Vektor als double-Array zurück.
     * Wenn der Vektor noch nicht geladen ist, wird er aus der String-Repräsentation {@link #localEmbeddingVectorString} konvertiert.
     *
     * @return Das double-Array des lokalen Embeddings oder null, wenn keine Daten vorhanden sind.
     */
    public double[] getLocalEmbedding() {
        if (localEmbedding == null && localEmbeddingVectorString != null) {
            this.localEmbedding = VektorUtil.DoubleArrayConverter.stringToArray(localEmbeddingVectorString);
        }
        return localEmbedding;
    }

    /**
     * Setzt den externen Embedding-Vektor.
     * Konvertiert das übergebene double-Array in seine String-Repräsentation und speichert es in {@link #embeddingVectorString}.
     * Setzt außerdem den Erstellungszeitstempel {@link #embeddingCreatedAt} auf den aktuellen Zeitpunkt, falls das Embedding gesetzt wird.
     *
     * @param embedding Das double-Array des externen Embeddings.
     */
    public void setEmbedding(final double[] embedding) {
        this.embedding = embedding;
        this.embeddingVectorString = VektorUtil.DoubleArrayConverter.arrayToString(embedding);

        if(this.embeddingVectorString != null){
            this.embeddingCreatedAt = LocalDateTime.now();
        }
    }

    /**
     * Setzt den lokalen Embedding-Vektor.
     * Konvertiert das übergebene double-Array in seine String-Repräsentation und speichert es in {@link #localEmbeddingVectorString}.
     * Setzt außerdem den Erstellungszeitstempel {@link #localEmbeddingCreatedAt} auf den aktuellen Zeitpunkt, falls das Embedding gesetzt wird.
     *
     * @param localEmbedding Das double-Array des lokalen Embeddings.
     */
    public void setLocalEmbedding(final double[] localEmbedding) {
        this.localEmbedding = localEmbedding;
        this.localEmbeddingVectorString = VektorUtil.DoubleArrayConverter.arrayToString(localEmbedding);

        if(this.localEmbeddingVectorString != null){
            this.localEmbeddingCreatedAt = LocalDateTime.now();
        }
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public Double getNegativeWeight() {
        return negativeWeight;
    }

    public void setNegativeWeight(final Double negativeWeight) {
        this.negativeWeight = negativeWeight;
    }

    /**
     * Findet eine Liste von Embeddings, die als nächstes von einem externen Dienst verarbeitet werden sollen.
     * Sucht nach Einträgen, bei denen der Text vorhanden ist, aber noch kein externes Embedding erstellt wurde.
     *
     * @return Eine Liste von bis zu 3 {@link Embedding}-Objekten, die auf die Erstellung eines externen Embeddings warten.
     */
    public static List<Embedding> findNextEmbeddings() {
        return find("embeddingCreatedAt is null and text is not null").range(0, 3).list();
    }

    /**
     * Findet eine Liste von Embeddings, die als nächstes lokal verarbeitet werden sollen.
     * Sucht nach Einträgen, bei denen der Text vorhanden ist, aber noch kein lokales Embedding erstellt wurde.
     *
     * @return Eine Liste von bis zu 3 {@link Embedding}-Objekten, die auf die Erstellung eines lokalen Embeddings warten.
     */
    public static List<Embedding> findNextLocalEmbeddings() {
        return find("localEmbeddingCreatedAt is null and text is not null").range(0,0).list();
    }

    /**
     * Findet alle Embeddings, für die bereits ein lokales Embedding generiert wurde.
     *
     * @return Eine Liste aller {@link Embedding}-Objekte mit einem existierenden lokalen Embedding-Vektor.
     */
    public static List<Embedding> findAllLocalEmbeddings() {
        return find("localEmbeddingVectorString is not null").list();
    }


    public static List<Embedding> findAllCalcedEmbeddings(){
        return find("embeddingVectorString is not null and localEmbeddingVectorString is not null and text is not null and createdAt < ?1", LocalDateTime.now().minusDays(30)).list();
    }

    public String getStatusOriginalUrl() {
        return statusOriginalUrl;
    }

    public void setStatusOriginalUrl(final String statusOriginalUrl) {
        this.statusOriginalUrl = statusOriginalUrl;
    }


    @Override
    public String toString() {
        return "Embedding{" +
                "id='" + id + '\'' +'\'' +
                ", uuid=" + uuid +
                ", createdAt=" + createdAt +
                ", resource='" + resource + '\'' +
                ", text='" + text + '\'' +
                ", embeddingVectorString='" + embeddingVectorString + '\'' +
                ", embedding=" + Arrays.toString(embedding) +
                ", mastodonStatusId='" + mastodonStatusId + '\'' +
                ", embeddingCreatedAt=" + embeddingCreatedAt +
                ", localEmbeddingVectorString='" + localEmbeddingVectorString + '\'' +
                ", localEmbedding=" + Arrays.toString(localEmbedding) +
                ", localEmbeddingCreatedAt=" + localEmbeddingCreatedAt +
                ", url='" + url + '\'' +
                ", negativeWeight=" + negativeWeight +
                ", statusOriginalUrl='" + statusOriginalUrl +
                '}';
    }
}
