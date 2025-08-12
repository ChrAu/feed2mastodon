package com.hexix.urlshortener;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.RandomStringUtils;

@ApplicationScoped
public class UrlShortenerService {

    @Transactional
    public String shortenUrl(String originalUrl) {
        // Implementierung der Logik zum Kürzen der URL
        // Beispiel: Generiere einen zufälligen kurzen Schlüssel
        String shortKey = RandomStringUtils.randomAlphanumeric(6);

        UrlMapping urlMapping = new UrlMapping();
        urlMapping.originalUrl = originalUrl;
        urlMapping.shortKey = shortKey;
        urlMapping.shortUrl = "https://s.hexix.de/s/" + shortKey;
        urlMapping.persist(); // Speichern in der Datenbank

        return urlMapping.shortUrl;
    }



}
