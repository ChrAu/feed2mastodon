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

            // 2. Fetch history (7 days) for outdoor values
            List<HaStateHistory> tempHistory = homeAssistantService.getHaStateHistory(ENTITY_OUTDOOR_TEMP, null, Duration.ofDays(7));
            List<HaStateHistory> humHistory = homeAssistantService.getHaStateHistory(ENTITY_OUTDOOR_ABS_HUM, null, Duration.ofDays(7));

            if (tempHistory.isEmpty() || humHistory.isEmpty()) {
                Log.warn("Cannot calculate forecast: outdoor temperature or absolute humidity history is empty in DB.");
                return;
            }

            // 3. Calculate forecast for next 48 hours in 10-minute intervals
            List<HoltWinterForecastService.GenericForecastPoint> tempForecast =
                    holtWinterForecastService.calculateGenericForecast(tempHistory, Duration.ofHours(48), 10);
            List<HoltWinterForecastService.GenericForecastPoint> humForecast =
                    holtWinterForecastService.calculateGenericForecast(humHistory, Duration.ofHours(48), 10);

            if (tempForecast.isEmpty() || humForecast.isEmpty()) {
                Log.warn("Holt-Winters forecast calculation returned empty results.");
                return;
            }

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

                // Logic from automation:
                // kuehl_und_trocken: t_aussen < (t_innen_schnitt - 0.5) and t_aussen < 26 and abs_f_aussen < (abs_f_innen_schnitt - 0.5)
                boolean kuehlUndTrocken = tAussen < (tInnenSchnitt - 0.5)
                        && tAussen < 26.0
                        && absFAussen < (absFInnenSchnitt - 0.5);

                // zu_warm_oder_schwuelfucht: t_aussen >= (t_innen_schnitt + 0.5) or t_aussen >= 28 or abs_f_aussen >= abs_f_innen_schnitt
                boolean zuWarmOderSchwuelfucht = tAussen >= (tInnenSchnitt + 0.5)
                        || tAussen >= 28.0
                        || absFAussen >= absFInnenSchnitt;

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
}
