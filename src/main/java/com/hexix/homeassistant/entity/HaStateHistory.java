package com.hexix.homeassistant.entity;


import com.hexix.homeassistant.dto.TemperatureBucketDTO;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
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
                name = HaStateHistory.FIND_BY_ENTITY_ID,
                query = "SELECT h FROM HaStateHistory h WHERE h.entityId = :entityId ORDER BY h.lastChanged DESC"
        ),
        @NamedQuery(
                name = HaStateHistory.FIND_AFTER_TIMESTAMP,
                query = "SELECT h FROM HaStateHistory h WHERE h.entityId = :entityId AND h.lastChanged > :timestamp ORDER BY h.lastChanged ASC"
        ),
        @NamedQuery(
                name = HaStateHistory.FIND_All_TEMPERATUR,
                query = "SELECT h FROM HaStateHistory h where h.entityId ilike :entityId and h.lastChanged > :startDate ORDER BY h.lastChanged ASC"
        ),
        @NamedQuery(
                name = HaStateHistory.FIND_All_TEMPERATUR_NO_DATA_TABLE,
                query = "SELECT h FROM HaStateHistory h where entityId ilike 'climate.%' and h.haTemperatureHistory is null ORDER BY h.lastChanged ASC limit 1000"
        ),
        @NamedQuery(
                name = HaStateHistory.FIND_ALL_WEATHER_DATA,
                query = "SELECT h FROM HaStateHistory h where entityId ilike 'weather.gosbach' and h.lastChanged > :startDate ORDER BY h.lastChanged ASC"
        )
})

@SqlResultSetMapping(
        name = HaStateHistory.SQL_RESULT_SET_MAPPING__TEMPERATURE_BUCKET_MAPPING,
        classes = @ConstructorResult(
                targetClass = TemperatureBucketDTO.class,
                columns = {
                        @ColumnResult(name = "time_bucket", type = OffsetDateTime.class),
                        @ColumnResult(name = "avg_temperature", type = Double.class)
                }
        )
)
// 2. Definiere die Named Query selbst. Sie verwendet das Mapping von oben.
@NamedNativeQueries({
        @NamedNativeQuery(
                name = HaStateHistory.NATIVE_FIND_AVG_TEMPERATUR_IN_BUCKETS,
                query = """
    WITH parameters AS (
        SELECT
            -- Der Wert für die Buckets wird durch einen Parameter :bucketsPerDay ersetzt
            CAST(:bucketsPerDay AS integer) AS buckets_per_day
    ),
    extracted_data AS (
        SELECT
            sh.last_changed,
            ((sh.attributes::jsonb -> 'additionalAttributes') ->> 'current_temperature')::float AS current_temperature
        FROM ha_state_history sh
        WHERE sh.entity_id = :entityId
          AND jsonb_typeof(sh.attributes::jsonb -> 'additionalAttributes' -> 'current_temperature') = 'number'
          AND sh.last_changed BETWEEN NOW() - INTERVAL '90 day' AND NOW()
    )
    SELECT
        to_timestamp(
            floor(
                EXTRACT(EPOCH FROM ed.last_changed) / (86400 / p.buckets_per_day)
            ) * (86400 / p.buckets_per_day)
        ) AS time_bucket,
        ROUND(AVG(ed.current_temperature)::numeric, 2) AS avg_temperature
    FROM extracted_data ed
    CROSS JOIN parameters p
    GROUP BY time_bucket
    ORDER BY time_bucket DESC
    """,
                resultSetMapping = HaStateHistory.SQL_RESULT_SET_MAPPING__TEMPERATURE_BUCKET_MAPPING
        )
})


public class HaStateHistory {

    public static final String FIND_BY_ENTITY_ID = "HaStateHistory.findByEntityId";

    public static final String FIND_AFTER_TIMESTAMP = "HaStateHistory.findAfterTimestamp";

    public static final String FIND_All_TEMPERATUR = "HaStateHistory.findAllTemperatur";

    public static final String FIND_All_TEMPERATUR_NO_DATA_TABLE = "HaStateHistory.findAllTemperaturNoDataTable";
    public static final String FIND_ALL_WEATHER_DATA = "HaStateHistory.findAllWeatherData";

    public static final String NATIVE_FIND_AVG_TEMPERATUR_IN_BUCKETS = "HaStateHistory.findAvgTemperatureInBuckets";

    public static final String SQL_RESULT_SET_MAPPING__TEMPERATURE_BUCKET_MAPPING = "HaStateHistory.TemperatureBucketMapping";

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

    @OneToOne(mappedBy = "haStateHistory", cascade = CascadeType.ALL, orphanRemoval = true)
    private HaTemperatureHistory haTemperatureHistory;

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

    public HaTemperatureHistory getHaTemperatureHistory() {
        return haTemperatureHistory;
    }

    public void setHaTemperatureHistory(final HaTemperatureHistory haTemperatureHistory) {
        this.haTemperatureHistory = haTemperatureHistory;
    }
}
