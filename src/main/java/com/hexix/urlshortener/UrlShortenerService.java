package com.hexix.urlshortener;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.Random;

@ApplicationScoped
public class UrlShortenerService {

    private static final String ALPHANUMERIC_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final Random RANDOM = new Random();

    private String generateShortKey(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(ALPHANUMERIC_CHARS.length());
            sb.append(ALPHANUMERIC_CHARS.charAt(index));
        }
        return sb.toString();
    }

    @Transactional
    public String shortenUrl(String originalUrl) {
        // Implementierung der Logik zum Kürzen der URL
        // Beispiel: Generiere einen zufälligen kurzen Schlüssel
        String shortKey = generateShortKey(6);

        UrlMapping urlMapping = new UrlMapping();
        urlMapping.originalUrl = originalUrl;
        urlMapping.shortKey = shortKey;
        urlMapping.shortUrl = "https://s.hexix.de/s/" + shortKey;
        urlMapping.persist(); // Speichern in der Datenbank

        return urlMapping.shortUrl;
    }



}
