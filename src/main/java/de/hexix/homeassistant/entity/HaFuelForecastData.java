package de.hexix.homeassistant.entity;

import jakarta.persistence.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "ha_fuel_forecast_data")
public class HaFuelForecastData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forecast_id", nullable = false)
    private HaFuelForecast forecast;

    @Column(name = "target_timestamp", nullable = false)
    private ZonedDateTime targetTimestamp;

    @Column(name = "predicted_price", nullable = false)
    private Double predictedPrice;

    // --- Getter und Setter ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public HaFuelForecast getForecast() {
        return forecast;
    }

    public void setForecast(HaFuelForecast forecast) {
        this.forecast = forecast;
    }

    public ZonedDateTime getTargetTimestamp() {
        return targetTimestamp;
    }

    public void setTargetTimestamp(ZonedDateTime targetTimestamp) {
        this.targetTimestamp = targetTimestamp;
    }

    public Double getPredictedPrice() {
        return predictedPrice;
    }

    public void setPredictedPrice(Double predictedPrice) {
        this.predictedPrice = predictedPrice;
    }
}
