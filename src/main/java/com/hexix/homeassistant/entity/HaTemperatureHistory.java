package com.hexix.homeassistant.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "ha_temperature_history")
public class HaTemperatureHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_generator")
    @SequenceGenerator(name = "id_generator", sequenceName = "ha_temperature_history_id_seq", // WICHTIG: Passe dies an den Namen deiner DB-Sequenz an
            allocationSize = 1)
    private Long id;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "ha_state_history_id", referencedColumnName = "id")
    private HaStateHistory haStateHistory;

    @Column(name = "current_temperature")
    private Double currentTemperature;

    @Column(name = "should_temperature")
    private Double shouldTemperature;


    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public HaStateHistory getHaStateHistory() {
        return haStateHistory;
    }

    public void setHaStateHistory(final HaStateHistory haStateHistory) {
        this.haStateHistory = haStateHistory;
    }

    public Double getShouldTemperature() {
        return shouldTemperature;
    }

    public void setShouldTemperature(final Double shouldTemperature) {
        this.shouldTemperature = shouldTemperature;
    }

    public Double getCurrentTemperature() {
        return currentTemperature;
    }

    public void setCurrentTemperature(final Double currentTemperature) {
        this.currentTemperature = currentTemperature;
    }
}
