package de.hexix.homeassistant.dto;

import java.util.Map;

public record FuelStationDto(
    String name,
    Map<String, FuelPriceDto> fuelPrices, // Map of fuel type (e.g., "diesel", "super") to FuelPriceDto
    boolean status
) {}
