-- 1. Optimierung für LIKE / ILIKE Abfragen (Prefix-Suchen wie 'climate.%' oder 'weather.%')
-- Da in JPA/Hibernate oft `ilike` (case-insensitive) genutzt wird, ist ein Index auf `lower(entity_id)`
-- mit der Operator-Klasse `varchar_pattern_ops` extrem hilfreich, da ein normaler B-Tree-Index
-- LIKE-Suchen mit Wildcards am Ende oft nicht optimal unterstützt.
CREATE INDEX IF NOT EXISTS ha_state_history_entity_id_lower_pattern_idx
    ON ha_state_history (lower(entity_id) varchar_pattern_ops);

-- 2. Bestehender Index (bereits sehr gut, wird hier der Vollständigkeit halber aufgeführt)
-- Dieser deckt fast alle Abfragen ab:
-- "WHERE entity_id = ? AND last_changed > ?"
-- "WHERE entity_id = ? ORDER BY last_changed DESC"
-- CREATE INDEX ha_state_history_entity_id_last_changed_index ON ha_state_history (entity_id, last_changed);

-- 3. Ein dedizierter absteigender Index für History-Queries.
-- PostgreSQL kann reguläre (aufsteigende) B-Tree Indizes fast genauso schnell rückwärts lesen.
-- Bei extrem großen Tabellen und sehr vielen "ORDER BY last_changed DESC LIMIT 1" Abfragen
-- (wie bei unserer vorherigen Preissuche) kann ein expliziter DESC-Index dennoch einen winzigen
-- Performance-Vorteil bringen.
CREATE INDEX IF NOT EXISTS ha_state_history_entity_id_last_changed_desc_idx
    ON ha_state_history (entity_id, last_changed DESC);

-- 4. Optimierung für den Native Query JSONB Cast (Optional, falls die Abfrage zu langsam ist)
-- Der Native Query castet `attributes` im laufenden Betrieb zu JSONB und sucht nach `current_temperature`.
-- Wenn die Temperatur-Historie (über 90 Tage) extrem groß wird, kann ein Expression-Index helfen:
CREATE INDEX IF NOT EXISTS ha_state_history_attributes_temp_idx
    ON ha_state_history (( (attributes::jsonb -> 'additionalAttributes' ->> 'current_temperature')::float ))
    WHERE attributes IS NOT NULL AND entity_id LIKE 'climate.%';

-- 5. Für die Tabelle ha_entities (Aktueller Zustand)
-- Hier ist der Primärschlüssel (entity_id) meist ausreichend.
-- Falls im System häufig nach "Zuletzt aktualisiert" (last_updated) sortiert oder gesucht wird:
CREATE INDEX IF NOT EXISTS ha_entities_last_updated_idx
    ON ha_entities (last_updated DESC);
