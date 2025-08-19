-- Create the message log table to track all outgoing broadcast messages
CREATE TABLE telegram_message_log (
    id BIGINT NOT NULL,
    subscriber_id BIGINT NOT NULL,
    message_content VARCHAR(4096),
    sent_at TIMESTAMP NOT NULL,
    successfully_sent BOOLEAN,
    delivery_timestamp TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_subscriber
        FOREIGN KEY(subscriber_id)
        REFERENCES telegram_subscribers(id)
);

-- Create a sequence for the primary key of the message log table, which is standard for Panache entities
CREATE SEQUENCE telegram_message_log_seq START 1 INCREMENT 50;
