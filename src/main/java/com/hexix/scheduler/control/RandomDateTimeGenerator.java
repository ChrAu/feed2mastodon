package com.hexix.scheduler.control;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;

import java.time.LocalDateTime;
import java.time.LocalTime;

@RequestScoped
public class RandomDateTimeGenerator {

    private LocalDateTime lastGenerationDay;
    private int dailyCallCounter;
    private final java.util.Random random = new java.util.Random();


    /**
     * Generiert ein zufälliges LocalDateTime innerhalb der nächsten 24 Stunden,
     * unter Berücksichtigung der folgenden Bedingungen:
     * - Maximal drei Zeitpunkte pro Tag.
     * - Die meisten Zeitpunkte sollen zwischen 06:30 Uhr und 17:00 Uhr liegen (gleichverteilt).
     * - Außerhalb dieses Bereichs sollen Zeitpunkte näher an 06:30 Uhr bzw. 17:00 Uhr wahrscheinlicher sein.
     *
     * @param startDateTime Das Start-LocalDateTime für die Berechnung der nächsten 24 Stunden.
     * @return Ein zufällig generiertes LocalDateTime.
     * @throws IllegalStateException Wenn mehr als drei Zeitpunkte für den aktuellen Tag generiert werden sollen.
     */
    public LocalDateTime getRandomDateTimeInNext24Hours(LocalDateTime startDateTime) {
        // Setze den Tageszähler zurück, wenn ein neuer Tag beginnt
        if (lastGenerationDay == null || startDateTime.toLocalDate().isAfter(lastGenerationDay.toLocalDate())) {
            dailyCallCounter = 0;
            lastGenerationDay = startDateTime;
        }

        if (dailyCallCounter >= 5) {
            throw new IllegalStateException("Es können maximal fünf Zeitpunkte pro Tag generiert werden.");
        }

        dailyCallCounter++;

        // Wahrscheinlichkeitsgewichtung für die Uhrzeit
        // 90% der Zeitpunkte sollen zwischen 06:30 und 17:00 liegen
        boolean preferWorkingHours = random.nextDouble() < 0.9;

        long randomMinutesOffset;
        int minMinutesPreferred = 6 * 60 + 30; // 06:30 Uhr in Minuten
        int maxMinutesPreferred = 17 * 60;     // 17:00 Uhr in Minuten

        if (preferWorkingHours) {
            // Generiere eine Zeit zwischen 06:30 und 17:00 (gleichverteilt)
            randomMinutesOffset = minMinutesPreferred + random.nextInt(maxMinutesPreferred - minMinutesPreferred + 1);
        } else {
            // Generiere eine Zeit außerhalb der bevorzugten Stunden
            // Hier nutzen wir eine gewichtete Zufallszahl, um die Ränder zu bevorzugen.
            // Die Verteilung ist ähnlich einer umgekehrten Parabel oder einer Dreiecksverteilung,
            // deren Spitzen an 06:30 und 17:00 liegen.

            // Gesamtminuten des Tages
            int totalMinutesDay = 24 * 60;

            // Die "unbevorzugten" Bereiche in Minuten
            int minutesBeforePreferred = minMinutesPreferred; // 00:00 bis 06:30
            int minutesAfterPreferred = totalMinutesDay - maxMinutesPreferred; // 17:00 bis 24:00

            // Wir kombinieren beide Bereiche und wählen einen Punkt in einem erweiterten, gewichteten Bereich.
            // Dies ist ein vereinfachtes Modell, um die Gewichtung zu erreichen.
            // Wir verwenden eine Transformation der Zufallszahl, um die Dichte an den Rändern zu erhöhen.

            // Wähle zufällig, ob es ein Zeitpunkt vor 6:30 oder nach 17:00 sein soll
            if (random.nextBoolean()) { // Zeitpunkt vor 06:30
                // Generiere eine Zufallszahl im Bereich [0, minutesBeforePreferred]
                // Wende eine Transformation an, die niedrigere Werte (näher an 06:30) bevorzugt
//                double r = random.nextDouble(); // r ist zwischen 0.0 und 1.0
                // Umkehrung einer quadratischen Funktion, um die Dichte am Ende zu erhöhen
//                randomMinutesOffset = (long) (minutesBeforePreferred * (1 - Math.sqrt(1 - r)));
                // randomMinutesOffset = (long) (minutesBeforePreferred * r * r); // Alternative für andere Verteilung

                randomMinutesOffset = (long) getSkewedRandom(minutesBeforePreferred, 3.0);
            } else { // Zeitpunkt nach 17:30
                // Generiere eine Zufallszahl im Bereich [0, minutesAfterPreferred]
                // Wende eine Transformation an, die höhere Werte (näher an 17:00) bevorzugt
//                double r = random.nextDouble(); // r ist zwischen 0.0 und 1.0
                // Hier wollen wir Werte näher an 17:00 (also am Anfang des "nach 17:00"-Bereichs)
//                randomMinutesOffset = maxMinutesPreferred + (long) (minutesAfterPreferred * Math.sqrt(r));
                // randomMinutesOffset = maxMinutesPreferred + (long) (minutesAfterPreferred * (1 - (1 - r) * (1 - r))); // Alternative für andere Verteilung

                randomMinutesOffset = maxMinutesPreferred + (minutesAfterPreferred - getSkewedRandom(minutesAfterPreferred, 3.0));
            }
        }

        // Umrechnung der Minuten in Stunden und Minuten für LocalTime
        int hour = (int) (randomMinutesOffset / 60);
        int minute = (int) (randomMinutesOffset % 60);

        LocalTime randomTime = LocalTime.of(hour, minute);

        // Füge die zufällige Uhrzeit zum Startdatum hinzu
        return startDateTime.toLocalDate().atTime(randomTime);
    }

    public int getSkewedRandom(int maxValue, double skewFactor) {
        if (maxValue <= 0) {
            throw new IllegalArgumentException("maxValue muss positiv sein.");
        }
        if (skewFactor <= 0) {
            throw new IllegalArgumentException("skewFactor muss positiv sein.");
        }

        // Math.random() gibt eine Zahl zwischen 0.0 (inklusiv) und 1.0 (exklusiv) zurück.
        // Math.pow(randomValue, exponent) verschiebt die Verteilung.
        // Ein Exponent zwischen 0.0 und 1.0 verschiebt die Werte in Richtung 1.0.
        // Ein Exponent > 1.0 verschiebt die Werte in Richtung 0.0.
        // Wir verwenden 1.0 / skewFactor, damit ein höherer skewFactor zu einer stärkeren Verschiebung in Richtung des Maximalwerts führt.
        double exponent = 1.0 / skewFactor;
        double randomValue = random.nextDouble();

        double skewedRandom = Math.pow(randomValue, exponent);

        return (int) (skewedRandom * maxValue);
    }
}
