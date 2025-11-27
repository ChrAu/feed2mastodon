package com.hexix.homeassistant;

import com.hexix.homeassistant.dto.AttributesDto;
import com.hexix.homeassistant.dto.EntityDto;
import com.hexix.homeassistant.dto.TemperatureBucketDTO;
import com.hexix.homeassistant.entity.HaEntity;
import com.hexix.homeassistant.entity.HaStateHistory;
import com.hexix.homeassistant.entity.HaTemperatureHistory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
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
import java.util.StringJoiner;

@RequestScoped
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
}
