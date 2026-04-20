package de.hexix.homeassistant;

import de.hexix.homeassistant.dto.AttributesDto;
import de.hexix.homeassistant.dto.CarDataDto;
import de.hexix.homeassistant.dto.CarHistoryItemDto; // New import
import de.hexix.homeassistant.dto.CpuDto;
import de.hexix.homeassistant.dto.ElectricityPriceDto;
import de.hexix.homeassistant.dto.ElectricityPriceHistoryDto;
import de.hexix.homeassistant.dto.ElectricityPriceOverviewDto;
import de.hexix.homeassistant.dto.EntityDto;
import de.hexix.homeassistant.dto.FuelPriceDto;
import de.hexix.homeassistant.dto.FuelPriceForecastDto;
import de.hexix.homeassistant.dto.FuelPriceHistoryDto;
import de.hexix.homeassistant.dto.FuelStationDto;
import de.hexix.homeassistant.dto.SavedForecastDto;
import de.hexix.homeassistant.dto.TemperatureBucketDTO;
import de.hexix.homeassistant.entity.HaEntity;
import de.hexix.homeassistant.entity.HaFuelForecast;
import de.hexix.homeassistant.entity.HaStateHistory;
import de.hexix.homeassistant.entity.HaTemperatureHistory;
import de.hexix.homeassistant.ignoredentity.IgnoredEntityRepository;
import de.hexix.homeassistant.service.HoltWinterForecastService;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class HomeAssistantService {

    private static final Logger LOG = Logger.getLogger(HomeAssistantService.class);

    @ConfigProperty(name = "home-assistant.api.token")
    String apiToken;

    @Inject
    @RestClient
    HomeAssistantClient homeAssistantClient;

    @Inject
    AttributeMapperHelper attributeMapperHelper;

    @Inject
    EntityManager em;

    @Inject
    HoltWinterForecastService holtWinterForecastService;

    @Inject
    IgnoredEntityRepository ignoredEntityRepository;

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

    public static final List<String> FUEL_PRICE_IDS = List.of(
            "sensor.aral_gosbach_diesel",
            "sensor.aral_gosbach_super",
            "sensor.aral_gosbach_super_e10",
            "binary_sensor.aral_gosbach_status",
            "sensor.totalenergies_deggingen_diesel",
            "binary_sensor.totalenergies_deggingen_status",
            "sensor.totalenergies_deggingen_super",
            "sensor.totalenergies_deggingen_super_e10"
    );

    public static final List<String> ELECTRICITY_PRICE_ENTITY_IDS = List.of(
            "sensor.strompreis_average_price",
            "sensor.strompreis_highest_price",
            "sensor.strompreis_lowest_price",
            "sensor.strompreis_market_price",
            "sensor.strompreis_median_price",
            "sensor.strompreis_quantile",
            "sensor.strompreis_rank",
            "sensor.strompreis_total_price"
    );
    public static final String ELECTRICITY_TOTAL_PRICE_ENTITY_ID = "sensor.strompreis_total_price";

    public static final List<String> CAR_DATA_ENTITY_IDS = List.of(
            "sensor.id4_mercatis_gmbh_odometer",
            "sensor.id4_mercatis_gmbh_electric_range",
            "sensor.id4_mercatis_gmbh_battery_level",
            "sensor.temperatur_am_standort_christopher",
            "sensor.id4_mercatis_gmbh_hv_battery_min_temperature"
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
    public List<HaStateHistory> getCpuData(final Duration duration) {
        final TypedQuery<HaStateHistory> query = em.createNamedQuery(HaStateHistory.FIND_ALL_CPU_DATA, HaStateHistory.class);
        final long seconds = duration.toSeconds();
        final ZonedDateTime zonedDateTime = ZonedDateTime.now().minusSeconds(seconds);

        query.setParameter("entityId", "sensor.codeheap_cpu_auslastung");
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

    public List<FuelStationDto> getFuelPrices() {
        Map<String, Map<String, FuelPriceDto>> stationPrices = new HashMap<>();
        Map<String, Boolean> stationStatus = new HashMap<>();
        Map<String, String> stationNames = new HashMap<>(); // To store friendly names

        for (String entityId : FUEL_PRICE_IDS) {
            try {
                EntityDto entity = homeAssistantClient.getState("Bearer " + apiToken, entityId);
                // FIX: entity.getAttributes() already returns AttributesDto
                AttributesDto attributes = entity.getAttributes();
                String friendlyName = attributes.getFriendlyName();
                ZonedDateTime lastChanged = ZonedDateTime.parse(entity.getLastChanged(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);

                // Extract station name from friendly name (e.g., "Aral Gosbach Diesel" -> "Aral Gosbach")
                Pattern pattern = Pattern.compile("^(.*?)(?: Diesel| Super| Super E10| Status)$");
                Matcher matcher = pattern.matcher(friendlyName);
                String stationKey;
                if (matcher.find()) {
                    stationKey = matcher.group(1).trim();
                } else {
                    stationKey = friendlyName.replace(" Status", "").trim(); // Fallback for status sensors
                }
                stationNames.put(stationKey, stationKey); // Store the base name

                if (entityId.startsWith("sensor.")) {
                    double price = Double.parseDouble(entity.getState());
                    String unit = (String) attributes.getAdditionalAttributes().get("unit_of_measurement");

                    String fuelType = "";
                    if (entityId.contains("diesel")) {
                        fuelType = "diesel";
                    } else if (entityId.contains("super_e10")) {
                        fuelType = "superE10";
                    } else if (entityId.contains("super")) {
                        fuelType = "super";
                    }

                    // Fetch the previous price from database to calculate difference
                    Double previousPrice = null;
                    try {
                        TypedQuery<HaStateHistory> beforeQuery = em.createNamedQuery(HaStateHistory.FIND_PREVIOUS_BY_ENTITY_ID_AND_LAST_CHANGED, HaStateHistory.class);
                        beforeQuery.setParameter("entityId", entityId);
                        beforeQuery.setParameter("lastChanged", lastChanged);
                        beforeQuery.setMaxResults(1);
                        HaStateHistory beforeHistory = beforeQuery.getSingleResultOrNull();

                        if (beforeHistory != null) {
                            previousPrice = Double.parseDouble(beforeHistory.getState());
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to fetch previous price for " + entityId + ": " + e.getMessage());
                    }

                    stationPrices.computeIfAbsent(stationKey, k -> new HashMap<>())
                            .put(fuelType, new FuelPriceDto(price, unit, lastChanged, entityId, previousPrice)); // Pass entityId and previousPrice here

                } else if (entityId.startsWith("binary_sensor.")) {
                    boolean status = "on".equalsIgnoreCase(entity.getState());
                    stationStatus.put(stationKey, status);
                }

            } catch (Exception e) {
                System.err.println("Fehler beim Abruf von Tankstellendaten für Entity: " + entityId + " - " + e.getMessage());
                // Continue processing other entities even if one fails
            }
        }

        List<FuelStationDto> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : stationNames.entrySet()) {
            String stationKey = entry.getKey();
            String stationDisplayName = entry.getValue(); // Use the extracted base name

            Map<String, FuelPriceDto> prices = stationPrices.getOrDefault(stationKey, new HashMap<>());
            boolean status = stationStatus.getOrDefault(stationKey, false); // Default to offline if status not found

            result.add(new FuelStationDto(stationDisplayName, prices, status));
        }

        return result;
    }

    @Transactional
    public List<FuelPriceHistoryDto> getFuelPriceHistory(String entityId, Duration duration) {
        final ZonedDateTime now = ZonedDateTime.now();
        final ZonedDateTime startDate = now.minus(duration);

        // 1. Fetch historical data within the specified duration
        final TypedQuery<HaStateHistory> historyQuery = em.createNamedQuery(HaStateHistory.FIND_BY_ENTITY_ID_AND_DATE_RANGE, HaStateHistory.class);
        historyQuery.setParameter("entityId", entityId);
        historyQuery.setParameter("startDate", startDate);
        List<HaStateHistory> rawHistory = historyQuery.getResultList();

        // 2. Get the current state/price
        Double currentPrice = null;
        try {
            EntityDto currentEntity = homeAssistantClient.getState("Bearer " + apiToken, entityId);
            currentPrice = Double.parseDouble(currentEntity.getState());
        } catch (Exception e) {
            System.err.println("Failed to get current state for entityId: " + entityId + " - " + e.getMessage());
            // If current price cannot be fetched, we might not be able to draw the end point
        }

        // 3. Get the last known price BEFORE the startDate
        Double priceBeforeStartDate = null;
        TypedQuery<HaStateHistory> beforeQuery = em.createNamedQuery(HaStateHistory.FIND_PREVIOUS_BY_ENTITY_ID_AND_LAST_CHANGED, HaStateHistory.class);
        beforeQuery.setParameter("entityId", entityId);
        beforeQuery.setParameter("lastChanged", startDate);
        beforeQuery.setMaxResults(1);
        HaStateHistory beforeHistory = beforeQuery.getSingleResultOrNull();

        if (beforeHistory != null) {
            try {
                priceBeforeStartDate = Double.parseDouble(beforeHistory.getState());
            } catch (NumberFormatException e) {
                System.err.println("State before startDate is not a number for entityId: " + entityId + " - " + e.getMessage());
            }
        }

        List<FuelPriceHistoryDto> finalHistory = new ArrayList<>();

        // Add the synthetic point at the beginning (startDate)
        if (priceBeforeStartDate != null) {
            finalHistory.add(new FuelPriceHistoryDto(startDate, priceBeforeStartDate));
        } else if (!rawHistory.isEmpty()) {
            // If no history before startDate, but there is history within the range,
            // use the first historical point's value at startDate.
            // This prevents a gap if the first actual data point is much later than startDate.
            try {
                finalHistory.add(new FuelPriceHistoryDto(startDate, Double.parseDouble(rawHistory.get(0).getState())));
            } catch (NumberFormatException e) {
                // ignore
            }
        }


        // Add actual historical data, filtering out non-numeric states
        rawHistory.stream()
                .filter(haStateHistory -> {
                    try {
                        Double.parseDouble(haStateHistory.getState());
                        return true;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                })
                .map(haStateHistory -> new FuelPriceHistoryDto(
                        haStateHistory.getLastChanged(),
                        Double.parseDouble(haStateHistory.getState())
                ))
                .forEach(finalHistory::add);

        // Add the synthetic point at the end (now)
        // Only add if we have a current price and there's at least one point in the history
        // (either from beforeStartDate, or actual history) to connect to.
        if (currentPrice != null && (!finalHistory.isEmpty() || priceBeforeStartDate != null)) {
            finalHistory.add(new FuelPriceHistoryDto(now, currentPrice));
        } else if (currentPrice != null && finalHistory.isEmpty()) {
            // If no history at all, but we have a current price, add just this one point
            finalHistory.add(new FuelPriceHistoryDto(now, currentPrice));
        }


        return finalHistory;
    }

    public List<FuelPriceForecastDto> getFuelPriceForecast(String entityId, Duration historyDuration, Duration forecastDuration, int rasterMinutes) {
        return getFuelPriceForecast(entityId, historyDuration, forecastDuration, rasterMinutes, false);
    }

    public List<FuelPriceForecastDto> getFuelPriceForecast(String entityId, Duration historyDuration, Duration forecastDuration, int rasterMinutes, boolean persist) {
        if (!FUEL_PRICE_IDS.contains(entityId)) {
             throw new IllegalArgumentException("Entity ID " + entityId + " is not supported for fuel price forecasting.");
        }

        List<HaStateHistory> historyData = getFuelPriceHistoryData(entityId, historyDuration);

        return holtWinterForecastService.calculateForecast(historyData, forecastDuration, rasterMinutes, persist);
    }

    @Transactional
    List<HaStateHistory> getFuelPriceHistoryData(String entityId, Duration historyDuration) {
        final ZonedDateTime now = ZonedDateTime.now();
        final ZonedDateTime startDate = now.minus(historyDuration);

        final TypedQuery<HaStateHistory> historyQuery = em.createNamedQuery(HaStateHistory.FIND_BY_ENTITY_ID_AND_DATE_RANGE, HaStateHistory.class);
        historyQuery.setParameter("entityId", entityId);
        historyQuery.setParameter("startDate", startDate);
        return historyQuery.getResultList();
    }


    private List<EntityDto> lastPiHoleData = null;
    private final BroadcastProcessor<List<EntityDto>> piHoleProcessor = BroadcastProcessor.create();

    private List<CpuDto> lastCpuData = null;
    private final BroadcastProcessor<List<CpuDto>> cpuProcessor = BroadcastProcessor.create();

    void onStart(@Observes StartupEvent ev) {
        // Use the event parameter to avoid it being considered unused by static analysis
        Objects.requireNonNull(ev);

        Multi.createFrom().ticks().every(Duration.ofSeconds(30))
                .onItem().transformToUniAndConcatenate(tick ->
                        Uni.createFrom().item(this::fetchSpecificPiHoleMetrics)
                                .onFailure().retry().withBackOff(Duration.ofSeconds(1), Duration.ofSeconds(60)).indefinitely() // Add retry here
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

        Multi.createFrom().ticks().every(Duration.ofSeconds(60))
                .onItem().transformToUniAndConcatenate(tick ->
                        Uni.createFrom().item(this::fetchCpuData)
                                .onFailure().retry().withBackOff(Duration.ofSeconds(1), Duration.ofSeconds(60)).indefinitely() // Add retry here
                                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                )
                .subscribe().with(
                        data -> {
                            this.lastCpuData = data;
                            cpuProcessor.onNext(data);
                        },
                        err -> System.err.println("Fehler beim CPU Live-Update: " + err.getMessage())
                );
    }

    private List<CpuDto> fetchCpuData() {
        final List<HaStateHistory> allCpuData = getCpuData(Duration.ofDays(1));

        return allCpuData.stream()
                .map(haStateHistory -> {
                    final AttributesDto attributesDto = attributeMapperHelper.stringToAttributes(haStateHistory.getAttributes());
                    final String friendlyName = attributesDto.getFriendlyName();
                    final String stateText = haStateHistory.getState();
                    if(!Objects.equals(haStateHistory.getState(), "unavailable")){
                        double state = Double.parseDouble(stateText);
                        BigDecimal bd = BigDecimal.valueOf(state).setScale(1, RoundingMode.HALF_UP);

                        return new CpuDto(haStateHistory.getEntityId(), friendlyName, bd.toString(), haStateHistory.getLastChanged());
                    }

                    return new CpuDto(haStateHistory.getEntityId(), friendlyName, "0.0", haStateHistory.getLastChanged());
                }).toList();
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

    public Multi<List<CpuDto>> getCpuStream() {
        if (lastCpuData != null) {
            return Multi.createBy().concatenating()
                    .streams(
                            Multi.createFrom().item(lastCpuData),
                            cpuProcessor
                    );
        }
        return cpuProcessor;
    }

    @Transactional
    public List<SavedForecastDto> getSavedForecasts(String entityId) {
        List<HaFuelForecast> entities = em.createNamedQuery(HaFuelForecast.FIND_BY_ENTITY_ID_ORDER_BY_CREATED_DESC, HaFuelForecast.class)
                .setParameter("entityId", entityId)
                .getResultList();

        return entities.stream()
                .filter(haFuelForecast -> haFuelForecast.getForecastDurationMinutes() == 2880)
                .map(f -> {
            List<FuelPriceForecastDto> points = f.getDataPoints().stream()
                    .map(dp -> new FuelPriceForecastDto(dp.getTargetTimestamp(), dp.getPredictedPrice()))
                    .toList();

            return new SavedForecastDto(
                    f.getId(),
                    f.getCreatedAt(),
                    f.getForecastDurationMinutes(),
                    f.getRasterMinutes(),
                    points
            );
        }).toList();
    }

    @Transactional
    public ElectricityPriceOverviewDto getElectricityPriceOverview() {
        Map<String, ElectricityPriceDto> prices = new HashMap<>();
        String mainEntityId = ELECTRICITY_TOTAL_PRICE_ENTITY_ID;

        String overviewFriendlyName = null;
        String overviewUnit = null;
        ZonedDateTime overviewLastChanged = null;
        String overviewCurrency = null;
        String overviewProvider = null;
        String overviewRegion = null;

        for (String entityId : ELECTRICITY_PRICE_ENTITY_IDS) {
            try {
                EntityDto entity = homeAssistantClient.getState("Bearer " + apiToken, entityId);
                AttributesDto attributes = entity.getAttributes();
                ZonedDateTime lastChanged = ZonedDateTime.parse(entity.getLastChanged(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);

                Double currentValue = Double.parseDouble(entity.getState());
                String unit = (String) attributes.getAdditionalAttributes().get("unit_of_measurement");
                String currency = (String) attributes.getAdditionalAttributes().get("currency");
                String provider = (String) attributes.getAdditionalAttributes().get("provider");
                String region = (String) attributes.getAdditionalAttributes().get("region");

                // Fetch previous value from DB
                Double previousValue = null;
                try {
                    TypedQuery<HaStateHistory> beforeQuery = em.createNamedQuery(HaStateHistory.FIND_PREVIOUS_BY_ENTITY_ID_AND_LAST_CHANGED, HaStateHistory.class);
                    beforeQuery.setParameter("entityId", entityId);
                    beforeQuery.setParameter("lastChanged", lastChanged);
                    beforeQuery.setMaxResults(1);
                    HaStateHistory beforeHistory = beforeQuery.getSingleResultOrNull();

                    if (beforeHistory != null) {
                        previousValue = Double.parseDouble(beforeHistory.getState());
                    }
                } catch (Exception e) {
                    System.err.println("Failed to fetch previous electricity price for " + entityId + ": " + e.getMessage());
                }

                ElectricityPriceDto priceDto = new ElectricityPriceDto(
                        entity.getEntityId(),
                        attributes.getFriendlyName(),
                        currentValue,
                        unit,
                        lastChanged.toLocalDateTime(),
                        previousValue,
                        currency,
                        provider,
                        region
                );
                // Use a shorter key for the map (e.g., "total_price" instead of "sensor.strompreis_total_price")
                prices.put(entityId.replace("sensor.strompreis_", ""), priceDto);

                // Capture overview details from the main entity
                if (entityId.equals(mainEntityId)) {
                    overviewFriendlyName = attributes.getFriendlyName();
                    overviewUnit = unit;
                    overviewLastChanged = lastChanged;
                    overviewCurrency = currency;
                    overviewProvider = provider;
                    overviewRegion = region;
                }

            } catch (Exception e) {
                LOG.error("Fehler beim Abruf des Strompreises für Entity: " + entityId, e);
                // Continue processing other entities even if one fails
            }
        }

        if (prices.isEmpty()) {
            throw new RuntimeException("Could not fetch any electricity prices.");
        }

        // Ensure main entity details are set, even if it failed to fetch (unlikely if prices is not empty)
        if (overviewFriendlyName == null) {
            ElectricityPriceDto total_price_dto = prices.get("total_price");
            if (total_price_dto != null) {
                overviewFriendlyName = total_price_dto.friendlyName();
                overviewUnit = total_price_dto.unit();
                overviewLastChanged = ZonedDateTime.of(total_price_dto.lastChanged(), ZoneId.systemDefault()); // Convert back to ZonedDateTime
                overviewCurrency = total_price_dto.currency();
                overviewProvider = total_price_dto.provider();
                overviewRegion = total_price_dto.region();
            } else {
                // Fallback if total_price is somehow missing or failed
                // This case should ideally not happen if prices is not empty
                LOG.warn("Main electricity price entity (total_price) not found in fetched prices, using first available.");
                Map.Entry<String, ElectricityPriceDto> firstEntry = prices.entrySet().iterator().next();
                overviewFriendlyName = firstEntry.getValue().friendlyName();
                overviewUnit = firstEntry.getValue().unit();
                overviewLastChanged = ZonedDateTime.of(firstEntry.getValue().lastChanged(), ZoneId.systemDefault());
                overviewCurrency = firstEntry.getValue().currency();
                overviewProvider = firstEntry.getValue().provider();
                overviewRegion = firstEntry.getValue().region();
            }
        }

        return new ElectricityPriceOverviewDto(
                mainEntityId,
                overviewFriendlyName,
                overviewUnit,
                overviewLastChanged.toLocalDateTime(),
                overviewCurrency,
                overviewProvider,
                overviewRegion,
                prices
        );
    }

    @Transactional
    public List<ElectricityPriceHistoryDto> getElectricityPriceHistory(String entityId, Duration duration) {
        final ZonedDateTime now = ZonedDateTime.now();
        final ZonedDateTime startDate = now.minus(duration);

        final TypedQuery<HaStateHistory> historyQuery = em.createNamedQuery(HaStateHistory.FIND_BY_ENTITY_ID_AND_DATE_RANGE, HaStateHistory.class);
        historyQuery.setParameter("entityId", entityId);
        historyQuery.setParameter("startDate", startDate);
        List<HaStateHistory> rawHistory = historyQuery.getResultList();

        Double currentPrice = null;
        try {
            EntityDto currentEntity = homeAssistantClient.getState("Bearer " + apiToken, entityId);
            currentPrice = Double.parseDouble(currentEntity.getState());
        } catch (Exception e) {
            System.err.println("Failed to get current state for entityId: " + entityId + " - " + e.getMessage());
        }

        Double priceBeforeStartDate = null;
        TypedQuery<HaStateHistory> beforeQuery = em.createNamedQuery(HaStateHistory.FIND_PREVIOUS_BY_ENTITY_ID_AND_LAST_CHANGED, HaStateHistory.class);
        beforeQuery.setParameter("entityId", entityId);
        beforeQuery.setParameter("lastChanged", startDate);
        beforeQuery.setMaxResults(1);
        HaStateHistory beforeHistory = beforeQuery.getSingleResultOrNull();

        if (beforeHistory != null) {
            try {
                priceBeforeStartDate = Double.parseDouble(beforeHistory.getState());
            } catch (NumberFormatException e) {
                System.err.println("State before startDate is not a number for entityId: " + entityId + " - " + e.getMessage());
            }
        }

        List<ElectricityPriceHistoryDto> finalHistory = new ArrayList<>();

        if (priceBeforeStartDate != null) {
            finalHistory.add(new ElectricityPriceHistoryDto(startDate, priceBeforeStartDate));
        } else if (!rawHistory.isEmpty()) {
            try {
                finalHistory.add(new ElectricityPriceHistoryDto(startDate, Double.parseDouble(rawHistory.get(0).getState())));
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        rawHistory.stream()
                .filter(haStateHistory -> {
                    try {
                        Double.parseDouble(haStateHistory.getState());
                        return true;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                })
                .map(haStateHistory -> new ElectricityPriceHistoryDto(
                        haStateHistory.getLastChanged(),
                        Double.parseDouble(haStateHistory.getState())
                ))
                .forEach(finalHistory::add);

        if (currentPrice != null && (!finalHistory.isEmpty() || priceBeforeStartDate != null)) {
            finalHistory.add(new ElectricityPriceHistoryDto(now, currentPrice));
        } else if (currentPrice != null && finalHistory.isEmpty()) {
            finalHistory.add(new ElectricityPriceHistoryDto(now, currentPrice));
        }

        return finalHistory;
    }

    @Transactional
    public CarDataDto getCarData() {
        Double odometer = null;
        Double electricRange = null;
        Double batteryLevel = null;
        Double externalTemperature = null;
        Double batteryTemperature = null;
        LocalDateTime lastUpdate = null;

        for (String entityId : CAR_DATA_ENTITY_IDS) {
            try {
                EntityDto entity = homeAssistantClient.getState("Bearer " + apiToken, entityId);
                double value = Double.parseDouble(entity.getState());
                LocalDateTime currentLastUpdate = ZonedDateTime.parse(entity.getLastChanged(), DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime();

                if ("sensor.id4_mercatis_gmbh_odometer".equals(entityId)) {
                    odometer = value;
                } else if ("sensor.id4_mercatis_gmbh_electric_range".equals(entityId)) {
                    electricRange = value;
                } else if ("sensor.id4_mercatis_gmbh_battery_level".equals(entityId)) {
                    batteryLevel = value;
                } else if ("sensor.temperatur_am_standort_christopher".equals(entityId)) {
                    externalTemperature = value;
                } else if("sensor.id4_mercatis_gmbh_hv_battery_min_temperature".equals(entityId)){
                    batteryTemperature = value;
                }

                if (lastUpdate == null || currentLastUpdate.isAfter(lastUpdate)) {
                    lastUpdate = currentLastUpdate;
                }

            } catch (Exception e) {
                LOG.error("Fehler beim Abruf der Fahrzeugdaten für Entity: " + entityId, e);
            }
        }
        return new CarDataDto(odometer, electricRange, batteryLevel, externalTemperature, batteryTemperature, lastUpdate);
    }

    @Transactional
    public List<CarHistoryItemDto> getCarDataHistory(String entityId, Duration duration, Duration aggregationInterval) {
        final ZonedDateTime now = ZonedDateTime.now();
        final ZonedDateTime startDate = now.minus(duration);

        final TypedQuery<HaStateHistory> historyQuery = em.createNamedQuery(HaStateHistory.FIND_BY_ENTITY_ID_AND_DATE_RANGE, HaStateHistory.class);
        historyQuery.setParameter("entityId", entityId);
        historyQuery.setParameter("startDate", startDate);
        List<HaStateHistory> rawHistory = historyQuery.getResultList();

        Double currentValue = null;
        try {
            EntityDto currentEntity = homeAssistantClient.getState("Bearer " + apiToken, entityId);
            currentValue = Double.parseDouble(currentEntity.getState());
        } catch (Exception e) {
            System.err.println("Failed to get current state for entityId: " + entityId + " - " + e.getMessage());
        }

        Double valueBeforeStartDate = null;
        TypedQuery<HaStateHistory> beforeQuery = em.createNamedQuery(HaStateHistory.FIND_PREVIOUS_BY_ENTITY_ID_AND_LAST_CHANGED, HaStateHistory.class);
        beforeQuery.setParameter("entityId", entityId);
        beforeQuery.setParameter("lastChanged", startDate);
        beforeQuery.setMaxResults(1);
        HaStateHistory beforeHistory = beforeQuery.getSingleResultOrNull();

        if (beforeHistory != null) {
            try {
                valueBeforeStartDate = Double.parseDouble(beforeHistory.getState());
            } catch (NumberFormatException e) {
                System.err.println("State before startDate is not a number for entityId: " + entityId + " - " + e.getMessage());
            }
        }

        List<CarHistoryItemDto> aggregatedHistory = new ArrayList<>();
        long aggregationSeconds = aggregationInterval.toSeconds();

        // Initialize last known value for filling gaps
        Double lastKnownValue = valueBeforeStartDate;

        // Iterate through time buckets
        ZonedDateTime currentBucketStart = startDate.withZoneSameInstant(ZoneId.systemDefault()); // Ensure consistent time zone
        while (currentBucketStart.isBefore(now) || currentBucketStart.isEqual(now)) {
            ZonedDateTime currentBucketEnd = currentBucketStart.plus(aggregationInterval);

            final ZonedDateTime finalCurrentBucketStart = currentBucketStart;
            List<Double> valuesInBucket = rawHistory.stream()
                    .filter(h -> {
                        ZonedDateTime historyTime = h.getLastChanged().withZoneSameInstant(ZoneId.systemDefault());
                        return (historyTime.isAfter(finalCurrentBucketStart) || historyTime.isEqual(finalCurrentBucketStart))
                                && historyTime.isBefore(currentBucketEnd);
                    })
                    .map(h -> {
                        try {
                            return Double.parseDouble(h.getState());
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();

            if (!valuesInBucket.isEmpty()) {
                double averageValue = valuesInBucket.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                aggregatedHistory.add(new CarHistoryItemDto(currentBucketStart, averageValue));
                lastKnownValue = averageValue;
            } else if (lastKnownValue != null) {
                // If no data in this bucket, use the last known value to ensure continuity
                aggregatedHistory.add(new CarHistoryItemDto(currentBucketStart, lastKnownValue));
            }
            // If lastKnownValue is null and no data in bucket, we skip adding a point for this bucket.
            // This handles cases where there's no data at all or at the very beginning.

            currentBucketStart = currentBucketEnd;
        }

        // Add the current value as the very last point if it's not already covered by the last bucket
        if (currentValue != null) {
            if (aggregatedHistory.isEmpty() || !aggregatedHistory.get(aggregatedHistory.size() - 1).timestamp().isEqual(now.withZoneSameInstant(ZoneId.systemDefault()))) {
                aggregatedHistory.add(new CarHistoryItemDto(now, currentValue));
            }
        }


        return aggregatedHistory;
    }


    @Transactional
    public int cleanupOldForecasts(long hoursOld) {
        ZonedDateTime threshold = ZonedDateTime.now().minusHours(hoursOld);


        return em.createNamedQuery(HaFuelForecast.DELETE_OLD_FORECASTS)
                .setParameter("threshold", threshold)
                .executeUpdate();
    }

    @Transactional
    public List<HaStateHistory> getHaStateHistory(String entityId, String entityIdPrefix, Duration duration) {
        ZonedDateTime startDate = ZonedDateTime.now().minus(duration);
        TypedQuery<HaStateHistory> query = em.createNamedQuery(HaStateHistory.FIND_ALL_FILTERED, HaStateHistory.class);
        query.setParameter("entityId", entityId);
        query.setParameter("entityIdPrefix", entityIdPrefix != null ? entityIdPrefix + "%" : null);
        query.setParameter("startDate", startDate);
        return query.getResultList();
    }

    @Transactional
    public List<HaEntity> getHaEntities(String entityId, String entityIdPrefix) {
        // Since we don't have a duration filter for entities in the same way, we can just use a default or very old date to show all, or define a new query
        // Actually, for entities, it's just the current state, so let's adjust the query
        TypedQuery<HaEntity> query = em.createNamedQuery(HaEntity.FIND_ALL_FILTERED, HaEntity.class);
        query.setParameter("entityId", entityId);
        query.setParameter("entityIdPrefix", entityIdPrefix != null ? entityIdPrefix + "%" : null);
        // HaEntity usually represents the latest state. We can just use a very old date if we want all, or create a specific query without date filter.
        // Let's modify HaEntity to have a query without date filter, or just use a very old date for now to keep it simple.
        query.setParameter("startDate", ZonedDateTime.now().minusYears(1)); // Essentially all time
        return query.getResultList();
    }
}
