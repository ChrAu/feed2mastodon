package de.hexix.homeassistant;

import de.hexix.homeassistant.dto.AttributesDto;
import de.hexix.homeassistant.dto.CarDataDto;
import de.hexix.homeassistant.dto.CpuDto;
import de.hexix.homeassistant.dto.ElectricityPriceHistoryDto;
import de.hexix.homeassistant.dto.ElectricityPriceOverviewDto;
import de.hexix.homeassistant.dto.EntityDto;
import de.hexix.homeassistant.dto.FuelPriceForecastDto;
import de.hexix.homeassistant.dto.FuelPriceHistoryDto;
import de.hexix.homeassistant.dto.FuelStationDto;
import de.hexix.homeassistant.dto.TemperatureBucketDTO;
import de.hexix.homeassistant.dto.TemperatureDeviceDto;
import de.hexix.homeassistant.dto.TemperatureDto;
import de.hexix.homeassistant.dto.WeatherDto;
import de.hexix.homeassistant.entity.HaEntity;
import de.hexix.homeassistant.entity.HaStateHistory;
import de.hexix.util.DurationLogger;
import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestStreamElementType;

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

    @GET
    @Path("/fuel-prices")
    @Produces(MediaType.APPLICATION_JSON)
    public List<FuelStationDto> getFuelPrices() {
        return homeAssistantService.getFuelPrices();
    }

    @GET
    @Path("/fuel-prices/history")
    @Produces(MediaType.APPLICATION_JSON)
    public List<FuelPriceHistoryDto> getFuelPriceHistory(
            @QueryParam("entityId") String entityId,
            @QueryParam("durationHours") int durationHours) {
        return homeAssistantService.getFuelPriceHistory(entityId, Duration.ofHours(durationHours));
    }

    @GET
    @Path("/fuel-prices/forecast")
    @Produces(MediaType.APPLICATION_JSON)
    public List<FuelPriceForecastDto> getFuelPriceForecast(
            @QueryParam("entityId") String entityId,
            @QueryParam("historyDays") @DefaultValue("3650") long historyDays,
            @QueryParam("forecastHours") @DefaultValue("12") int forecastHours) {
        return homeAssistantService.getFuelPriceForecast(entityId, Duration.ofDays(historyDays), Duration.ofHours(forecastHours), 10);
    }

    @GET
    @Path("/fuel-prices/forecast/saved")
    @Produces(MediaType.APPLICATION_JSON)
    public List<de.hexix.homeassistant.dto.SavedForecastDto> getSavedForecasts(@QueryParam("entityId") String entityId) {
        return homeAssistantService.getSavedForecasts(entityId);
    }

    // New Electricity Price Endpoints
    @GET
    @Path("/electricity-price")
    @Produces(MediaType.APPLICATION_JSON)
    public ElectricityPriceOverviewDto getElectricityPriceOverview() { // Changed return type and method name
        return homeAssistantService.getElectricityPriceOverview();
    }

    @GET
    @Path("/electricity-price/history")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ElectricityPriceHistoryDto> getElectricityPriceHistory(
            @QueryParam("entityId") String entityId,
            @QueryParam("durationHours") int durationHours) {
        return homeAssistantService.getElectricityPriceHistory(entityId, Duration.ofHours(durationHours));
    }

    @GET
    @Path("/car-data")
    @Produces(MediaType.APPLICATION_JSON)
    public CarDataDto getCarData() {
        return homeAssistantService.getCarData();
    }

    @GET
    @Path("/car-data/history")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ElectricityPriceHistoryDto> getCarDataHistory(
            @QueryParam("entityId") String entityId,
            @QueryParam("durationHours") int durationHours) {
        return homeAssistantService.getCarDataHistory(entityId, Duration.ofHours(durationHours));
    }

    @GET
    @Path("/cpu/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<List<CpuDto>> streamCpuData() {
        return homeAssistantService.getCpuStream();
    }

    @GET
    @Path("/pihole/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<List<EntityDto>> streamPiHoleData() {
        return homeAssistantService.getPiHoleStream();
    }

    @Authenticated
    @GET
    @Path("/state-history")
    @Produces(MediaType.APPLICATION_JSON)
    public List<HaStateHistory> getHaStateHistory(
            @QueryParam("entityId") String entityId,
            @QueryParam("entityIdPrefix") String entityIdPrefix,
            @QueryParam("days") Long days,
            @QueryParam("duration") String durationString) { // durationString to parse into Duration
        Duration duration = null;
        if (durationString != null) {
            try {
                duration = Duration.parse(durationString);
            } catch (Exception e) {
                LOG.warn("Invalid duration string: " + durationString + ". Using default (1 year).", e);
            }
        }
        if (duration == null && days != null) {
            duration = Duration.ofDays(days);
        }
        if (duration == null) {
            duration = Duration.ofDays(365); // Default to 1 year
        }

        return homeAssistantService.getHaStateHistory(entityId, entityIdPrefix, duration);
    }

    @Authenticated
    @GET
    @Path("/entities")
    @Produces(MediaType.APPLICATION_JSON)
    public List<HaEntity> getHaEntities(
            @QueryParam("entityId") String entityId,
            @QueryParam("entityIdPrefix") String entityIdPrefix) {
        return homeAssistantService.getHaEntities(entityId, entityIdPrefix);
    }
}
