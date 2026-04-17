package de.hexix.homeassistant.dto;

import java.time.LocalDateTime;

public record CarDataDto(
        Double odometer,
        Double electricRange,
        Double batteryLevel,
        Double externalTemperature,
        LocalDateTime lastUpdate
) {}
