package de.hexix.homeassistant.service;

import de.hexix.homeassistant.dto.FuelPriceForecastDto;
import de.hexix.homeassistant.entity.HaFuelForecast;
import de.hexix.homeassistant.entity.HaFuelForecastData;
import de.hexix.homeassistant.entity.HaStateHistory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class HoltWinterForecastService {

    @Inject
    EntityManager em;

    private static final Logger LOG = LoggerFactory.getLogger(HoltWinterForecastService.class);

    private static class CachedParams {
        final double alpha;
        final double beta;
        final double gamma;
        final ZonedDateTime timestamp;

        CachedParams(double alpha, double beta, double gamma, ZonedDateTime timestamp) {
            this.alpha = alpha;
            this.beta = beta;
            this.gamma = gamma;
            this.timestamp = timestamp;
        }
    }

    private record ForecastCacheKey(String entityId, ZonedDateTime lastHistoryTimestamp, Duration forecastDuration, int rasterMinutes) {}

    private final Map<String, CachedParams> paramCache = new ConcurrentHashMap<>();
    private final Map<ForecastCacheKey, List<FuelPriceForecastDto>> fuelForecastResultCache = new ConcurrentHashMap<>();


    public static class PricePoint {
        public ZonedDateTime timestamp;
        public double price;

        public PricePoint(ZonedDateTime timestamp, double price) {
            this.timestamp = timestamp;
            this.price = price;
        }
    }

    public List<FuelPriceForecastDto> calculateForecast(List<HaStateHistory> historyData, Duration forecastDuration, int rasterMinutes, boolean persist) {
        if (historyData == null || historyData.isEmpty()) {
            return List.of();
        }

        String entityId = historyData.getFirst().getEntityId();
        ZonedDateTime lastHistoryTimestamp = historyData.getLast().getLastChanged();
        ForecastCacheKey cacheKey = new ForecastCacheKey(entityId, lastHistoryTimestamp, forecastDuration, rasterMinutes);

        // Check if the forecast result is already in the cache
        if (fuelForecastResultCache.containsKey(cacheKey)) {
            LOG.info("Returning cached fuel forecast for entityId: {} with lastHistoryTimestamp: {}, forecastDuration: {}, rasterMinutes: {}", entityId, lastHistoryTimestamp, forecastDuration, rasterMinutes);
            return fuelForecastResultCache.get(cacheKey);
        }

        List<PricePoint> rawData = new ArrayList<>();
        for (HaStateHistory h : historyData) {
            if ("unavailable".equalsIgnoreCase(h.getState()) || "unknown".equalsIgnoreCase(h.getState())) {
                continue;
            }
            try {
                double price = Double.parseDouble(h.getState());
                rawData.add(new PricePoint(h.getLastChanged(), price));
            } catch (NumberFormatException e) {
                // Ignore invalid numbers
            }
        }

        if (rawData.isEmpty()) {
            return List.of();
        }

        ZonedDateTime now = ZonedDateTime.now();
        double[] normalizedGrid = normalizeToGrid(rawData, rasterMinutes, now);
        if (normalizedGrid.length == 0) {
            return List.of();
        }

        // 144 Intervalle à 10 Minuten = 24 Stunden (Tagesmuster)
        int zyklusLaenge = (24 * 60) / rasterMinutes;

        // Vorhersage Schritte berechnen
        int vorhersageSchritte = (int) (forecastDuration.toMinutes() / rasterMinutes);

        CachedParams cache = paramCache.get(entityId);

        double alpha, beta, gamma;


        if (cache != null && lastHistoryTimestamp.isEqual(cache.timestamp)) {
            alpha = cache.alpha;
            beta = cache.beta;
            gamma = cache.gamma;
            LOG.info("Nutze gecachte Parameter für {}: alpha={:.2f}, beta={:.2f}, gamma={:.2f}", entityId, alpha, beta, gamma);
        } else {
            // Optimiere die Holt-Winters Parameter durch Grid Search
            double[] bestParams = optimizeHoltWintersParameters(normalizedGrid, zyklusLaenge, rasterMinutes);
            alpha = bestParams[0];
            beta = bestParams[1];
            gamma = bestParams[2];
            paramCache.put(entityId, new CachedParams(alpha, beta, gamma, lastHistoryTimestamp));
            LOG.info("Optimale Parameter gefunden für {}: alpha={:.2f}, beta={:.2f}, gamma={:.2f}", entityId, alpha, beta, gamma);
        }

        double[] prognose;
        try {
             prognose = predictHoltWinters(normalizedGrid, alpha, beta, gamma, zyklusLaenge, vorhersageSchritte);
        } catch (IllegalArgumentException e) {
            // Not enough data
            return List.of();
        }

        ZonedDateTime firstTimestamp = rawData.getFirst().timestamp;
        int minuteMod = firstTimestamp.getMinute() % rasterMinutes;
        ZonedDateTime gridStart = firstTimestamp.minusMinutes(minuteMod).truncatedTo(ChronoUnit.MINUTES);

        int totalSlots = normalizedGrid.length;
        ZonedDateTime prognoseStart = gridStart.plusMinutes((long) totalSlots * rasterMinutes);

        List<FuelPriceForecastDto> result = new ArrayList<>();
        double previousForecastValue = normalizedGrid[normalizedGrid.length - 1];
        ZoneId berlinZone = ZoneId.of("Europe/Berlin");

        for (int i = 0; i < prognose.length; i++) {
            ZonedDateTime progTime = prognoseStart.plusMinutes((long) i * rasterMinutes);

            double rawForecastValue = prognose[i];
            double forecastValue = rawForecastValue;

            ZonedDateTime berlinTime = progTime.withZoneSameInstant(berlinZone);
            int berlinHour = berlinTime.getHour();
            boolean is12OClock = (berlinHour == 12);

            if (!is12OClock && rawForecastValue > previousForecastValue) {
                forecastValue = previousForecastValue;
            }

            result.add(new FuelPriceForecastDto(progTime, forecastValue));
            previousForecastValue = forecastValue;
        }

        // Vorhersage in der Datenbank speichern (jetzt neu berechnet)
        if (persist) {
            persistFuelForecast(entityId, now, forecastDuration, rasterMinutes, result);
        }
        // Cache the calculated result
        fuelForecastResultCache.put(cacheKey, result);
        return result;
    }

    /**
     * Sucht die optimalen Werte für Alpha, Beta und Gamma mithilfe eines Grid Search.
     * Es werden die Parameter gewählt, die den RMSE (Root Mean Square Error) für einen Testzeitraum von 36 Stunden minimieren.
     */
    private double[] optimizeHoltWintersParameters(double[] data, int period, int rasterMinutes) {
        int n = data.length;

        // Testzeitraum in Schritten für die Optimierung (mindestens 36 Stunden)
        int optimizationTestSteps = (36 * 60) / rasterMinutes;

        // Wir brauchen mindestens 2 Perioden + die Testschritte, um testen zu können
        if (n < period * 2 + optimizationTestSteps) {
            LOG.info("Nicht genug Daten für Parameter-Optimierung. Verwende Standardwerte.");
            return new double[]{0.4, 0.1, 0.5}; // Standardwerte
        }

        double[] bestParams = new double[]{0.3, 0.0, 0.4};
        double minRmse = Double.MAX_VALUE;

        // Wir spalten die Daten in Trainings- und Validierungsdaten auf
        int trainSize = n - optimizationTestSteps;
        double[] trainData = new double[trainSize];
        System.arraycopy(data, 0, trainData, 0, trainSize);

        double[] testData = new double[optimizationTestSteps];
        System.arraycopy(data, trainSize, testData, 0, optimizationTestSteps);

        for (double alpha = 0.0; alpha <= 1.0; alpha += 0.025) {
            for (double beta = 0.0; beta <= 1.0; beta += 0.025) {
                for (double gamma = 0.0; gamma <= 1.0; gamma += 0.025) {
                    try {
                        double[] forecast = predictHoltWinters(trainData, alpha, beta, gamma, period, optimizationTestSteps);
                        double rmse = calculateRMSE(testData, forecast);

                        if (rmse < minRmse) {
                            minRmse = rmse;
                            bestParams = new double[]{alpha, beta, gamma};
                        }
                    } catch (IllegalArgumentException e) {
                        // Passiert eigentlich nicht, da wir oben die Länge geprüft haben
                    }
                }
            }
        }

        return bestParams;
    }

    private double calculateRMSE(double[] actual, double[] predicted) {
        if (actual.length != predicted.length) {
            throw new IllegalArgumentException("Längen der Arrays müssen übereinstimmen.");
        }
        double sumSq = 0.0;
        for (int i = 0; i < actual.length; i++) {
            double diff = actual[i] - predicted[i];
            sumSq += diff * diff;
        }
        return Math.sqrt(sumSq / actual.length);
    }


    private double[] normalizeToGrid(List<PricePoint> rawData, int intervalMinutes, ZonedDateTime endTime) {
        if (rawData == null || rawData.isEmpty()) return new double[0];

        // Sicherheitshalber nach Zeit sortieren
        rawData.sort(Comparator.comparing(p -> p.timestamp));
        ZonedDateTime firstTimestamp = rawData.getFirst().timestamp;
        ZonedDateTime lastTimestamp = rawData.getLast().timestamp;

        ZonedDateTime actualEndTime = endTime.isAfter(lastTimestamp) ? endTime : lastTimestamp;

        int minuteMod = firstTimestamp.getMinute() % intervalMinutes;
        ZonedDateTime gridStart = firstTimestamp.minusMinutes(minuteMod).truncatedTo(ChronoUnit.MINUTES);

        long totalSlotsLong = ChronoUnit.MINUTES.between(gridStart, actualEndTime) / intervalMinutes + 1;
        if (totalSlotsLong <= 0) return new double[0];

        if(totalSlotsLong > Integer.MAX_VALUE){
            throw new IllegalArgumentException("Zeitraum ist zu groß");
        }

        int totalSlots = (int) totalSlotsLong;
        double[] normalizedGrid = new double[totalSlots];

        double lastKnownPrice = rawData.getFirst().price;
        int rawDataIndex = 0;

        for (int i = 0; i < totalSlots; i++) {
            ZonedDateTime currentSlotStart = gridStart.plusMinutes((long) i * intervalMinutes);
            ZonedDateTime currentSlotEnd = currentSlotStart.plusMinutes(intervalMinutes);

            List<Double> pricesInSlot = new ArrayList<>();
            while (rawDataIndex < rawData.size()) {
                PricePoint point = rawData.get(rawDataIndex);
                ZonedDateTime pointTime = point.timestamp;

                if (pointTime.isBefore(currentSlotEnd)) {
                    pricesInSlot.add(point.price);
                    rawDataIndex++;
                } else {
                    break;
                }
            }

            if (!pricesInSlot.isEmpty()) {
                lastKnownPrice = pricesInSlot.getLast();
            }
            normalizedGrid[i] = lastKnownPrice;
        }
        return normalizedGrid;
    }

    private double[] predictHoltWinters(double[] data, double alpha, double beta, double gamma, int period, int m) {
        int n = data.length;
        if (n < period * 2) {
            throw new IllegalArgumentException("Nicht genug Daten! Vorhandene Slots: " + n + ", Benötigt: " + (period * 2));
        }

        double[] forecast = new double[m];
        double[] L = new double[n];
        double[] T = new double[n];
        double[] S = new double[n + m];

        double initialLevel = 0.0;
        for (int i = 0; i < period; i++) initialLevel += data[i];
        initialLevel /= period;
        L[period - 1] = initialLevel;

        double initialTrend = 0.0;
        for (int i = 0; i < period; i++) {
            initialTrend += (data[i + period] - data[i]) / period;
        }
        initialTrend /= period;
            T[period - 1] = initialTrend;

        for (int i = 0; i < period; i++) {
            S[i] = data[i] - initialLevel;
        }

        for (int i = period; i < n; i++) {
            L[i] = alpha * (data[i] - S[i - period]) + (1 - alpha) * (L[i - 1] + T[i - 1]);
            T[i] = beta * (L[i] - L[i - 1]) + (1 - beta) * T[i - 1];
            S[i] = gamma * (data[i] - L[i]) + (1 - gamma) * S[i - period];
        }

        for (int i = 0; i < m; i++) {
            int h = i + 1;
            S[n + i] = S[n + i - period];
            forecast[i] = L[n - 1] + (h * T[n - 1]) + S[n + i];
        }

        return forecast;
    }

    @Transactional
    public void persistFuelForecast(String entityId, ZonedDateTime createdAt, Duration forecastDuration, int rasterMinutes, List<FuelPriceForecastDto> forecast) {
        if (forecast == null || forecast.isEmpty()) {
            return;
        }

        HaFuelForecast forecastEntity = new HaFuelForecast();
        forecastEntity.setEntityId(entityId);
        forecastEntity.setCreatedAt(createdAt);
        forecastEntity.setForecastDurationMinutes((int) forecastDuration.toMinutes());
        forecastEntity.setRasterMinutes(rasterMinutes);

        for (FuelPriceForecastDto dto : forecast) {
            HaFuelForecastData dataPoint = new HaFuelForecastData();
            dataPoint.setTargetTimestamp(dto.timestamp());
            dataPoint.setPredictedPrice(dto.predictedPrice());
            forecastEntity.addDataPoint(dataPoint);
        }

        em.persist(forecastEntity);
        LOG.info("Neue Tankpreis-Vorhersage für {} (Dauer: {}m, Raster: {}m) in der DB gespeichert.",
                entityId, forecastDuration.toMinutes(), rasterMinutes);
    }
}
