package com.hexix.mastodon;

import com.hexix.util.VektorUtil;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Sort;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
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
@Table(name = "embeddings")
public class Embedding extends PanacheEntityBase {


    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_generator")
    @SequenceGenerator(name = "id_generator", sequenceName = "embeddings_id_seq", // WICHTIG: Passe dies an den Namen deiner DB-Sequenz an
            allocationSize = 1)
    @Column(name = "id")
    private Long id;
    /**
     * Ein eindeutiger Identifikator für dieses Embedding-Objekt.
     */
    @Column(name = "uuid")
    private UUID uuid = UUID.randomUUID();

    /**
     * Der Zeitstempel, an dem dieser Entitätseintrag in der Datenbank erstellt wurde.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Ein eindeutiger Bezeichner für die Quelle des Textes (z.B. eine URL oder eine ID aus einem RSS-Feed).
     */
    @Column(name = "resource", columnDefinition = "TEXT", nullable = false)
    private String resource;

    /**
     * Der eigentliche Textinhalt, der für das Embedding verwendet wird.
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "text_id", referencedColumnName = "id")
    private TextEntity text;

    /**
     * Die String-Repräsentation des Embedding-Vektors, der von einem externen Dienst generiert wurde.
     * Wird in der Datenbank persistiert.
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "embedding_vector_string_id", referencedColumnName = "id")
    private TextEntity embeddingVectorString;

    /**
     * Das transiente double-Array des externen Embedding-Vektors.
     * Wird bei Bedarf aus {@link #embeddingVectorString} lazy-geladen.
     */
    @Transient
    private double[] embedding;

    /**
     * Die ID des zugehörigen Status, der auf Mastodon veröffentlicht wurde.
     */
    @Column(name = "mastodon_status_id", columnDefinition = "TEXT")
    private String mastodonStatusId;

    /**
     * Der Zeitstempel, an dem das externe Embedding generiert wurde.
     */
    @Column(name = "embedding_created_at")
    private LocalDateTime embeddingCreatedAt;


    /**
     * Die String-Repräsentation des lokal generierten Embedding-Vektors.
     * Wird in der Datenbank persistiert.
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "local_embedding_vector_string_id", referencedColumnName = "id")
    private TextEntity localEmbeddingVectorString;

    /**
     * Das transiente double-Array des lokalen Embedding-Vektors.
     * Wird bei Bedarf aus {@link #localEmbeddingVectorString} lazy-geladen.
     */
    @Transient
    private double[] localEmbedding;

    /**
     * Der Zeitstempel, an dem das lokale Embedding generiert wurde.
     */
    @Column(name = "local_embedding_created_at")
    private LocalDateTime localEmbeddingCreatedAt;

    /**
     * Die URL, die mit der Ressource verknüpft ist.
     */
    @Column(name = "url", columnDefinition = "TEXT")
    private String url;

    /**
     * Ein optionales negatives Gewicht, das für Scoring- oder Filterzwecke verwendet werden kann.
     */
    @Column(name = "negative_weight")
    private Double negativeWeight = null;

    @Column(name = "status_original_url", columnDefinition = "TEXT")
    private String statusOriginalUrl;

    @Column(name = "local_embedding_model", columnDefinition = "TEXT")
    private String localEmbeddingModel;

    public static Embedding findByUUID(final UUID uuid) {
        return find("uuid = ?1", uuid).firstResult();
    }

    /**
     * Findet eine Liste von Embeddings, die als nächstes von einem externen Dienst verarbeitet werden sollen.
     * Sucht nach Einträgen, bei denen der Text vorhanden ist, aber noch kein externes Embedding erstellt wurde.
     *
     * @return Eine Liste von bis zu 3 {@link Embedding}-Objekten, die auf die Erstellung eines externen Embeddings warten.
     */
    public static List<Embedding> findNextEmbeddings() {
        return find("embeddingCreatedAt is null and text is not null and text.text is not null").range(0, 3).list();
    }

    /**
     * Findet eine Liste von Embeddings, die als nächstes lokal verarbeitet werden sollen.
     * Sucht nach Einträgen, bei denen der Text vorhanden ist, aber noch kein lokales Embedding erstellt wurde.
     *
     * @return Eine Liste von bis zu 3 {@link Embedding}-Objekten, die auf die Erstellung eines lokalen Embeddings warten.
     */
    public static List<Embedding> findNextLocalEmbeddings() {
        return find("localEmbeddingCreatedAt is null and text is not null and text.text is not null", Sort.by("createdAt").descending()).page(0, 10).list();
    }

    /**
     * Findet alle Embeddings, für die bereits ein lokales Embedding generiert wurde.
     *
     * @return Eine Liste aller {@link Embedding}-Objekte mit einem existierenden lokalen Embedding-Vektor.
     */
    public static List<Embedding> findAllLocalEmbeddings() {
        return find("localEmbeddingVectorString is not null").list();
    }

    public static List<Embedding> findAllCalcedEmbeddings() {
        return find("embeddingVectorString is not null and localEmbeddingVectorString is not null and text is not null and  text.text is not null and createdAt < ?1", LocalDateTime.now().minusDays(30)).list();
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(final UUID uuid) {
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

    public TextEntity getText() {
        return text;
    }

    public void setText(final TextEntity text) {
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

    TextEntity getEmbeddingVectorString() {
        return embeddingVectorString;
    }

    TextEntity getLocalEmbeddingVectorString() {
        return localEmbeddingVectorString;
    }

    /**
     * Gibt den externen Embedding-Vektor als double-Array zurück.
     * Wenn der Vektor noch nicht geladen ist, wird er aus der String-Repräsentation {@link #embeddingVectorString} konvertiert.
     *
     * @return Das double-Array des externen Embeddings oder null, wenn keine Daten vorhanden sind.
     */
    public double[] getEmbedding() {
        if (embedding == null && embeddingVectorString != null) {
            this.embedding = VektorUtil.DoubleArrayConverter.stringToArray(embeddingVectorString.getText());
        }
        return embedding;
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
        final String text = VektorUtil.DoubleArrayConverter.arrayToString(embedding);


        if (text != null) {
            this.embeddingCreatedAt = LocalDateTime.now();
            this.embeddingVectorString = new TextEntity(text);
        }
    }

    /**
     * Gibt den lokalen Embedding-Vektor als double-Array zurück.
     * Wenn der Vektor noch nicht geladen ist, wird er aus der String-Repräsentation {@link #localEmbeddingVectorString} konvertiert.
     *
     * @return Das double-Array des lokalen Embeddings oder null, wenn keine Daten vorhanden sind.
     */
    public double[] getLocalEmbedding() {
        if (localEmbedding == null && localEmbeddingVectorString != null) {
            this.localEmbedding = VektorUtil.DoubleArrayConverter.stringToArray(localEmbeddingVectorString.getText());
        }
        return localEmbedding;
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
        final String text = VektorUtil.DoubleArrayConverter.arrayToString(localEmbedding);


        if (text != null) {
            this.localEmbeddingCreatedAt = LocalDateTime.now();
            this.localEmbeddingVectorString = new TextEntity(text);
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

    public String getStatusOriginalUrl() {
        return statusOriginalUrl;
    }

    public void setStatusOriginalUrl(final String statusOriginalUrl) {
        this.statusOriginalUrl = statusOriginalUrl;
    }


    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getLocalEmbeddingModel() {
        return localEmbeddingModel;
    }

    public void setLocalEmbeddingModel(final String localEmbeddingModel) {
        this.localEmbeddingModel = localEmbeddingModel;
    }

    @Override
    public String toString() {
        return "Embedding{" + "id=" + id + ", uuid='" + uuid + '\'' + ", createdAt=" + createdAt + ", resource='" + resource + '\'' + ", text=" + text + ", embeddingVectorString=" + embeddingVectorString + ", embedding=" + Arrays.toString(embedding) + ", mastodonStatusId='" + mastodonStatusId + '\'' + ", embeddingCreatedAt=" + embeddingCreatedAt + ", localEmbeddingVectorString=" + localEmbeddingVectorString + ", localEmbedding=" + Arrays.toString(localEmbedding) + ", localEmbeddingCreatedAt=" + localEmbeddingCreatedAt + ", url='" + url + '\'' + ", negativeWeight=" + negativeWeight + ", statusOriginalUrl='" + statusOriginalUrl + '\'' + ", localEmbeddingModel='" + localEmbeddingModel + '\'' + '}';
    }
}
