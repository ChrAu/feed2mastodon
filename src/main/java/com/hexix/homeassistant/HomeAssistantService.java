package com.hexix.homeassistant;

import com.hexix.homeassistant.dto.EntityDto;
import com.hexix.homeassistant.entity.HaEntity;
import com.hexix.homeassistant.entity.HaStateHistory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
            haStateHistory.setLastChanged(haEntity.getLastChanged());
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
}
