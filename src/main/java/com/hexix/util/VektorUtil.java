package com.hexix.util;

import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class VektorUtil {

    static final Logger LOG = Logger.getLogger(VektorUtil.class);


    public static final class DoubleArrayConverter {

        private static final String TRENNZEICHEN = ",";

        // Privater Konstruktor, da dies eine Utility-Klasse ist und nicht instanziiert werden soll.
        private DoubleArrayConverter() {
        }

        /**
         * Wandelt ein double-Array in einen einzelnen, durch Kommas getrennten String um.
         *
         * @param daten Das Array, das konvertiert werden soll.
         * @return Ein String, der die Array-Elemente darstellt (z.B. "1.5,-3.14,10.0").
         * Gibt null zurück, wenn das Eingabe-Array null ist.
         */
        public static String arrayToString(double[] daten) {
            if (daten == null) {
                return null;
            }
            // Nutzt Java Streams, um jeden double in einen String zu wandeln und mit dem Trennzeichen zu verbinden.
            return Arrays.stream(daten)
                    .mapToObj(String::valueOf)
                    .collect(Collectors.joining(TRENNZEICHEN));
        }

        /**
         * Wandelt einen durch Kommas getrennten String zurück in ein double-Array.
         *
         * @param datenString Der String, der konvertiert werden soll (z.B. "1.5,-3.14,10.0").
         * @return Ein double-Array mit den Werten aus dem String.
         * Gibt null zurück, wenn der Eingabe-String null ist.
         * Gibt ein leeres Array zurück, wenn der Eingabe-String leer ist.
         * @throws NumberFormatException Wenn ein Teil des Strings keine gültige Zahl ist.
         */
        public static double[] stringToArray(String datenString) {
            if (datenString == null) {
                return null;
            }
            if (datenString.isEmpty()) {
                return new double[0];
            }
            // Teilt den String am Trennzeichen und wandelt jeden Teil zurück in einen double.
            return Arrays.stream(datenString.split(TRENNZEICHEN))
                    .mapToDouble(Double::parseDouble)
                    .toArray();
        }
    }


    /**
     * Berechnet die korrekte Kosinus-Ähnlichkeit zwischen zwei Vektoren.
     *
     * @param vectorA Der erste Vektor.
     * @param vectorB Der zweite Vektor.
     * @return Der Ähnlichkeits-Score zwischen -1.0 und 1.0.
     */
    public static double getCosineSimilarity(double[] vectorA, double[] vectorB) {
        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException("Vektoren müssen die gleiche Dimension haben.");
        }


        double dotProduct = 0.0;
        double magnitude1 = 0.0;
        double magnitude2 = 0.0;

        // Berechne Skalarprodukt und die Summe der Quadrate für die Normen
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            magnitude1 += Math.pow(vectorA[i], 2);
            magnitude2 += Math.pow(vectorB[i], 2);
        }

        magnitude1 = Math.sqrt(magnitude1);
        magnitude2 = Math.sqrt(magnitude2);


        if (magnitude1 == 0 || magnitude2 == 0) {
            return 0.0; // Wenn einer der Vektoren ein Nullvektor ist, ist die Ähnlichkeit 0.
        }

        return dotProduct / (magnitude1 * magnitude2);
    }

    /**
     * Erstellt einen Profil-Vektor, indem positive Vektoren gewichtet addiert und negative Vektoren gewichtet subtrahiert werden.
     * Der resultierende Vektor wird normalisiert.
     *
     * @param positiveVectors Eine Liste von Vektoren, die positive Interessen repräsentieren.
     * @param negativeVectors Eine Liste von Vektoren, die negative Interessen repräsentieren.
     * @return Der resultierende, normalisierte Profil-Vektor. Gibt einen Nullvektor zurück, wenn keine gültigen Vektoren vorhanden sind.
     */
    public static double[] createProfileVector(List<VektorWeight> positiveVectors,
                                               List<VektorWeight> negativeVectors) {

        // Bestimme die Dimension der Vektoren. Annahme: Alle Vektoren haben die gleiche Dimension.
        // Wenn keine Vektoren vorhanden sind, gib einen Nullvektor zurück.
        int dimension;
        if (positiveVectors != null && !positiveVectors.isEmpty()) {
            dimension = positiveVectors.getFirst().vektor().length;
        } else if (negativeVectors != null && !negativeVectors.isEmpty()) {
            dimension = negativeVectors.getFirst().vektor().length;
        } else {
            dimension = 0;
            // Keine Vektoren zum Erstellen eines Profils vorhanden.
            throw new IllegalArgumentException("Kann keinen Vektor erstellen, keine Vektoren vorhanden.");
        }

        // Überprüfe, ob alle Vektoren die gleiche Dimension haben (optional, aber gute Praxis)
        if ((positiveVectors != null && positiveVectors.stream().anyMatch(v -> v.vektor().length != dimension)) ||
                (negativeVectors != null && negativeVectors.stream().anyMatch(v -> v.vektor().length != dimension))) {
            LOG.warn("Warnung: Vektoren haben unterschiedliche Dimensionen. Dies könnte zu Fehlern führen.");
            // Eine robustere Implementierung könnte hier eine Exception werfen oder die fehlerhaften Vektoren ignorieren.
        }


        double[] profileVector = new double[dimension];

        // Positive Vektoren mit Gewichtung addieren
        if (positiveVectors != null) {
            for (VektorWeight vector : positiveVectors) {
                for (int i = 0; i < dimension; i++) {
                    profileVector[i] += vector.vektor()[i] * vector.weight;
                }
            }
        }

        // Negative Vektoren mit Gewichtung subtrahieren
        if (negativeVectors != null) {
            for (VektorWeight vector : negativeVectors) {
                for (int i = 0; i < dimension; i++) {
                    profileVector[i] -= vector.vektor()[i] * vector.weight();
                }
            }
        }

        // Den resultierenden Vektor normalisieren
        return normalize(profileVector);
    }

    /**
     * Normalisiert einen Vektor, sodass seine Länge 1 beträgt.
     * @param vector Der zu normalisierende Vektor.
     * @return Der normalisierte Vektor. Gibt den ursprünglichen Vektor zurück, wenn die Länge 0 ist, um Division durch Null zu vermeiden.
     */
    private static double[] normalize(double[] vector) {
        double magnitude = 0.0;
        for (double v : vector) {
            magnitude += v * v;
        }
        magnitude = Math.sqrt(magnitude);

        // Wenn die Magnitude 0 ist (Nullvektor), kann er nicht normalisiert werden.
        // In diesem Fall geben wir den ursprünglichen Vektor zurück.
        if (magnitude == 0) {
            return vector;
        }

        double[] normalizedVector = new double[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalizedVector[i] = vector[i] / magnitude;
        }
        return normalizedVector;
    }


    /**
     * Erstellt einen Profil-Vektor, indem der Durchschnitt mehrerer Vektoren gebildet wird.
     * @param vectors Eine Liste von Vektoren.
     * @return Der durchschnittliche, normalisierte Vektor.
     */
    public static double[] createProfileVector(List<double[]> vectors) {
        return createProfileVector(vectors.stream().map(doubles -> new VektorWeight(doubles, 1.0)).toList(), Collections.emptyList());
    }



        public record VektorWeight(double[] vektor, double weight){}



}
