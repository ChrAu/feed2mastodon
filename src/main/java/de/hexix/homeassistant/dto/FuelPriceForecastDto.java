package de.hexix.homeassistant.dto;

import java.time.ZonedDateTime;

public record FuelPriceForecastDto(
    ZonedDateTime timestamp,
    double predictedPrice
) {}
