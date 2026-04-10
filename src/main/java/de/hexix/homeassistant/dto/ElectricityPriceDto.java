package de.hexix.homeassistant.dto;

import java.time.LocalDateTime;

public record ElectricityPriceDto(
        String entityId,
        String friendlyName,
        Double value,
        String unit,
        LocalDateTime lastChanged,
        Double previousValue,
        String currency,
        String provider,
        String region
) {}
