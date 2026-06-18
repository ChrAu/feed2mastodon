-- 1. Unnötige, historische Einträge aus ha_state_history löschen
-- Zuerst eventuelle Referenzen in ha_temperature_history löschen (falls vorhanden)
DELETE FROM ha_temperature_history
WHERE ha_state_history_id IN (
    SELECT id FROM ha_state_history
    WHERE entity_id LIKE 'geo_location.lightning_strike_%'
       OR entity_id LIKE 'automation.%'
       OR entity_id LIKE 'button.%'
       OR entity_id LIKE 'device_tracker.%'
       OR entity_id LIKE 'event.%'
       OR entity_id LIKE 'camera.%'
       OR entity_id LIKE 'calendar.%'
       OR entity_id LIKE 'conversation.%'
);

-- Nun die eigentlichen Einträge aus ha_state_history löschen
DELETE FROM ha_state_history
WHERE entity_id LIKE 'geo_location.lightning_strike_%'
   OR entity_id LIKE 'automation.%'
   OR entity_id LIKE 'button.%'
   OR entity_id LIKE 'device_tracker.%'
   OR entity_id LIKE 'event.%'
   OR entity_id LIKE 'camera.%'
   OR entity_id LIKE 'calendar.%'
   OR entity_id LIKE 'conversation.%';

-- 2. Bestehende, riesige Forecast-Attribute aus Weather-Entitäten in ha_state_history löschen, um massiv Speicher freizugeben
UPDATE ha_state_history
SET attributes = (attributes::jsonb #- '{additionalAttributes,forecast}')::text
WHERE entity_id LIKE 'weather.%'
  AND attributes IS NOT NULL
  AND attributes::jsonb -> 'additionalAttributes' ? 'forecast';

-- 3. Wildcards/Muster in die ignored_entity Tabelle einfügen, damit zukünftige Daten gar nicht erst geladen werden
INSERT INTO ignored_entity (entity_id) VALUES
('geo_location.%'),
('automation.%'),
('button.%'),
('device_tracker.%'),
('event.%'),
('camera.%'),
('calendar.%'),
('conversation.%')
ON CONFLICT (entity_id) DO NOTHING;
