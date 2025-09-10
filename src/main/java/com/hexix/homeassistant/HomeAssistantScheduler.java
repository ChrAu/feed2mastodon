package com.hexix.homeassistant;

import com.hexix.homeassistant.dto.AttributesDto;
import com.hexix.homeassistant.dto.EntityDto;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
    public void test(){
        final List<EntityDto> entityDtos = homeAssistantService.currentState();
        final List<String> filterEntityIds = entityDtos.stream().map(EntityDto::getEntityId).filter(entityId -> entityId.contains("climate.")).distinct().toList();

        final List<List<EntityDto>> lists = homeAssistantService.historyState(filterEntityIds);
        final ZonedDateTime parse = ZonedDateTime.parse(lists.getFirst().getFirst().getLastUpdated(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        final LocalDateTime localDateTimeUTC = parse.toLocalDateTime();
        final ZonedDateTime zonedDateTime = parse.withZoneSameInstant(ZoneId.systemDefault());
        final LocalDateTime localDateTimeBerlin = zonedDateTime.toLocalDateTime();


        System.out.println(entityDtos);
    }

    @Scheduled(every = "1m" )
    public void saveState(){
        final List<EntityDto> entityDtos = homeAssistantService.currentState();

       entityDtos.forEach(entityDto -> homeAssistantService.saveOrUpdate(entityDto));

//        final List<EntityDto> climateDevices = entityDtos.stream().filter(entityDto -> entityDto.getEntityId().contains("climate.")).toList();
//
//        final EntityDto first = climateDevices.getFirst();
//        final AttributesDto attributes = first.getAttributes();
//
//        final String s = attributeMapperHelper.attributesToString(attributes);
//
//        final AttributesDto attributesDto = attributeMapperHelper.stringToAttributes(s);
//
//        System.out.println();
    }

}
