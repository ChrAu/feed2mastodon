package com.hexix.homeassistant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import java.time.ZonedDateTime;

/**
 * Repräsentiert den aktuellen Zustand einer Home Assistant Entität in der Datenbank
 * unter Verwendung von klassischem JPA mit NamedQueries.
 * Die entityId von Home Assistant wird als Primärschlüssel verwendet.
 */
@Entity
@Table(name = "ha_entities")
@NamedQueries({
        @NamedQuery(name = "HaEntity.findAll", query = "SELECT e FROM HaEntity e ORDER BY e.entityId"),
        @NamedQuery(name = "HaEntity.findByEntityId", query = "SELECT e FROM HaEntity e WHERE e.entityId = :entityId"),
        @NamedQuery(name = "HaEntity.findByState", query = "SELECT e FROM HaEntity e WHERE e.state = :state ORDER BY e.entityId")
})
public class HaEntity {

    public static String FIND_ALL = "HaEntity.findAll";
    public static String FIND_BY_ENTITY_ID = "HaEntity.findByEntityId";
    public static String FIND_BY_STATE = "HaEntity.findByState";

    @Id
    @Column(name = "entity_id")
    private String entityId;

    @Column(name = "state")
    private String state;

    /**
     * Die Attribute werden als serialisierter JSON-String gespeichert.
     * Bei PostgreSQL kann hierfür auch der Datentyp JSONB verwendet werden,
     * indem man eine entsprechende Hibernate-Type-Definition hinzufügt.
     */
    @Column(name = "attributes", columnDefinition = "TEXT")
    private String attributes;

    @Column(name = "last_updated")
    private ZonedDateTime lastUpdated;

    @Column(name = "last_changed")
    private ZonedDateTime lastChanged;

    // Standard-Konstruktor (von JPA benötigt)
    public HaEntity() {
    }

    // Getter und Setter

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getAttributes() {
        return attributes;
    }

    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }

    public ZonedDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(ZonedDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public ZonedDateTime getLastChanged() {
        return lastChanged;
    }

    public void setLastChanged(ZonedDateTime lastChanged) {
        this.lastChanged = lastChanged;
    }
}
