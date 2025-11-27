package com.hexix.homeassistant;

import com.hexix.homeassistant.dto.AttributesDto;
import com.hexix.homeassistant.dto.TemperatureBucketDTO;
import com.hexix.homeassistant.dto.TemperatureDeviceDto;
import com.hexix.homeassistant.dto.TemperatureDto;
import com.hexix.homeassistant.dto.WeatherDto;
import com.hexix.homeassistant.entity.HaStateHistory;
import com.hexix.util.DurationLogger;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/api/homeassistant")
public class HomeAssistantResource {
    private final Logger LOG = Logger.getLogger(this.getClass());

    @Inject
    HomeAssistantService homeAssistantService;

    @Inject
    AttributeMapperHelper attributeMapperHelper;

    @Inject
    WeatherMapper weatherMapper;


    @GET
    @Path("/temperature-old")
    @Produces(MediaType.APPLICATION_JSON)
    public List<TemperatureDeviceDto> getTemperatureDataOld() {
        final List<HaStateHistory> allTemperaturData = homeAssistantService.getAllTemperaturData(Duration.ofDays(2));


        final List<TemperatureDto> allTemperaturDtos = allTemperaturData.stream()
                .filter(haStateHistory -> {
                    final AttributesDto attributesDto = attributeMapperHelper.stringToAttributes(haStateHistory.getAttributes());
                    final Map<String, Object> map = attributesDto.getAdditionalAttributes();
                    return map.get("current_temperature") != null && map.get("temperature") != null;

                })
                .map(haStateHistory -> {
                    final AttributesDto attributesDto = attributeMapperHelper.stringToAttributes(haStateHistory.getAttributes());
                    final String friendlyName = attributesDto.getFriendlyName();
                    final Number currentTemperature = (Number) attributesDto.getAdditionalAttributes().get("current_temperature");
                    final Number shouldTemperatur = (Number) attributesDto.getAdditionalAttributes().get("temperature");
                    return new TemperatureDto(haStateHistory.getEntityId(), friendlyName, currentTemperature, shouldTemperatur, haStateHistory.getLastChanged(), haStateHistory.getState());
                }).toList();

        return allTemperaturDtos.stream()
                .collect(Collectors.groupingBy(temperaturDto -> new AbstractMap.SimpleEntry<>(temperaturDto.entityId(), temperaturDto.friendlyName())))
                .entrySet().stream()
                .map(entry -> new TemperatureDeviceDto(entry.getKey().getKey(), entry.getKey().getValue(), entry.getValue()))
                .toList();
    }

    @GET
    @Path("/test")
    @Produces(MediaType.APPLICATION_JSON)
    public List<TemperatureBucketDTO> getAverageTemperatures(@QueryParam("entityId") String entityId, @QueryParam("bucketsPerDay") int bucketsPerDay) {

        try(DurationLogger d = new DurationLogger("Query der TemperaturBucketDto abfrage", LOG)){
            return homeAssistantService.getAverageTemperatures(entityId, bucketsPerDay);
        }

    }

    @GET
    @Path("/weather")
    @Produces(MediaType.APPLICATION_JSON)
    public List<WeatherDto> getAttributeMapperHelper() {
        List<HaStateHistory> weatherData = homeAssistantService.getWeatherData(Duration.ofDays(7));


        return weatherData.stream().map(haStateHistory -> {
            final AttributesDto attributesDto = attributeMapperHelper.stringToAttributes(haStateHistory.getAttributes());

            final WeatherDto weatherDto = new WeatherDto(
                    haStateHistory.getState(),
                    weatherMapper.getGermanTranslation(haStateHistory.getState()),
                    attributesDto.getFriendlyName(),
                    (Double) attributesDto.getAdditionalAttributes().get("pressure"),
                    (String) attributesDto.getAdditionalAttributes().get("pressure_unit"),
                    (Double) attributesDto.getAdditionalAttributes().get("temperature"),
                    (Double) attributesDto.getAdditionalAttributes().get("apparent_temperature"),
                    (String) attributesDto.getAdditionalAttributes().get("temperature_unit"),
                    (Integer) attributesDto.getAdditionalAttributes().get("humidity"),
                    (Double) attributesDto.getAdditionalAttributes().get("wind_speed"),
                    (Integer) attributesDto.getAdditionalAttributes().get("wind_bearing"),
                    (String) attributesDto.getAdditionalAttributes().get("wind_speed_unit"),
                    (String) attributesDto.getAdditionalAttributes().get("precipitation_unit"),
                    haStateHistory.getLastChanged());


            return weatherDto;
        }).toList();
    }

    @GET
    @Path("/temperature")
    @Produces(MediaType.APPLICATION_JSON)
    public List<TemperatureDeviceDto> getTemperatureData() {
        boolean hasNext;
        do{
            hasNext = homeAssistantService.saveAllTemperaturStateHistoryWithNoTemperatureHistory();
        }while (hasNext);



        final List<HaStateHistory> allTemperaturData = homeAssistantService.getAllTemperaturData(Duration.ofDays(2));


        final List<TemperatureDto> allTemperaturDtos = allTemperaturData.stream()
                .filter(haStateHistory -> {
                    final AttributesDto attributesDto = attributeMapperHelper.stringToAttributes(haStateHistory.getAttributes());
                    final Map<String, Object> map = attributesDto.getAdditionalAttributes();
                    return map.get("current_temperature") != null && map.get("temperature") != null;

                })
                .map(haStateHistory -> {
                    final AttributesDto attributesDto = attributeMapperHelper.stringToAttributes(haStateHistory.getAttributes());
                    final String friendlyName = attributesDto.getFriendlyName();
                    final Number currentTemperature = (Number) attributesDto.getAdditionalAttributes().get("current_temperature");
                    final Number shouldTemperatur = (Number) attributesDto.getAdditionalAttributes().get("temperature");
                    return new TemperatureDto(haStateHistory.getEntityId(), friendlyName, currentTemperature, shouldTemperatur, haStateHistory.getLastChanged(), haStateHistory.getState());
                }).toList();

        return allTemperaturDtos.stream()
                .collect(Collectors.groupingBy(temperaturDto -> new AbstractMap.SimpleEntry<>(temperaturDto.entityId(), temperaturDto.friendlyName())))
                .entrySet().stream()
                .map(entry -> new TemperatureDeviceDto(entry.getKey().getKey(), entry.getKey().getValue(), entry.getValue()))
                .toList();
    }
}
