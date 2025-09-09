package com.hexix.mastodon;

import com.hexix.BaseEntity;
import com.hexix.util.VektorUtil;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.util.Arrays;

@Entity
@Table(name = "mastodon_posts")
@NamedQueries({
        @NamedQuery(
                name = PublicMastodonPostEntity.FIND_ALL,
                query = "SELECT p FROM PublicMastodonPostEntity p ORDER BY p.createdAt DESC"
        ),
        @NamedQuery(
                name = PublicMastodonPostEntity.FIND_NEXT_PUBLIC_MASTODON_POST,
                query = "SELECT p FROM PublicMastodonPostEntity p " +
                        "LEFT JOIN p.postText pt " +
                        "LEFT JOIN p.urlText ut " +
                        "WHERE p.embeddingVectorString IS NULL AND (pt.text IS NOT NULL OR ut.text IS NOT NULL) ORDER BY p.createdAt DESC"
        ),
        @NamedQuery(
                name = PublicMastodonPostEntity.FIND_ALL_COMPARABLE,
                query = "SELECT p FROM PublicMastodonPostEntity p WHERE p.embeddingVectorString IS NOT NULL AND p.cosDistance IS NULL"
        ),
        @NamedQuery(
                name = PublicMastodonPostEntity.FIND_BY_MASTODON_ID,
                query = "SELECT p FROM PublicMastodonPostEntity p WHERE p.mastodonId = :" + PublicMastodonPostEntity.PARAM_MASTODON_ID
        ),
        @NamedQuery(
                name = PublicMastodonPostEntity.FIND_ALL_NEGATIVE_POSTS,
                query = "SELECT p FROM PublicMastodonPostEntity p WHERE p.negativeWeight IS NOT NULL AND p.embeddingVectorString IS NOT NULL"
        ),
        @NamedQuery(
                name = PublicMastodonPostEntity.FIND_ALL_CALCED_EMBEDDINGS,
                query = "SELECT p FROM PublicMastodonPostEntity p " +
                        "LEFT JOIN p.postText pt " +
                        "LEFT JOIN p.urlText ut " +
                        "WHERE p.embeddingVectorString IS NOT NULL AND (pt.text IS NOT NULL OR ut.text IS NOT NULL) AND p.createdAt < :date"
        ),
        @NamedQuery(
                name = PublicMastodonPostEntity.FIND_ALL_NO_EMBEDDING_AND_TEXT,
                query = "SELECT p FROM PublicMastodonPostEntity p " +
                        "LEFT JOIN p.postText pt " +
                        "LEFT JOIN p.urlText ut " +
                        "WHERE p.embeddingVectorString IS NULL AND (pt.text IS NULL OR ut.text IS NULL) AND p.urlText IS NULL"
        ),
        @NamedQuery(
                name = PublicMastodonPostEntity.FIND_BY_NO_VIKI_COMMENT,
                query = "SELECT p from PublicMastodonPostEntity p WHERE p.vikiCommented = false and p.cosDistance IS NOT NULL ORDER BY p.cosDistance DESC LIMIT 1"
        )
})
public class PublicMastodonPostEntity extends BaseEntity {

    // Named Query Constants
    public static final String FIND_ALL = "PublicMastodonPostEntity.findAll";
    public static final String FIND_NEXT_PUBLIC_MASTODON_POST = "PublicMastodonPostEntity.findNextPublicMastodonPost";
    public static final String FIND_ALL_COMPARABLE = "PublicMastodonPostEntity.findAllComparable";
    public static final String FIND_BY_MASTODON_ID = "PublicMastodonPostEntity.findByMastodonId";
    public static final String FIND_ALL_NEGATIVE_POSTS = "PublicMastodonPostEntity.findAllNegativPosts";
    public static final String FIND_ALL_CALCED_EMBEDDINGS = "PublicMastodonPostEntity.findAllCalcedEmbeddings";
    public static final String FIND_ALL_NO_EMBEDDING_AND_TEXT = "PublicMastodonPostEntity.findAllNoEmbeddingAndText";

    // Parameter Constants
    public static final String PARAM_MASTODON_ID = "mastodonId";
    public static final String FIND_BY_NO_VIKI_COMMENT = "PublicMastodonPostEntity.findByNoVikiComment";


    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_generator")
    @SequenceGenerator(
            name = "id_generator",
            sequenceName = "mastodon_posts_id_seq", // WICHTIG: Passe dies an den Namen deiner DB-Sequenz an
            allocationSize = 1
    )
    @Column(name = "id")
    public Long id;

    @Column(name = "mastodon_id", columnDefinition = "TEXT")
    String mastodonId;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "post_text_id", referencedColumnName = "id")
    TextEntity postText;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "url_text_id", referencedColumnName = "id")
    public TextEntity urlText;

    @Column(name = "cosinus_distance")
    Double cosDistance;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "embedding_id", referencedColumnName = "id")
    TextEntity embeddingVectorString;

    @Transient
    double[] embeddingVector;

    @Column(name = "status_original_url", columnDefinition = "TEXT")
    private String statusOriginalUrl;

    @Column(name = "negative_weight")
    private Double negativeWeight;

    @Column(name = "no_url")
    private Boolean noURL;

    // Dieses Feld speichert, ob Viki bereits einen Kommentar zu diesem Post generiert hat.
    @Column(name = "viki_commented")
    private Boolean vikiCommented = false;

    @Column(name = "intern_mastodon_url", columnDefinition = "TEXT")
    private String internMastodonUrl;


    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    @Column(name = "embedding_model", columnDefinition = "TEXT")
    private String localModel;


    public String getMastodonId() {
        return mastodonId;
    }

    public void setMastodonId(final String mastodonId) {
        this.mastodonId = mastodonId;
    }

    public TextEntity getPostText() {
        return postText;
    }

    public void setPostText(final TextEntity postText) {
        this.postText = postText;
    }

    public TextEntity getUrlText() {
        return urlText;
    }

    public void setUrlText(final TextEntity urlText) {
        this.urlText = urlText;
    }

    public Double getCosDistance() {
        return cosDistance;
    }

    public void setCosDistance(final Double cosDistance) {
        this.cosDistance = cosDistance;
    }

    TextEntity getEmbeddingVectorString() {
        return embeddingVectorString;
    }

    void setEmbeddingVectorString(final TextEntity embeddingVectorString) {
        this.embeddingVectorString = embeddingVectorString;
    }

    public double[] getEmbeddingVector() {
        if (embeddingVector == null && embeddingVectorString != null) {
            embeddingVector = VektorUtil.DoubleArrayConverter.stringToArray(embeddingVectorString.getText());
        }

        return embeddingVector;
    }

    public void setEmbeddingVector(final double[] embeddingVector) {
        this.embeddingVector = embeddingVector;
        final String embeddingVectorString1 = VektorUtil.DoubleArrayConverter.arrayToString(embeddingVector);
        if(embeddingVectorString1 != null){
            embeddingVectorString = new TextEntity(embeddingVectorString1);
        }

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

    public String getInternMastodonUrl() {
        return internMastodonUrl;
    }

    public void setInternMastodonUrl(final String internMastodonUrl) {
        this.internMastodonUrl = internMastodonUrl;
    }

    @Override
    public String toString() {
        return "PublicMastodonPostEntity{" +
                "id=" + id +
                ", mastodonId='" + mastodonId + '\'' +
                ", postText=" + postText +
                ", urlText=" + urlText +
                ", cosDistance=" + cosDistance +
                ", embeddingVectorString=" + embeddingVectorString +
                ", embeddingVector=" + Arrays.toString(embeddingVector) +
                ", statusOriginalUrl='" + statusOriginalUrl + '\'' +
                ", negativeWeight=" + negativeWeight +
                ", noURL=" + noURL +
                ", vikiCommented=" + vikiCommented +
                ", internMastodonUrl='" + internMastodonUrl + '\'' +
                ", localModel='" + localModel + '\'' +
                '}';
    }
}

