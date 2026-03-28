package de.hexix.homeassistant.dto;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

public record CpuDto(String entityId, String friendlyName, String state, ZonedDateTime lastChanged) {
}
