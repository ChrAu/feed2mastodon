package com.hexix.mastodon;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;


@Entity
@Table(name = "mastodon_paging_configs")
public class PagingConfigEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_generator")
    @SequenceGenerator(
            name = "id_generator",
            sequenceName = "mastodon_paging_configs_id_seq", // WICHTIG: Passe dies an den Namen deiner DB-Sequenz an
            allocationSize = 1
    )
    @Column(name = "id")
    private Long id;

    @Column(name = "resource", columnDefinition = "TEXT", nullable = false)
    private String resource;

    @Column(name = "max_id", columnDefinition = "TEXT")
    private  String maxId;

    @Column(name = "since_id", columnDefinition = "TEXT")
    private  String sinceId;

    @Column(name = "min_id", columnDefinition = "TEXT")
    private String minId;


    protected PagingConfigEntity() {

    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public PagingConfigEntity(final String resource) {
        this.resource = resource;
    }


    public String getResource() {
        return resource;
    }

    public void setResource(final String resource) {
        this.resource = resource;
    }

    public String getMaxId() {
        return maxId;
    }

    public void setMaxId(final String maxId) {
        this.maxId = maxId;
    }

    public String getSinceId() {
        return sinceId;
    }

    public void setSinceId(final String sinceId) {
        this.sinceId = sinceId;
    }

    public String getMinId() {
        return minId;
    }

    public void setMinId(final String minId) {
        this.minId = minId;
    }

    public static PagingConfigEntity find(String resource){
        return find("resource = ?1", resource).firstResult();
    }


    @Override
    public String toString() {
        return "PagingConfigEntity{" +
                "id=" + id +
                ", resource='" + resource + '\'' +
                ", maxId='" + maxId + '\'' +
                ", sinceId='" + sinceId + '\'' +
                ", minId='" + minId + '\'' +
                '}';
    }
}
