package com.hexix.homeassistant;

import jakarta.inject.Singleton;

import java.util.HashMap;
import java.util.Map;

@Singleton
public class WeatherMapper {

    private final Map<String, String> weatherMap = new HashMap<>();

    public WeatherMapper() {
        // Clear sky
        weatherMap.put("clear-night", "Klare Nacht");
        weatherMap.put("sunny", "Sonnig");

        // Clouds
        weatherMap.put("partlycloudy", "Teilweise bewölkt");
        weatherMap.put("cloudy", "Bewölkt");

        // Precipitation
        weatherMap.put("rainy", "Regnerisch");
        weatherMap.put("pouring", "Starkregen");
        weatherMap.put("snowy", "Schneefall");
        weatherMap.put("snowy-rainy", "Schneeregen");
        weatherMap.put("hail", "Hagel");

        // Wind
        weatherMap.put("windy", "Windig");
        weatherMap.put("windy-variant", "Windig mit Wolken");

        // Other conditions
        weatherMap.put("lightning", "Gewitter");
        weatherMap.put("lightning-rainy", "Gewitter mit Regen");
        weatherMap.put("fog", "Nebel");
        weatherMap.put("exceptional", "Außergewöhnlich");
    }

    /**
     * Translates an English weather condition key to its German equivalent.
     *
     * @param key The English weather condition key (e.g., "sunny").
     * @return The German translation (e.g., "Sonnig"), or null if the key is not found.
     */
    public String getGermanTranslation(String key) {
        return weatherMap.get(key);
    }

}
