package com.hexix.homeassistant.dto;

import java.time.ZonedDateTime;

public record WeatherDto (
        String state,
        String wetterZustand,
        String friendlyName,
        Double pressure,
        String pressureUnit,
        Double temperature,
        Double apparentTemperature,
        String temperatureUnit,
        Integer humidity,
        Double windSpeed,
        Integer windBearing,
        String windSpeedUnit,
        String precipitationUnit,
        ZonedDateTime lastUpdate) {
}
