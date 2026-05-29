package de.hexix.homeassistant.service;

import de.hexix.homeassistant.HomeAssistantClient;
import de.hexix.homeassistant.HomeAssistantService;
import de.hexix.homeassistant.dto.EntityDto;
import de.hexix.homeassistant.dto.EntityStateUpdateRequest;
import de.hexix.homeassistant.entity.HaStateHistory;
import de.hexix.homeassistant.service.HoltWinterForecastService.GenericForecastPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class VentilationForecastServiceTest {

    HomeAssistantClient homeAssistantClient;
    HomeAssistantService homeAssistantService;
    HoltWinterForecastService holtWinterForecastService;
    VentilationForecastService ventilationForecastService;

    @BeforeEach
    void setUp() {
        homeAssistantClient = mock(HomeAssistantClient.class);
        homeAssistantService = mock(HomeAssistantService.class);
        holtWinterForecastService = mock(HoltWinterForecastService.class);

        ventilationForecastService = new VentilationForecastService();
        ventilationForecastService.homeAssistantClient = homeAssistantClient;
        ventilationForecastService.homeAssistantService = homeAssistantService;
        ventilationForecastService.holtWinterForecastService = holtWinterForecastService;
        ventilationForecastService.apiToken = "test-token";
    }

    @Test
    void testCalculateAndPushForecast_WindowsClosed() {
        // Current indoor conditions: Temp = 21.0, Abs Hum = 10.0 (Average of Wohn/Schlaf)
        mockState("sensor.wohnzimmer_thermometer_temperatur", "21.0");
        mockState("sensor.schlafzimmer_thermometer_temperatur", "21.0");
        mockState("sensor.wohnzimmer_absolute_luftfeuchtigkeit", "10.0");
        mockState("sensor.schlafzimmer_absolute_luftfeuchtigkeit", "10.0");
        mockState("input_boolean.lueftung_fenster_offen", "off");
        mockState("sensor.balkon_thermometer_temperatur", "15.0");
        mockState("sensor.thermal_comfort_absolute_luftfeuchtigkeit", "8.0");

        // Mock outdoor history
        List<HaStateHistory> mockTempHistory = List.of(new HaStateHistory());
        List<HaStateHistory> mockHumHistory = List.of(new HaStateHistory());
        when(homeAssistantService.getHaStateHistory(eq("sensor.balkon_thermometer_temperatur"), any(), any(Duration.class)))
                .thenReturn(mockTempHistory);
        when(homeAssistantService.getHaStateHistory(eq("sensor.thermal_comfort_absolute_luftfeuchtigkeit"), any(), any(Duration.class)))
                .thenReturn(mockHumHistory);

        // Mock indoor history
        List<HaStateHistory> mockWohnTempHist = List.of(new HaStateHistory());
        List<HaStateHistory> mockSchlafTempHist = List.of(new HaStateHistory());
        List<HaStateHistory> mockWohnHumHist = List.of(new HaStateHistory());
        List<HaStateHistory> mockSchlafHumHist = List.of(new HaStateHistory());

        when(homeAssistantService.getHaStateHistory(eq("sensor.wohnzimmer_thermometer_temperatur"), any(), any(Duration.class)))
                .thenReturn(mockWohnTempHist);
        when(homeAssistantService.getHaStateHistory(eq("sensor.schlafzimmer_thermometer_temperatur"), any(), any(Duration.class)))
                .thenReturn(mockSchlafTempHist);
        when(homeAssistantService.getHaStateHistory(eq("sensor.wohnzimmer_absolute_luftfeuchtigkeit"), any(), any(Duration.class)))
                .thenReturn(mockWohnHumHist);
        when(homeAssistantService.getHaStateHistory(eq("sensor.schlafzimmer_absolute_luftfeuchtigkeit"), any(), any(Duration.class)))
                .thenReturn(mockSchlafHumHist);

        // Mock forecasting:
        // We predict 2 timesteps:
        // ts1: t_aussen = 22.0, abs_hum = 10.5 => kuehlUndTrocken = false (t_aussen > t_innenSchnitt - 0.5)
        // ts2: t_aussen = 20.0, abs_hum = 9.0 => kuehlUndTrocken = true (20.0 < 20.5 && 9.0 < 9.5)
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime ts1 = now.plusMinutes(10);
        ZonedDateTime ts2 = now.plusMinutes(20);

        List<GenericForecastPoint> tempForecast = List.of(
                new GenericForecastPoint(ts1, 22.0),
                new GenericForecastPoint(ts2, 20.0)
        );
        List<GenericForecastPoint> humForecast = List.of(
                new GenericForecastPoint(ts1, 10.5),
                new GenericForecastPoint(ts2, 9.0)
        );

        List<GenericForecastPoint> wohnTempForecast = List.of(new GenericForecastPoint(ts1, 21.0), new GenericForecastPoint(ts2, 21.0));
        List<GenericForecastPoint> schlafTempForecast = List.of(new GenericForecastPoint(ts1, 21.0), new GenericForecastPoint(ts2, 21.0));
        List<GenericForecastPoint> wohnHumForecast = List.of(new GenericForecastPoint(ts1, 10.0), new GenericForecastPoint(ts2, 10.0));
        List<GenericForecastPoint> schlafHumForecast = List.of(new GenericForecastPoint(ts1, 10.0), new GenericForecastPoint(ts2, 10.0));

        when(holtWinterForecastService.calculateGenericForecast(eq(mockTempHistory), any(Duration.class), eq(10)))
                .thenReturn(tempForecast);
        when(holtWinterForecastService.calculateGenericForecast(eq(mockHumHistory), any(Duration.class), eq(10)))
                .thenReturn(humForecast);
        when(holtWinterForecastService.calculateGenericForecast(eq(mockWohnTempHist), any(Duration.class), eq(10)))
                .thenReturn(wohnTempForecast);
        when(holtWinterForecastService.calculateGenericForecast(eq(mockSchlafTempHist), any(Duration.class), eq(10)))
                .thenReturn(schlafTempForecast);
        when(holtWinterForecastService.calculateGenericForecast(eq(mockWohnHumHist), any(Duration.class), eq(10)))
                .thenReturn(wohnHumForecast);
        when(holtWinterForecastService.calculateGenericForecast(eq(mockSchlafHumHist), any(Duration.class), eq(10)))
                .thenReturn(schlafHumForecast);

        // Action
        ventilationForecastService.calculateAndPushVentilationForecast();

        // Capture verification
        ArgumentCaptor<EntityStateUpdateRequest> requestCaptor = ArgumentCaptor.forClass(EntityStateUpdateRequest.class);
        verify(homeAssistantClient).postState(eq("Bearer test-token"), eq("sensor.lueftung_vorhersage"), requestCaptor.capture());

        EntityStateUpdateRequest request = requestCaptor.getValue();
        // Since window is closed, next_action is "öffnen" and state should be nextOpenTime (ts2)
        assertNotNull(request.state());
        ZonedDateTime stateTime = ZonedDateTime.parse(request.state(), java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        assertEquals(ts2.toEpochSecond() / 60, stateTime.toEpochSecond() / 60);

        Map<String, Object> attrs = request.attributes();
        assertEquals("öffnen", attrs.get("next_action"));
        assertEquals("closed", attrs.get("current_window_state"));
        assertEquals(21.0, attrs.get("t_innen_schnitt"));
        assertEquals(10.0, attrs.get("abs_f_innen_schnitt"));
        assertNotNull(attrs.get("next_open_time"));
    }

    @Test
    void testCalculateAndPushForecast_NoTargetTimeFallback() {
        // Current indoor conditions: Temp = 21.0, Abs Hum = 10.0 (Average of Wohn/Schlaf)
        mockState("sensor.wohnzimmer_thermometer_temperatur", "21.0");
        mockState("sensor.schlafzimmer_thermometer_temperatur", "21.0");
        mockState("sensor.wohnzimmer_absolute_luftfeuchtigkeit", "10.0");
        mockState("sensor.schlafzimmer_absolute_luftfeuchtigkeit", "10.0");
        mockState("input_boolean.lueftung_fenster_offen", "off");
        mockState("sensor.balkon_thermometer_temperatur", "30.0");
        mockState("sensor.thermal_comfort_absolute_luftfeuchtigkeit", "15.0");

        // Mock outdoor history
        List<HaStateHistory> mockTempHistory = List.of(new HaStateHistory());
        List<HaStateHistory> mockHumHistory = List.of(new HaStateHistory());
        when(homeAssistantService.getHaStateHistory(eq("sensor.balkon_thermometer_temperatur"), any(), any(Duration.class)))
                .thenReturn(mockTempHistory);
        when(homeAssistantService.getHaStateHistory(eq("sensor.thermal_comfort_absolute_luftfeuchtigkeit"), any(), any(Duration.class)))
                .thenReturn(mockHumHistory);

        // Mock indoor history
        List<HaStateHistory> mockWohnTempHist = List.of(new HaStateHistory());
        List<HaStateHistory> mockSchlafTempHist = List.of(new HaStateHistory());
        List<HaStateHistory> mockWohnHumHist = List.of(new HaStateHistory());
        List<HaStateHistory> mockSchlafHumHist = List.of(new HaStateHistory());

        when(homeAssistantService.getHaStateHistory(eq("sensor.wohnzimmer_thermometer_temperatur"), any(), any(Duration.class)))
                .thenReturn(mockWohnTempHist);
        when(homeAssistantService.getHaStateHistory(eq("sensor.schlafzimmer_thermometer_temperatur"), any(), any(Duration.class)))
                .thenReturn(mockSchlafTempHist);
        when(homeAssistantService.getHaStateHistory(eq("sensor.wohnzimmer_absolute_luftfeuchtigkeit"), any(), any(Duration.class)))
                .thenReturn(mockWohnHumHist);
        when(homeAssistantService.getHaStateHistory(eq("sensor.schlafzimmer_absolute_luftfeuchtigkeit"), any(), any(Duration.class)))
                .thenReturn(mockSchlafHumHist);

        // Mock forecasting:
        // Outdoor temperature is always 30.0 (too hot to open window, kuehlUndTrocken is never true)
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime ts1 = now.plusMinutes(10);
        ZonedDateTime ts2 = now.plusMinutes(20);

        List<GenericForecastPoint> tempForecast = List.of(
                new GenericForecastPoint(ts1, 30.0),
                new GenericForecastPoint(ts2, 30.0)
        );
        List<GenericForecastPoint> humForecast = List.of(
                new GenericForecastPoint(ts1, 15.0),
                new GenericForecastPoint(ts2, 15.0)
        );

        List<GenericForecastPoint> wohnTempForecast = List.of(new GenericForecastPoint(ts1, 21.0), new GenericForecastPoint(ts2, 21.0));
        List<GenericForecastPoint> schlafTempForecast = List.of(new GenericForecastPoint(ts1, 21.0), new GenericForecastPoint(ts2, 21.0));
        List<GenericForecastPoint> wohnHumForecast = List.of(new GenericForecastPoint(ts1, 10.0), new GenericForecastPoint(ts2, 10.0));
        List<GenericForecastPoint> schlafHumForecast = List.of(new GenericForecastPoint(ts1, 10.0), new GenericForecastPoint(ts2, 10.0));

        when(holtWinterForecastService.calculateGenericForecast(eq(mockTempHistory), any(Duration.class), eq(10)))
                .thenReturn(tempForecast);
        when(holtWinterForecastService.calculateGenericForecast(eq(mockHumHistory), any(Duration.class), eq(10)))
                .thenReturn(humForecast);
        when(holtWinterForecastService.calculateGenericForecast(eq(mockWohnTempHist), any(Duration.class), eq(10)))
                .thenReturn(wohnTempForecast);
        when(holtWinterForecastService.calculateGenericForecast(eq(mockSchlafTempHist), any(Duration.class), eq(10)))
                .thenReturn(schlafTempForecast);
        when(holtWinterForecastService.calculateGenericForecast(eq(mockWohnHumHist), any(Duration.class), eq(10)))
                .thenReturn(wohnHumForecast);
        when(holtWinterForecastService.calculateGenericForecast(eq(mockSchlafHumHist), any(Duration.class), eq(10)))
                .thenReturn(schlafHumForecast);

        // Action
        ventilationForecastService.calculateAndPushVentilationForecast();

        // Capture verification
        ArgumentCaptor<EntityStateUpdateRequest> requestCaptor = ArgumentCaptor.forClass(EntityStateUpdateRequest.class);
        verify(homeAssistantClient).postState(eq("Bearer test-token"), eq("sensor.lueftung_vorhersage"), requestCaptor.capture());

        EntityStateUpdateRequest request = requestCaptor.getValue();
        // Since no open time is found, state should fall back to the last forecast timestamp (ts2)
        assertNotNull(request.state());
        ZonedDateTime stateTime = ZonedDateTime.parse(request.state(), java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        assertEquals(ts2.toEpochSecond() / 60, stateTime.toEpochSecond() / 60);

        Map<String, Object> attrs = request.attributes();
        assertEquals("öffnen", attrs.get("next_action"));
        assertNull(attrs.get("next_open_time")); // attributes next_open_time should remain null to reflect reality
    }

    @Test
    void testCalculateAndPushForecast_WinterCold() {
        // Current indoor conditions: Temp = 21.0, Abs Hum = 10.0 (Average of Wohn/Schlaf)
        mockState("sensor.wohnzimmer_thermometer_temperatur", "21.0");
        mockState("sensor.schlafzimmer_thermometer_temperatur", "21.0");
        mockState("sensor.wohnzimmer_absolute_luftfeuchtigkeit", "10.0");
        mockState("sensor.schlafzimmer_absolute_luftfeuchtigkeit", "10.0");
        mockState("input_boolean.lueftung_fenster_offen", "on"); // window is open!
        mockState("sensor.balkon_thermometer_temperatur", "15.0");
        mockState("sensor.thermal_comfort_absolute_luftfeuchtigkeit", "8.0");

        // Mock outdoor history
        List<HaStateHistory> mockTempHistory = List.of(new HaStateHistory());
        List<HaStateHistory> mockHumHistory = List.of(new HaStateHistory());
        when(homeAssistantService.getHaStateHistory(eq("sensor.balkon_thermometer_temperatur"), any(), any(Duration.class)))
                .thenReturn(mockTempHistory);
        when(homeAssistantService.getHaStateHistory(eq("sensor.thermal_comfort_absolute_luftfeuchtigkeit"), any(), any(Duration.class)))
                .thenReturn(mockHumHistory);

        // Mock indoor history
        List<HaStateHistory> mockWohnTempHist = List.of(new HaStateHistory());
        List<HaStateHistory> mockSchlafTempHist = List.of(new HaStateHistory());
        List<HaStateHistory> mockWohnHumHist = List.of(new HaStateHistory());
        List<HaStateHistory> mockSchlafHumHist = List.of(new HaStateHistory());

        when(homeAssistantService.getHaStateHistory(eq("sensor.wohnzimmer_thermometer_temperatur"), any(), any(Duration.class)))
                .thenReturn(mockWohnTempHist);
        when(homeAssistantService.getHaStateHistory(eq("sensor.schlafzimmer_thermometer_temperatur"), any(), any(Duration.class)))
                .thenReturn(mockSchlafTempHist);
        when(homeAssistantService.getHaStateHistory(eq("sensor.wohnzimmer_absolute_luftfeuchtigkeit"), any(), any(Duration.class)))
                .thenReturn(mockWohnHumHist);
        when(homeAssistantService.getHaStateHistory(eq("sensor.schlafzimmer_absolute_luftfeuchtigkeit"), any(), any(Duration.class)))
                .thenReturn(mockSchlafHumHist);

        // Mock forecasting:
        // ts1: t_aussen = 13.0 => too cold (<= 13.5), should trigger closing the window!
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime ts1 = now.plusMinutes(10);

        List<GenericForecastPoint> tempForecast = List.of(new GenericForecastPoint(ts1, 13.0));
        List<GenericForecastPoint> humForecast = List.of(new GenericForecastPoint(ts1, 5.0));

        List<GenericForecastPoint> wohnTempForecast = List.of(new GenericForecastPoint(ts1, 21.0));
        List<GenericForecastPoint> schlafTempForecast = List.of(new GenericForecastPoint(ts1, 21.0));
        List<GenericForecastPoint> wohnHumForecast = List.of(new GenericForecastPoint(ts1, 10.0));
        List<GenericForecastPoint> schlafHumForecast = List.of(new GenericForecastPoint(ts1, 10.0));

        when(holtWinterForecastService.calculateGenericForecast(eq(mockTempHistory), any(Duration.class), eq(10)))
                .thenReturn(tempForecast);
        when(holtWinterForecastService.calculateGenericForecast(eq(mockHumHistory), any(Duration.class), eq(10)))
                .thenReturn(humForecast);
        when(holtWinterForecastService.calculateGenericForecast(eq(mockWohnTempHist), any(Duration.class), eq(10)))
                .thenReturn(wohnTempForecast);
        when(holtWinterForecastService.calculateGenericForecast(eq(mockSchlafTempHist), any(Duration.class), eq(10)))
                .thenReturn(schlafTempForecast);
        when(holtWinterForecastService.calculateGenericForecast(eq(mockWohnHumHist), any(Duration.class), eq(10)))
                .thenReturn(wohnHumForecast);
        when(holtWinterForecastService.calculateGenericForecast(eq(mockSchlafHumHist), any(Duration.class), eq(10)))
                .thenReturn(schlafHumForecast);

        // Action
        ventilationForecastService.calculateAndPushVentilationForecast();

        // Capture verification
        ArgumentCaptor<EntityStateUpdateRequest> requestCaptor = ArgumentCaptor.forClass(EntityStateUpdateRequest.class);
        verify(homeAssistantClient).postState(eq("Bearer test-token"), eq("sensor.lueftung_vorhersage"), requestCaptor.capture());

        EntityStateUpdateRequest request = requestCaptor.getValue();
        // Since window is open and it gets too cold (13.0 <= 13.5), state should be nextCloseTime (ts1)
        assertNotNull(request.state());
        ZonedDateTime stateTime = ZonedDateTime.parse(request.state(), java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        assertEquals(ts1.toEpochSecond() / 60, stateTime.toEpochSecond() / 60);

        Map<String, Object> attrs = request.attributes();
        assertEquals("schließen", attrs.get("next_action"));
        assertEquals("open", attrs.get("current_window_state"));
        assertNotNull(attrs.get("next_close_time"));
    }

    private void mockState(String entityId, String state) {
        EntityDto dto = new EntityDto();
        dto.setEntityId(entityId);
        dto.setState(state);
        when(homeAssistantClient.getState(eq("Bearer test-token"), eq(entityId))).thenReturn(dto);
    }
}
