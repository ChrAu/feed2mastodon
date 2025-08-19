-- V1: Create the initial telegram_subscribers table
-- This script defines the baseline for the subscribers table.

-- If you are running this on a development machine where Hibernate already created this table,
-- you must first DROP the old table and its sequence manually to allow Flyway to take over.
-- Connect to your database and run:
-- DROP TABLE telegram_subscribers CASCADE;
-- DROP SEQUENCE telegram_subscribers_seq;

CREATE TABLE telegram_subscribers (
    id BIGINT NOT NULL,
    chat_id VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_subscriber_chat_id UNIQUE (chat_id)
);

CREATE SEQUENCE telegram_subscribers_seq START 1 INCREMENT 50;
