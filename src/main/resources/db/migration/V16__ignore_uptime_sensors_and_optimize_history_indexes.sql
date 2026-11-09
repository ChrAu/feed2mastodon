-- 1. Uptime-Kuma / Antwortzeit- und Verfügbarkeits-Sensoren künftig ignorieren
INSERT INTO ignored_entity (entity_id) VALUES
('sensor.%antwortzeit%'),
('sensor.%verfuegbarkeit%'),
('sensor.%verfugbarkeit%')
ON CONFLICT (entity_id) DO NOTHING;

-- 2. Redundanten Einzel-Index entfernen (wird bereits durch ha_state_history_entity_id_last_changed_index abgedeckt)
DROP INDEX IF EXISTS ha_state_history_entity_id_index;

-- 3. Index auf last_changed für schnelle Cleanup-Läufe (DELETE WHERE last_changed < :threshold)
CREATE INDEX IF NOT EXISTS ha_state_history_last_changed_idx
    ON ha_state_history (last_changed);
