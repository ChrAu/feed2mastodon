package com.hexix.homeassistant.dto;

import java.time.ZonedDateTime;

public record TemperatureDto(
        String entityId,
        String friendlyName,
        Number currentTemperature,
        Number shouldTemperature,
        ZonedDateTime lastChanged,
        String heatingState
) {
}
