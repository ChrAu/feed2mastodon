

ALTER TABLE mastodon_posts ADD COLUMN if not exists intern_mastodon_url TEXT default NULL;
