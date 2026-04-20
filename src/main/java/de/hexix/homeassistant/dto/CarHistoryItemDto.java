package de.hexix.homeassistant.dto;

import java.time.ZonedDateTime;

public record CarHistoryItemDto(
        ZonedDateTime timestamp,
        Double value
) {}
