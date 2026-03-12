package com.hexix.homeassistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.hexix.homeassistant.dto.AttributesDto;
import com.hexix.homeassistant.dto.EntityDto;
import com.hexix.homeassistant.dto.TemperatureBucketDTO;
import com.hexix.homeassistant.entity.HaEntity;
import com.hexix.homeassistant.entity.HaStateHistory;
import com.hexix.homeassistant.entity.HaTemperatureHistory;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

@ApplicationScoped
public class HomeAssistantService {

    @ConfigProperty(name = "home-assistant.api.token")
    String apiToken;

    @Inject
    @RestClient
    HomeAssistantClient homeAssistantClient;

    @Inject
    AttributeMapperHelper attributeMapperHelper;

    @Inject
    EntityManager em;

    // Liste der relevanten IDs basierend auf deinem Input
    private static final List<String> PI_HOLE_IDS = List.of(
            "sensor.pi_hole_blockierte_anzeigen",
            "sensor.pi_hole_anteil_blockierter_anzeigen",
            "sensor.pi_hole_gesehene_clients",
            "sensor.pi_hole_dns_abfragen",
            "sensor.pi_hole_blockierte_domains",
            "sensor.pi_hole_dns_abfragen_zwischengespeichert",
            "sensor.pi_hole_dns_abfragen_weitergeleitet",
            "sensor.pi_hole_eindeutige_dns_clients",
            "sensor.pi_hole_eindeutige_dns_domains",
            "switch.pi_hole",
            "binary_sensor.pi_hole_status"
    );

    public List<EntityDto> currentState() {
        return homeAssistantClient.getAllStates("Bearer " + apiToken);
    }


    public List<List<EntityDto>> historyState(List<String> filterEntityIds) {
        return historyState(1, filterEntityIds);
    }

    public List<List<EntityDto>> historyState(int days, List<String> filterEntityIds) {
        final StringJoiner sj = new StringJoiner(",");
        filterEntityIds.forEach(sj::add);
        return homeAssistantClient.getHistorySince("Bearer " + apiToken, sj.toString());
    }

    @Transactional
    public void saveOrUpdate(EntityDto entityDto){
        final HaEntity haEntity = findOrCreateHaEntity(entityDto);


        final ZonedDateTime lastUpdated = haEntity.getLastUpdated();
        haEntity.setState(entityDto.getState());
        haEntity.setLastUpdated(ZonedDateTime.parse(entityDto.getLastUpdated(), DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        haEntity.setLastChanged(ZonedDateTime.parse(entityDto.getLastChanged(), DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        haEntity.setAttributes(attributeMapperHelper.attributesToString(entityDto.getAttributes()));

        if(lastUpdated == null || !haEntity.getLastUpdated().isEqual(lastUpdated)){
            final HaStateHistory haStateHistory = new HaStateHistory();
            haStateHistory.setEntityId(haEntity.getEntityId());
            haStateHistory.setLastChanged(haEntity.getLastUpdated());
            haStateHistory.setState(haEntity.getState());
            haStateHistory.setAttributes(haEntity.getAttributes());

            em.persist(haStateHistory);
        }


    }

    @Transactional
    public HaEntity findOrCreateHaEntity(EntityDto entityDto){
        final TypedQuery<HaEntity> query = em.createNamedQuery(HaEntity.FIND_BY_ENTITY_ID, HaEntity.class);
        query.setParameter("entityId", entityDto.getEntityId());
        final HaEntity result = query.getSingleResultOrNull();

        if(result == null){
            final HaEntity entity = new HaEntity();
            entity.setEntityId(entityDto.getEntityId());
            em.persist(entity);

            return entity;
        }
        return result;
    }

    @Transactional
    public List<HaStateHistory> getAllTemperaturData(final Duration duration) {
        final TypedQuery<HaStateHistory> query = em.createNamedQuery(HaStateHistory.FIND_All_TEMPERATUR, HaStateHistory.class);
        final long seconds = duration.toSeconds();
        final ZonedDateTime zonedDateTime = ZonedDateTime.now().minusSeconds(seconds);

        query.setParameter("entityId", "climate.%");
        query.setParameter("startDate", zonedDateTime);

        return query.getResultList();
    }

    @Transactional
    public boolean saveAllTemperaturStateHistoryWithNoTemperatureHistory() {
        List<HaStateHistory> temperatureEntities = em.createNamedQuery(HaStateHistory.FIND_All_TEMPERATUR_NO_DATA_TABLE, HaStateHistory.class).getResultList();
            temperatureEntities.forEach(haStateHistory -> {
                final AttributesDto attributesDto = attributeMapperHelper.stringToAttributes(haStateHistory.getAttributes());
                final HaTemperatureHistory haTemperatureHistory = new HaTemperatureHistory();
                haTemperatureHistory.setHaStateHistory(haStateHistory);
                haTemperatureHistory.setCurrentTemperature((Double) attributesDto.getAdditionalAttributes().get("current_temperature"));
                haTemperatureHistory.setShouldTemperature((Double) attributesDto.getAdditionalAttributes().get("temperature"));

                em.persist(haTemperatureHistory);
            });
        return !temperatureEntities.isEmpty();

    }


    public List<TemperatureBucketDTO> getAverageTemperatures(String entityId, int bucketsPerDay) {

        // Rufe die Named Query über ihren definierten Namen auf
        return em.createNamedQuery(HaStateHistory.NATIVE_FIND_AVG_TEMPERATUR_IN_BUCKETS, TemperatureBucketDTO.class)
                // Setze die Parameter, die in der Query definiert sind
                .setParameter("entityId", entityId)
                .setParameter("bucketsPerDay", bucketsPerDay)
                // Führe die Abfrage aus und erhalte die gemappte Ergebnisliste
                .getResultList();
    }

    public List<HaStateHistory> getWeatherData(final Duration duration) {
        final TypedQuery<HaStateHistory> query = em.createNamedQuery(HaStateHistory.FIND_ALL_WEATHER_DATA, HaStateHistory.class);

        return query.setParameter("startDate", ZonedDateTime.now().minusSeconds(duration.toSeconds())).getResultList();
    }

    private List<EntityDto> lastPiHoleData = null;
    private final BroadcastProcessor<List<EntityDto>> piHoleProcessor = BroadcastProcessor.create();

    void onStart(@Observes StartupEvent ev) {
        // Use the event parameter to avoid it being considered unused by static analysis
        Objects.requireNonNull(ev);
        Multi.createFrom().ticks().every(Duration.ofSeconds(30))
                .onItem().transformToUniAndConcatenate(tick ->
                        Uni.createFrom().item(this::fetchSpecificPiHoleMetrics)
                                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                )
                .subscribe().with(
                        data -> {
                            // 2. Cache bei jedem erfolgreichen Update aktualisieren
                            this.lastPiHoleData = data;
                            piHoleProcessor.onNext(data);
                        },
                        err -> System.err.println("Fehler beim Pi-Hole Live-Update: " + err.getMessage())
                );
    }

    /**
     * Holt gezielt nur die definierten Pi-Hole Entitäten einzeln ab.
     * Dies ist wesentlich schneller als alle States vom Home Assistant zu laden.
     */
    private List<EntityDto> fetchSpecificPiHoleMetrics() {
        return PI_HOLE_IDS.stream()
                .map(id -> {
                    try {
                        // Gezielter Abruf pro ID
                        return homeAssistantClient.getState("Bearer " + apiToken, id);
                    } catch (Exception e) {
                        // Logge den Fehler, aber lass den Stream weiterlaufen
                        System.err.println("Fehler beim Abruf von " + id);
                        e.printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }

    public Multi<List<EntityDto>> getPiHoleStream() {
        // 3. Wenn Cache vorhanden, diesen sofort als erstes Element senden
        if (lastPiHoleData != null) {
            return Multi.createBy().concatenating()
                    .streams(
                            Multi.createFrom().item(lastPiHoleData),
                            piHoleProcessor
                    );
        }
        return piHoleProcessor;
    }
}
