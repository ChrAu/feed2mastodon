package de.hexix.homeassistant.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ElectricityPriceOverviewDto(
        String entityId, // entityId for total_price, as it's the main one
        String friendlyName, // friendly name for total_price
        String unit,
        LocalDateTime lastChanged,
        String currency,
        String provider,
        String region,
        Map<String, ElectricityPriceDto> prices // Map of price type to its details
) {}
