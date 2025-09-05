-- Flyway Migration V2
-- Entfernt den redundanten Index auf der Spalte 'feed_id' in der Tabelle 'posted_entries'.
-- Dieser Index ist nicht mehr notwendig, da er durch den zusammengesetzten UNIQUE-Index
-- auf den Spalten (feed_id, entry_guid) vollständig abgedeckt wird.
-- Das Entfernen verbessert die Schreibleistung und spart Speicherplatz.

DROP INDEX IF EXISTS idx_posted_entries_feed_id;
