alter table if exists Embedding add column local_embedding_model TEXT;
alter table if exists public_mastodon_posts add column embedding_model TEXT;
alter table if exists Embedding drop constraint if exists idx_Embedding_uuid;
alter table if exists Embedding add constraint idx_Embedding_uuid unique (uuid);
