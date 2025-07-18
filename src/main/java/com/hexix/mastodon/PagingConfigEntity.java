package com.hexix.mastodon;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;


@Entity
@Table(name = "mastodon_paging_config", indexes = {
        @Index(name = "idx_PagingConfigEntity_resource", columnList = "resource", unique = true)
})
public class PagingConfigEntity extends PanacheEntity {

    @Column(name = "resource", columnDefinition = "TEXT", nullable = false)
    String resource;

    @Column(name = "max_id", columnDefinition = "TEXT")
    String maxId;

    @Column(name = "sinceId", columnDefinition = "TEXT")
    String sinceId;

    @Column(name = "min_id", columnDefinition = "TEXT")
    String minId;

    PagingConfigEntity(){

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
}
