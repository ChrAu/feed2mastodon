-- Tabelle für die Metadaten der Vorhersage
CREATE TABLE ha_fuel_forecast (
                                  id BIGSERIAL PRIMARY KEY,
                                  entity_id VARCHAR(255) NOT NULL,
                                  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                  forecast_duration_minutes INT NOT NULL,
                                  raster_minutes INT NOT NULL
);

CREATE INDEX idx_ha_fuel_forecast_entity_created ON ha_fuel_forecast(entity_id, created_at);

-- Tabelle für die einzelnen Vorhersagepunkte
CREATE TABLE ha_fuel_forecast_data (
                                       id BIGSERIAL PRIMARY KEY,
                                       forecast_id BIGINT NOT NULL,
                                       target_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
                                       predicted_price DOUBLE PRECISION NOT NULL,
                                       CONSTRAINT fk_forecast_data_forecast FOREIGN KEY (forecast_id) REFERENCES ha_fuel_forecast(id) ON DELETE CASCADE
);

CREATE INDEX idx_ha_fuel_forecast_data_target ON ha_fuel_forecast_data(target_timestamp);
