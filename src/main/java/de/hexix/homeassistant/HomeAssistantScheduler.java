package de.hexix.homeassistant;

import de.hexix.homeassistant.dto.EntityDto;
import de.hexix.util.DurationLogger;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@ApplicationScoped
public class HomeAssistantScheduler {

    @Inject
    HomeAssistantService homeAssistantService;

    @Inject
    AttributeMapperHelper attributeMapperHelper;

    //    @Scheduled(every = "1m" )
    public void test() {
        final List<EntityDto> entityDtos = homeAssistantService.currentState();
        final List<String> filterEntityIds = entityDtos.stream().map(EntityDto::getEntityId).filter(entityId -> entityId.contains("climate.")).distinct().toList();

        final List<List<EntityDto>> lists = homeAssistantService.historyState(filterEntityIds);
        final ZonedDateTime parse = ZonedDateTime.parse(lists.getFirst().getFirst().getLastUpdated(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        final LocalDateTime localDateTimeUTC = parse.toLocalDateTime();
        final ZonedDateTime zonedDateTime = parse.withZoneSameInstant(ZoneId.systemDefault());
        final LocalDateTime localDateTimeBerlin = zonedDateTime.toLocalDateTime();


        System.out.println(entityDtos);
    }

    @Scheduled(every = "30s")
    public void saveState() {
        try (DurationLogger d = new DurationLogger("HomeAssistantScheduler.saveState()", Logger.getLogger(this.getClass()))) {
            final List<EntityDto> entityDtos = homeAssistantService.currentState();

            entityDtos.forEach(entityDto -> homeAssistantService.saveOrUpdate(entityDto));
        }
    }

    @Scheduled(every = "1h") // Läuft einmal pro Stunde
    public void cleanupOldForecasts() {
        try (DurationLogger d = new DurationLogger("HomeAssistantScheduler.cleanupOldForecasts()", Logger.getLogger(this.getClass()))) {
            int deleted = homeAssistantService.cleanupOldForecasts(36);
            if (deleted > 0) {
                Logger.getLogger(this.getClass()).infof("%d alte Vorhersagen (älter als 24 Stunden) wurden gelöscht.", deleted);
            }
        } catch (Exception e) {
            Logger.getLogger(this.getClass()).error("Fehler beim Löschen alter Vorhersagen", e);
        }
    }

    @Scheduled(cron = "0 0 8,18 * * ?")   // Täglich um 08:00 und 18:00 Uhr
    @Scheduled(cron = "0 30 12 * * ?")    // Täglich um 12:30 Uhr
    public void scheduleForecastStorage() {
        try (DurationLogger d = new DurationLogger("HomeAssistantScheduler.scheduleForecastStorage()", Logger.getLogger(this.getClass()))) {

            // Wir iterieren über alle Kraftstoff-Entitäten, die im Service definiert sind
            for (String entityId : HomeAssistantService.FUEL_PRICE_IDS) {
                // Wir filtern die Status-Sensoren aus, da wir nur für Preise (sensor.) Prognosen brauchen
                if (entityId.startsWith("sensor.")) {
                    try {
                        // Der Aufruf triggert automatisch die Berechnung und das Speichern in der DB
                        // Wir nutzen 30 Tage Historie und 48 Stunden Prognose im 10-Minuten-Raster
                        homeAssistantService.getFuelPriceForecast(
                                entityId,
                                java.time.Duration.ofDays(365),
                                java.time.Duration.ofHours(48),
                                10
                        );
                    } catch (Exception e) {
                        Logger.getLogger(this.getClass()).errorf("Fehler bei automatischer Prognose für %s: %s", entityId, e.getMessage());
                    }
                }
            }

        }
    }

}
