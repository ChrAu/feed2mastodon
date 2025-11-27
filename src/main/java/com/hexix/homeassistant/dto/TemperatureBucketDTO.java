package com.hexix.homeassistant.dto;

import java.time.OffsetDateTime;

public record TemperatureBucketDTO(OffsetDateTime timeBucket, Double avgTemperature) {

    @Override
    public String toString() {
        return "TemperatureBucketDTO{" +
                "timeBucket=" + timeBucket +
                ", avgTemperature=" + avgTemperature +
                '}';
    }
}
