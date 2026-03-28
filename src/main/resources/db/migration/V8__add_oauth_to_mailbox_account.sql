-- Add OAuth fields to mailbox_account table
ALTER TABLE mailbox_account
    ADD COLUMN authenticationType VARCHAR(255) DEFAULT 'PASSWORD' NOT NULL;

ALTER TABLE mailbox_account
    ADD COLUMN accessToken OID; -- @Lob is typically OID or TEXT in Postgres

ALTER TABLE mailbox_account
    ADD COLUMN refreshToken OID;

ALTER TABLE mailbox_account
    ADD COLUMN accessTokenExpiry TIMESTAMP;

-- Make password optional
ALTER TABLE mailbox_account
    ALTER COLUMN password DROP NOT NULL;
