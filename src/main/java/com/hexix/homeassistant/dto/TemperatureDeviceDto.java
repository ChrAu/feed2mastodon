package com.hexix.homeassistant.dto;

import java.util.List;

public record TemperatureDeviceDto(String entityId, String friendlyName, List<TemperatureDto> temperatures) {
}
