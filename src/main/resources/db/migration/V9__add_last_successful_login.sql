-- Add last successful login timestamp to mailbox_account table
ALTER TABLE mailbox_account ADD COLUMN last_successful_login TIMESTAMP;