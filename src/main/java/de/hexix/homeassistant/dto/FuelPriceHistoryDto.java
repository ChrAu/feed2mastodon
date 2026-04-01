package de.hexix.homeassistant.dto;

import java.time.ZonedDateTime;

public record FuelPriceHistoryDto(
    ZonedDateTime timestamp,
    double value
) {}
