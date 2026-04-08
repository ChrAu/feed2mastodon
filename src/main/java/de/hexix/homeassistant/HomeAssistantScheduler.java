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

}
