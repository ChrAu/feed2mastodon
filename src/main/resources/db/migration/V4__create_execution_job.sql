CREATE TABLE if not exists ExecutionJob (
    id BIGINT NOT NULL PRIMARY KEY,
    schedulerName TEXT,
    executionTime TIMESTAMP,
    completed BOOLEAN
);

CREATE SEQUENCE if not exists ExecutionJob_SEQ
    START WITH 1
    INCREMENT BY 1;
