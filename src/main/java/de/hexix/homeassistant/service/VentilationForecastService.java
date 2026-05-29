package de.hexix.homeassistant.service;

import de.hexix.homeassistant.HomeAssistantClient;
import de.hexix.homeassistant.HomeAssistantService;
import de.hexix.homeassistant.dto.EntityDto;
import de.hexix.homeassistant.dto.EntityStateUpdateRequest;
import de.hexix.homeassistant.entity.HaStateHistory;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class VentilationForecastService {

    @ConfigProperty(name = "home-assistant.api.token")
    String apiToken;

    @Inject
    @RestClient
    HomeAssistantClient homeAssistantClient;

    @Inject
    HomeAssistantService homeAssistantService;

    @Inject
    HoltWinterForecastService holtWinterForecastService;

    @Inject
    jakarta.persistence.EntityManager em;

    private static final String ENTITY_OUTDOOR_TEMP = "sensor.balkon_thermometer_temperatur";
    private static final String ENTITY_OUTDOOR_ABS_HUM = "sensor.thermal_comfort_absolute_luftfeuchtigkeit";

    private static final String ENTITY_INDOOR_WOHN_TEMP = "sensor.wohnzimmer_thermometer_temperatur";
    private static final String ENTITY_INDOOR_SCHLAF_TEMP = "sensor.schlafzimmer_thermometer_temperatur";

    private static final String ENTITY_INDOOR_WOHN_ABS_HUM = "sensor.wohnzimmer_absolute_luftfeuchtigkeit";
    private static final String ENTITY_INDOOR_SCHLAF_ABS_HUM = "sensor.schlafzimmer_absolute_luftfeuchtigkeit";

    private static final String ENTITY_WINDOW_OPEN = "input_boolean.lueftung_fenster_offen";

    private static final String TARGET_PREDICTION_ENTITY = "sensor.lueftung_vorhersage";

    public void calculateAndPushVentilationForecast() {
        Log.info("Starting ventilation forecast calculation...");

        try {
            // 1. Get current values
            EntityDto wohnTempDto = fetchStateSafe(ENTITY_INDOOR_WOHN_TEMP);
            EntityDto schlafTempDto = fetchStateSafe(ENTITY_INDOOR_SCHLAF_TEMP);
            EntityDto wohnAbsHumDto = fetchStateSafe(ENTITY_INDOOR_WOHN_ABS_HUM);
            EntityDto schlafAbsHumDto = fetchStateSafe(ENTITY_INDOOR_SCHLAF_ABS_HUM);
            EntityDto windowOpenDto = fetchStateSafe(ENTITY_WINDOW_OPEN);
            EntityDto outdoorTempDto = fetchStateSafe(ENTITY_OUTDOOR_TEMP);
            EntityDto outdoorAbsHumDto = fetchStateSafe(ENTITY_OUTDOOR_ABS_HUM);

            double tWohn = parseDoubleState(wohnTempDto, 21.0);
            double tSchlaf = parseDoubleState(schlafTempDto, 21.0);
            double absFWohn = parseDoubleState(wohnAbsHumDto, 10.0);
            double absFSchlaf = parseDoubleState(schlafAbsHumDto, 10.0);

            boolean windowOpen = windowOpenDto != null && "on".equalsIgnoreCase(windowOpenDto.getState());

            double tInnenSchnitt = round((tWohn + tSchlaf) / 2.0, 1);
            double absFInnenSchnitt = round((absFWohn + absFSchlaf) / 2.0, 1);

            double tAussenCurrent = parseDoubleState(outdoorTempDto, 15.0);
            double absFAussenCurrent = parseDoubleState(outdoorAbsHumDto, 8.0);

            // 2. Try to fetch weather forecast from Home Assistant (weather.gosbach or weather.forecast_home)
            List<HoltWinterForecastService.GenericForecastPoint> tempForecast = fetchForecastFromEntity("weather.gosbach", true);
            List<HoltWinterForecastService.GenericForecastPoint> humForecast = fetchForecastFromEntity("weather.gosbach", false);

            if (tempForecast.isEmpty() || humForecast.isEmpty()) {
                Log.info("weather.gosbach returned no forecast, trying weather.forecast_home...");
                tempForecast = fetchForecastFromEntity("weather.forecast_home", true);
                humForecast = fetchForecastFromEntity("weather.forecast_home", false);
            }

            boolean usedWeatherForecast = !tempForecast.isEmpty() && !humForecast.isEmpty();

            if (!usedWeatherForecast) {
                Log.info("No weather forecast available from Home Assistant. Falling back to Holt-Winters model...");
                List<HaStateHistory> tempHistory = homeAssistantService.getHaStateHistory(ENTITY_OUTDOOR_TEMP, null, Duration.ofDays(7));
                List<HaStateHistory> humHistory = homeAssistantService.getHaStateHistory(ENTITY_OUTDOOR_ABS_HUM, null, Duration.ofDays(7));

                if (tempHistory.isEmpty() || humHistory.isEmpty()) {
                    Log.warn("Cannot calculate forecast: outdoor temperature or absolute humidity history is empty in DB.");
                    return;
                }

                tempForecast = holtWinterForecastService.calculateGenericForecast(tempHistory, Duration.ofHours(48), 10);
                humForecast = holtWinterForecastService.calculateGenericForecast(humHistory, Duration.ofHours(48), 10);
            } else {
                Log.info("Using actual Home Assistant weather forecast for predictions.");
            }

            if (tempForecast.isEmpty() || humForecast.isEmpty()) {
                Log.warn("Forecast calculation returned empty results.");
                return;
            }

            // 2.1 Fetch indoor temperature forecasts
            List<HaStateHistory> wohnTempHistory = homeAssistantService.getHaStateHistory(ENTITY_INDOOR_WOHN_TEMP, null, Duration.ofDays(7));
            List<HaStateHistory> schlafTempHistory = homeAssistantService.getHaStateHistory(ENTITY_INDOOR_SCHLAF_TEMP, null, Duration.ofDays(7));
            List<HoltWinterForecastService.GenericForecastPoint> wohnTempForecast = 
                    holtWinterForecastService.calculateGenericForecast(wohnTempHistory, Duration.ofHours(48), 10);
            List<HoltWinterForecastService.GenericForecastPoint> schlafTempForecast = 
                    holtWinterForecastService.calculateGenericForecast(schlafTempHistory, Duration.ofHours(48), 10);
            
            Map<ZonedDateTime, Double> indoorTempForecastMap = averageForecasts(wohnTempForecast, schlafTempForecast, tInnenSchnitt);

            // 2.2 Fetch indoor humidity forecasts
            List<HaStateHistory> wohnHumHistory = homeAssistantService.getHaStateHistory(ENTITY_INDOOR_WOHN_ABS_HUM, null, Duration.ofDays(7));
            List<HaStateHistory> schlafHumHistory = homeAssistantService.getHaStateHistory(ENTITY_INDOOR_SCHLAF_ABS_HUM, null, Duration.ofDays(7));
            List<HoltWinterForecastService.GenericForecastPoint> wohnHumForecast = 
                    holtWinterForecastService.calculateGenericForecast(wohnHumHistory, Duration.ofHours(48), 10);
            List<HoltWinterForecastService.GenericForecastPoint> schlafHumForecast = 
                    holtWinterForecastService.calculateGenericForecast(schlafHumHistory, Duration.ofHours(48), 10);

            Map<ZonedDateTime, Double> indoorHumForecastMap = averageForecasts(wohnHumForecast, schlafHumForecast, absFInnenSchnitt);

            Map<ZonedDateTime, Double> humMap = humForecast.stream()
                    .collect(Collectors.toMap(p -> p.timestamp(), p -> p.value(), (a, b) -> a));

            ZonedDateTime nextOpenTime = null;
            ZonedDateTime nextCloseTime = null;

            for (var tPoint : tempForecast) {
                ZonedDateTime ts = tPoint.timestamp();
                Double absFAussen = humMap.get(ts);
                if (absFAussen == null) {
                    continue;
                }

                double tAussen = tPoint.value();
                double tInnen = indoorTempForecastMap.getOrDefault(ts, tInnenSchnitt);
                double absFInnen = indoorHumForecastMap.getOrDefault(ts, absFInnenSchnitt);

                // Logic from automation:
                // kuehl_und_trocken: t_aussen < (t_innen - 0.5) and t_aussen < 26 and abs_f_aussen < (abs_f_innen - 0.5)
                boolean kuehlUndTrocken = tAussen < (tInnen - 0.5)
                        && tAussen < 26.0
                        && absFAussen < (absFInnen - 0.5);

                // zu_warm_oder_schwuelfucht: t_aussen >= (t_innen + 0.5) or t_aussen >= 28 or abs_f_aussen >= abs_f_innen
                boolean zuWarmOderSchwuelfucht = tAussen >= (tInnen + 0.5)
                        || tAussen >= 28.0
                        || absFAussen >= absFInnen;

                if (kuehlUndTrocken && nextOpenTime == null) {
                    nextOpenTime = ts;
                }

                if (zuWarmOderSchwuelfucht && nextCloseTime == null) {
                    nextCloseTime = ts;
                }

                if (nextOpenTime != null && nextCloseTime != null) {
                    break;
                }
            }

            // 4. Construct response body
            String nextAction = windowOpen ? "schließen" : "öffnen";
            ZonedDateTime primaryTargetTime = windowOpen ? nextCloseTime : nextOpenTime;
            if (primaryTargetTime == null && !tempForecast.isEmpty()) {
                primaryTargetTime = tempForecast.get(tempForecast.size() - 1).timestamp();
                Log.infof("No matching time found for action '%s'. Falling back to the end of the forecast period: %s", nextAction, primaryTargetTime);
            }
            String stateStr = (primaryTargetTime != null) ? DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(primaryTargetTime) : "unknown";


            Map<String, Object> attrs = new HashMap<>();
            attrs.put("device_class", "timestamp");
            attrs.put("friendly_name", "Lüftungsempfehlung Vorhersage");
            attrs.put("icon", windowOpen ? "mdi:window-closed-variant" : "mdi:window-open-variant");
            attrs.put("next_action", nextAction);
            attrs.put("next_open_time", (nextOpenTime != null) ? DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(nextOpenTime) : null);
            attrs.put("next_close_time", (nextCloseTime != null) ? DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(nextCloseTime) : null);
            attrs.put("t_innen_schnitt", tInnenSchnitt);
            attrs.put("abs_f_innen_schnitt", absFInnenSchnitt);
            attrs.put("t_aussen_current", round(tAussenCurrent, 1));
            attrs.put("abs_f_aussen_current", round(absFAussenCurrent, 1));
            attrs.put("current_window_state", windowOpen ? "open" : "closed");
            attrs.put("forecast_method", usedWeatherForecast ? "weather_forecast" : "holt_winters");
            attrs.put("calculated_at", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now()));

            EntityStateUpdateRequest request = new EntityStateUpdateRequest(stateStr, attrs);

            // 5. Post to Home Assistant
            EntityDto result = homeAssistantClient.postState("Bearer " + apiToken, TARGET_PREDICTION_ENTITY, request);
            Log.infof("Successfully updated Home Assistant prediction entity. State: %s, Next Action: %s", stateStr, nextAction);

        } catch (Exception e) {
            Log.error("Error calculating or pushing ventilation forecast", e);
        }
    }

    private EntityDto fetchStateSafe(String entityId) {
        try {
            return homeAssistantClient.getState("Bearer " + apiToken, entityId);
        } catch (Exception e) {
            Log.warnf("Failed to fetch current state for %s from Home Assistant: %s", entityId, e.getMessage());
            return null;
        }
    }

    private double parseDoubleState(EntityDto entity, double defaultValue) {
        if (entity == null || entity.getState() == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(entity.getState());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    @jakarta.transaction.Transactional
    public void generateDemoData() {
        Log.info("Deleting old ventilation history data for demo setup...");

        em.createQuery("DELETE FROM HaStateHistory h WHERE h.entityId IN :ids")
                .setParameter("ids", List.of(
                        ENTITY_OUTDOOR_TEMP,
                        ENTITY_OUTDOOR_ABS_HUM,
                        ENTITY_INDOOR_WOHN_TEMP,
                        ENTITY_INDOOR_SCHLAF_TEMP,
                        ENTITY_INDOOR_WOHN_ABS_HUM,
                        ENTITY_INDOOR_SCHLAF_ABS_HUM,
                        ENTITY_WINDOW_OPEN
                ))
                .executeUpdate();

        Log.info("Generating clean demo data for the last 8 days...");
        ZonedDateTime start = ZonedDateTime.now().minusDays(8);
        ZonedDateTime end = ZonedDateTime.now();

        for (ZonedDateTime ts = start; ts.isBefore(end); ts = ts.plusMinutes(30)) {
            double hour = ts.getHour() + ts.getMinute() / 60.0;

            // Outdoor: Temp min 12°C, max 24°C; Hum min 7.0, max 11.0
            double outdoorTemp = 18.0 + 6.0 * Math.sin(2 * Math.PI * (hour - 9.0) / 24.0) + (Math.random() - 0.5) * 0.5;
            double outdoorAbsHum = 9.0 + 2.0 * Math.sin(2 * Math.PI * (hour - 10.0) / 24.0) + (Math.random() - 0.5) * 0.3;

            // Indoor Wohnzimmer: Temp around 21.5°C, Hum around 10.5
            double wohnTemp = 21.5 + (Math.random() - 0.5) * 0.2;
            double wohnAbsHum = 10.5 + (Math.random() - 0.5) * 0.1;

            // Indoor Schlafzimmer: Temp around 20.0°C, Hum around 9.8
            double schlafTemp = 20.0 + (Math.random() - 0.5) * 0.2;
            double schlafAbsHum = 9.8 + (Math.random() - 0.5) * 0.1;

            persistStateHistory(ENTITY_OUTDOOR_TEMP, String.format(java.util.Locale.US, "%.1f", outdoorTemp), ts, "{\"friendly_name\": \"Balkon Thermometer Temperatur\", \"unit_of_measurement\": \"°C\"}");
            persistStateHistory(ENTITY_OUTDOOR_ABS_HUM, String.format(java.util.Locale.US, "%.1f", outdoorAbsHum), ts, "{\"friendly_name\": \"Balkon absolute Luftfeuchtigkeit\", \"unit_of_measurement\": \"g/m³\"}");

            persistStateHistory(ENTITY_INDOOR_WOHN_TEMP, String.format(java.util.Locale.US, "%.1f", wohnTemp), ts, "{\"friendly_name\": \"Wohnzimmer Thermometer Temperatur\", \"unit_of_measurement\": \"°C\"}");
            persistStateHistory(ENTITY_INDOOR_SCHLAF_TEMP, String.format(java.util.Locale.US, "%.1f", schlafTemp), ts, "{\"friendly_name\": \"Schlafzimmer Thermometer Temperatur\", \"unit_of_measurement\": \"°C\"}");

            persistStateHistory(ENTITY_INDOOR_WOHN_ABS_HUM, String.format(java.util.Locale.US, "%.1f", wohnAbsHum), ts, "{\"friendly_name\": \"Wohnzimmer absolute Luftfeuchtigkeit\", \"unit_of_measurement\": \"g/m³\"}");
            persistStateHistory(ENTITY_INDOOR_SCHLAF_ABS_HUM, String.format(java.util.Locale.US, "%.1f", schlafAbsHum), ts, "{\"friendly_name\": \"Schlafzimmer absolute Luftfeuchtigkeit\", \"unit_of_measurement\": \"g/m³\"}");
        }

        // Window open helper: off
        persistStateHistory(ENTITY_WINDOW_OPEN, "off", end, "{\"friendly_name\": \"lueftung_fenster_offen\"}");
        Log.info("Demo data generation successfully completed!");
    }

    private void persistStateHistory(String entityId, String state, ZonedDateTime ts, String attrs) {
        HaStateHistory h = new HaStateHistory();
        h.setEntityId(entityId);
        h.setState(state);
        h.setLastChanged(ts);
        h.setAttributes(attrs);
        em.persist(h);
    }

    private List<HoltWinterForecastService.GenericForecastPoint> fetchForecastFromEntity(String entityId, boolean temperature) {
        try {
            Map<String, Object> body = Map.of(
                "entity_id", entityId,
                "type", "hourly"
            );
            Map<String, Object> response = homeAssistantClient.getWeatherForecasts("Bearer " + apiToken, body);
            if (response != null && response.containsKey(entityId)) {
                Map<String, Object> entityForecast = (Map<String, Object>) response.get(entityId);
                List<Map<String, Object>> forecastList = (List<Map<String, Object>>) entityForecast.get("forecast");

                if (forecastList != null && !forecastList.isEmpty()) {
                    List<HoltWinterForecastService.GenericForecastPoint> points = new java.util.ArrayList<>();
                    for (Map<String, Object> f : forecastList) {
                        String dtStr = (String) f.get("datetime");
                        Number tempNum = (Number) f.get("temperature");
                        Number humNum = (Number) f.get("humidity");

                        if (dtStr != null && tempNum != null) {
                            ZonedDateTime ts = ZonedDateTime.parse(dtStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                            double temp = tempNum.doubleValue();
                            double val;
                            if (temperature) {
                                val = temp;
                            } else {
                                double humidity = (humNum != null) ? humNum.doubleValue() : 50.0;
                                val = calculateAbsoluteHumidity(temp, humidity);
                            }
                            points.add(new HoltWinterForecastService.GenericForecastPoint(ts, val));
                        }
                    }
                    return points;
                }
            }
        } catch (Exception e) {
            Log.warnf("Failed to fetch forecast from %s: %s", entityId, e.getMessage());
        }
        return List.of();
    }

    private double calculateAbsoluteHumidity(double temp, double relativeHumidity) {
        // Clausius-Clapeyron / Magnus formula
        double vaporPressure = (relativeHumidity / 100.0) * 6.112 * Math.exp((17.67 * temp) / (temp + 243.5));
        double absHum = (216.7 * vaporPressure) / (273.15 + temp);
        return absHum;
    }

    private Map<ZonedDateTime, Double> averageForecasts(
        List<HoltWinterForecastService.GenericForecastPoint> forecast1,
        List<HoltWinterForecastService.GenericForecastPoint> forecast2,
        double fallbackValue
    ) {
        if (forecast1.isEmpty() && forecast2.isEmpty()) {
            return Map.of();
        }

        Map<ZonedDateTime, Double> f2Map = forecast2.stream()
                .collect(Collectors.toMap(p -> p.timestamp(), p -> p.value(), (a, b) -> a));

        Map<ZonedDateTime, Double> resultMap = new HashMap<>();

        if (forecast1.isEmpty()) {
            for (var p : forecast2) {
                resultMap.put(p.timestamp(), p.value());
            }
            return resultMap;
        }

        if (forecast2.isEmpty()) {
            for (var p : forecast1) {
                resultMap.put(p.timestamp(), p.value());
            }
            return resultMap;
        }

        for (var p1 : forecast1) {
            ZonedDateTime ts = p1.timestamp();
            Double v2 = f2Map.get(ts);
            if (v2 != null) {
                resultMap.put(ts, round((p1.value() + v2) / 2.0, 2));
            }
        }
        return resultMap;
    }
}
