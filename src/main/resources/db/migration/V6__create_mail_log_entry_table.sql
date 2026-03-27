CREATE TABLE mail_log_entry (
    id BIGSERIAL PRIMARY KEY,
    uniqueMailId VARCHAR(255) NOT NULL,
    recipientEmail VARCHAR(255) NOT NULL,
    senderEmail VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    bodySnippet TEXT,
    sentTimestamp TIMESTAMP NOT NULL,
    sentStatus VARCHAR(50) NOT NULL,
    errorMessage TEXT,
    receivedTimestamp TIMESTAMP,
    receivedStatus VARCHAR(50),
    receptionCheckMessage TEXT
);
