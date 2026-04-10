package de.hexix.homeassistant.dto;

import java.time.ZonedDateTime;

public record ElectricityPriceHistoryDto(
    ZonedDateTime timestamp,
    double value
) {}
