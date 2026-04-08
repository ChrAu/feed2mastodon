package de.hexix.homeassistant.entity;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ha_fuel_forecast")
@NamedQueries({
        @NamedQuery(
                name = HaFuelForecast.DELETE_OLD_FORECASTS,
                query = "DELETE FROM HaFuelForecast f WHERE f.createdAt < :threshold"
        ),
        @NamedQuery(
                name = HaFuelForecast.FIND_BY_ENTITY_ID_ORDER_BY_CREATED_DESC,
                query = "SELECT DISTINCT f FROM HaFuelForecast f LEFT JOIN FETCH f.dataPoints WHERE f.entityId = :entityId ORDER BY f.createdAt DESC"
        )
})
public class HaFuelForecast {

    public static final String DELETE_OLD_FORECASTS = "HaFuelForecast.deleteOldForecasts";
    public static final String FIND_BY_ENTITY_ID_ORDER_BY_CREATED_DESC = "HaFuelForecast.findByEntityIdOrderByCreatedDesc";
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_id", nullable = false)
    private String entityId;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    @OneToMany(mappedBy = "forecast", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HaFuelForecastData> dataPoints = new ArrayList<>();

    @Column(name = "forecast_duration_minutes", nullable = false)
    private Integer forecastDurationMinutes;

    @Column(name = "raster_minutes", nullable = false)
    private Integer rasterMinutes;

    // --- Getter und Setter ---

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

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<HaFuelForecastData> getDataPoints() {
        return dataPoints;
    }

    public void setDataPoints(List<HaFuelForecastData> dataPoints) {
        this.dataPoints = dataPoints;
    }

    public void addDataPoint(HaFuelForecastData point) {
        dataPoints.add(point);
        point.setForecast(this);
    }

    public Integer getForecastDurationMinutes() {
        return forecastDurationMinutes;
    }

    public void setForecastDurationMinutes(Integer forecastDurationMinutes) {
        this.forecastDurationMinutes = forecastDurationMinutes;
    }

    public Integer getRasterMinutes() {
        return rasterMinutes;
    }

    public void setRasterMinutes(Integer rasterMinutes) {
        this.rasterMinutes = rasterMinutes;
    }
}
