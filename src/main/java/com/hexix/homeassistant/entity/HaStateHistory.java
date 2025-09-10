package com.hexix.homeassistant.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.time.ZonedDateTime;

/**
 * Speichert einen einzelnen historischen Zustand einer Entität unter Verwendung
 * von klassischem JPA mit NamedQueries. Verwendet eine eigene, automatisch
 * generierte ID als Primärschlüssel.
 */
@Entity
@Table(name = "ha_state_history")
@NamedQueries({
        @NamedQuery(
                name = "HaStateHistory.findByEntityId",
                query = "SELECT h FROM HaStateHistory h WHERE h.entityId = :entityId ORDER BY h.lastChanged DESC"
        ),
        @NamedQuery(
                name = "HaStateHistory.findAfterTimestamp",
                query = "SELECT h FROM HaStateHistory h WHERE h.entityId = :entityId AND h.lastChanged > :timestamp ORDER BY h.lastChanged ASC"
        )
})
public class HaStateHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_generator")
    @SequenceGenerator(name = "id_generator", sequenceName = "ha_state_history_id_seq", // WICHTIG: Passe dies an den Namen deiner DB-Sequenz an
            allocationSize = 1)
    private Long id;

    @Column(name = "entity_id", nullable = false)
    private String entityId;

    @Column(name = "state")
    private String state;

    @Column(name = "last_changed", nullable = false)
    private ZonedDateTime lastChanged;

    /**
     * Die Attribute werden als serialisierter JSON-String gespeichert.
     * Bei PostgreSQL kann hierfür auch der Datentyp JSONB verwendet werden,
     * indem man eine entsprechende Hibernate-Type-Definition hinzufügt.
     */
    @Column(name = "attributes", columnDefinition = "TEXT")
    private String attributes;

    // Standard-Konstruktor (von JPA benötigt)
    public HaStateHistory() {
    }

    // Getter und Setter

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public ZonedDateTime getLastChanged() {
        return lastChanged;
    }

    public void setLastChanged(ZonedDateTime lastChanged) {
        this.lastChanged = lastChanged;
    }

    public String getAttributes() {
        return attributes;
    }

    public void setAttributes(final String attributes) {
        this.attributes = attributes;
    }
}
