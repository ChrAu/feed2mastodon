CREATE TABLE ExecutionJob (
    id BIGINT NOT NULL PRIMARY KEY,
    schedulerName TEXT,
    executionTime TIMESTAMP,
    completed BOOLEAN
);

CREATE SEQUENCE ExecutionJob_SEQ
    START WITH 1
    INCREMENT BY 1;
