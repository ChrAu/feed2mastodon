package com.hexix.mastodon;

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
import java.util.stream.Collectors;


@Entity
@Table(indexes = {@Index(name = "idx_Embedding_resource", columnList = "resource", unique = true)})
public class Embedding extends PanacheEntity {

    @Column(name = "uuid", nullable = false, columnDefinition = "TEXT", unique = true)
    String uuid = UUID.randomUUID().toString();

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "resource", columnDefinition = "TEXT", nullable = false)
    String resource;

    @Column(name = "text", columnDefinition = "TEXT")
    String text;

    @Column(name = "embedding_vector_string", columnDefinition = "TEXT")
    String embeddingVectorString;

    @Transient
    double[] embedding;

    @Column(name = "mastodon_status_id", columnDefinition = "TEXT")
    String mastodonStatusId;

    @Column(name = "embedding_created_at")
    LocalDateTime embeddingCreatedAt;

    @Column(name= "local_embedding_vector_string", columnDefinition = "TEXT")
    String localEmbeddingVectorString;

    @Transient
    double[] localEmbedding;

    @Column(name = "local_embedding_created_at")
    LocalDateTime localEmbeddingCreatedAt;




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

    public double[] getEmbedding() {

        if(embedding == null && embeddingVectorString != null){
            this.embedding = DoubleArrayConverter.stringToArray(embeddingVectorString);
        }

        return embedding;
    }

    public double[] getLocalEmbedding() {
        if (localEmbedding == null && localEmbeddingVectorString != null) {
            this.localEmbedding = DoubleArrayConverter.stringToArray(localEmbeddingVectorString);
        }

        return localEmbedding;
    }

    public void setEmbedding(final double[] embedding) {

        this.embeddingVectorString = DoubleArrayConverter.arrayToString(embedding);

        if(embeddingVectorString != null){
            this.embeddingCreatedAt = LocalDateTime.now();
        }


        this.embedding = embedding;
    }

    public void setLocalEmbedding(final double[] localEmbedding) {
        this.localEmbeddingVectorString = DoubleArrayConverter.arrayToString(localEmbedding);

        if(localEmbeddingVectorString != null){
            this.localEmbeddingCreatedAt = LocalDateTime.now();
        }

        this.localEmbedding = localEmbedding;
    }

    final class DoubleArrayConverter {

        private static final String TRENNZEICHEN = ",";

        // Privater Konstruktor, da dies eine Utility-Klasse ist und nicht instanziiert werden soll.
        private DoubleArrayConverter() {
        }

        /**
         * Wandelt ein double-Array in einen einzelnen, durch Kommas getrennten String um.
         *
         * @param daten Das Array, das konvertiert werden soll.
         * @return Ein String, der die Array-Elemente darstellt (z.B. "1.5,-3.14,10.0").
         * Gibt null zurück, wenn das Eingabe-Array null ist.
         */
        public static String arrayToString(double[] daten) {
            if (daten == null) {
                return null;
            }
            // Nutzt Java Streams, um jeden double in einen String zu wandeln und mit dem Trennzeichen zu verbinden.
            return Arrays.stream(daten)
                    .mapToObj(String::valueOf)
                    .collect(Collectors.joining(TRENNZEICHEN));
        }

        /**
         * Wandelt einen durch Kommas getrennten String zurück in ein double-Array.
         *
         * @param datenString Der String, der konvertiert werden soll (z.B. "1.5,-3.14,10.0").
         * @return Ein double-Array mit den Werten aus dem String.
         * Gibt null zurück, wenn der Eingabe-String null ist.
         * Gibt ein leeres Array zurück, wenn der Eingabe-String leer ist.
         * @throws NumberFormatException Wenn ein Teil des Strings keine gültige Zahl ist.
         */
        public static double[] stringToArray(String datenString) {
            if (datenString == null) {
                return null;
            }
            if (datenString.isEmpty()) {
                return new double[0];
            }
            // Teilt den String am Trennzeichen und wandelt jeden Teil zurück in einen double.
            return Arrays.stream(datenString.split(TRENNZEICHEN))
                    .mapToDouble(Double::parseDouble)
                    .toArray();
        }
    }


    public static List<Embedding> findNextEmbeddings() {
        return find("embeddingCreatedAt is null").range(0, 3).list();
    }

    public static List<Embedding> findNextLocalEmbeddings() {
        return find("localEmbeddingCreatedAt is null").range(0,10).list();
    }
}
