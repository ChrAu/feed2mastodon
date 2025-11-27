CREATE TABLE ha_temperature_history
(
    id                  BIGINT NOT NULL PRIMARY KEY,
    current_temperature DOUBLE PRECISION,
    should_temperature  DOUBLE PRECISION,
    ha_state_history_id BIGINT NOT NULL
);

ALTER TABLE ha_temperature_history
    ADD CONSTRAINT uk_temperature_history_state_history_id UNIQUE (ha_state_history_id);

CREATE SEQUENCE ha_temperature_history_id_seq
    START WITH 1
    INCREMENT BY 1;

ALTER TABLE ha_temperature_history
    ADD CONSTRAINT fk_temperature_history_state_history
        FOREIGN KEY (ha_state_history_id)
            REFERENCES ha_state_history (id);
