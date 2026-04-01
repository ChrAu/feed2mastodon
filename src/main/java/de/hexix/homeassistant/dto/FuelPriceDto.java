package de.hexix.homeassistant.dto;

import java.time.ZonedDateTime;

public record FuelPriceDto(
    double value,
    String unit,
    ZonedDateTime lastChanged
) {}
