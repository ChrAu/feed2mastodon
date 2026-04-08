package de.hexix.homeassistant.dto;

import java.time.ZonedDateTime;
import java.util.List;

public record SavedForecastDto(
        Long id,
        ZonedDateTime createdAt,
        Integer forecastDurationMinutes,
        Integer rasterMinutes,
        List<FuelPriceForecastDto> dataPoints
) {}
