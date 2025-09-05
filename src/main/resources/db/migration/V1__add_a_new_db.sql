-- ========= V1: Finales Optimiertes Datenbankschema =========
-- Dieses Skript ist für die vollständige Neuinstallation der Datenbank konzipiert.
-- Es konsolidiert alle bisherigen Migrationen und wendet Best Practices an:
-- 1. Konsistente Namensgebung (snake_case).
-- 2. Optimale Datentypen (UUID, TIMESTAMPTZ, ENUM).
-- 3. Automatische Sequenzerstellung durch BIGSERIAL.
-- 4. Logische Indizierung für bessere Performance.

-- Erforderliche Extension für Vektor-Operationen
CREATE EXTENSION IF NOT EXISTS vector;

-- ========= Benutzerdefinierte Typen =========
-- Ein ENUM-Typ für Event-Typen sorgt für Typsicherheit und ist performanter als Strings.
CREATE TYPE event_type AS ENUM ('MASTODON_NOTIFICATION', 'LITTLE_VIKI', 'BABY_VIKI');


-- ========= Tabellendefinitionen =========

-- Die Tabelle 'text_contents' (vorher 'text') wird zuerst erstellt,
-- da viele andere Tabellen darauf verweisen.
CREATE TABLE IF NOT EXISTS text_contents (
                                             id            BIGSERIAL PRIMARY KEY,
                                             content       TEXT,
                                             created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                             updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Die Tabelle 'monitored_feeds' (vorher 'monitoredfeed')
CREATE TABLE IF NOT EXISTS monitored_feeds (
                                               id           BIGSERIAL PRIMARY KEY,
                                               feed_url     TEXT NOT NULL UNIQUE,
                                               title        TEXT,
                                               default_text TEXT,
                                               is_active    BOOLEAN NOT NULL DEFAULT TRUE,
                                               try_ai       BOOLEAN DEFAULT FALSE,
                                               added_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Die Tabelle 'telegram_subscribers'
CREATE TABLE IF NOT EXISTS telegram_subscribers (
                                                    id         BIGSERIAL PRIMARY KEY,
                                                    chat_id    TEXT NOT NULL UNIQUE, -- Telegram Chat-IDs sind numerisch
                                                    is_active  BOOLEAN NOT NULL DEFAULT TRUE
);

-- Die Tabelle 'themes' (vorher 'themenentity')
CREATE TABLE IF NOT EXISTS themes (
                                      id         BIGSERIAL PRIMARY KEY,
                                      uuid       UUID NOT NULL UNIQUE,
                                      theme      VARCHAR(255) UNIQUE,
                                      last_post  DATE
);

-- Die Tabelle 'prompts' (vorher 'promptentity')
CREATE TABLE IF NOT EXISTS prompts (
                                       id         BIGSERIAL PRIMARY KEY,
                                       uuid       UUID NOT NULL UNIQUE,
                                       prompt     TEXT,
                                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Die Tabelle 'mastodon_posts' (vorher 'public_mastodon_posts')
CREATE TABLE IF NOT EXISTS mastodon_posts (
                                              id                    BIGSERIAL PRIMARY KEY,
                                              mastodon_id           TEXT UNIQUE,
                                              post_text_id          BIGINT REFERENCES text_contents(id),
                                              url_text_id           BIGINT REFERENCES text_contents(id),
                                              status_original_url   TEXT,
                                              embedding_id          BIGINT REFERENCES text_contents(id),
                                              embedding_model       TEXT,
                                              cosinus_distance      DOUBLE PRECISION,
                                              negative_weight       DOUBLE PRECISION,
                                              no_url                BOOLEAN DEFAULT FALSE,
                                              viki_commented        BOOLEAN DEFAULT FALSE,
                                              posted_at             TIMESTAMPTZ, -- 'create_at' umbenannt für Klarheit
                                              created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                              updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Die Tabelle 'embeddings'
CREATE TABLE IF NOT EXISTS embeddings (
                                          id                                  BIGSERIAL PRIMARY KEY,
                                          uuid                                UUID NOT NULL UNIQUE,
                                          resource                            TEXT NOT NULL UNIQUE,
                                          mastodon_status_id                  TEXT,
                                          url                                 TEXT,
                                          status_original_url                 TEXT,
                                          negative_weight                     DOUBLE PRECISION,
                                          text_id                             BIGINT REFERENCES text_contents(id) UNIQUE,
                                          embedding_vector_string_id          BIGINT REFERENCES text_contents(id) UNIQUE,
                                          local_embedding_vector_string_id    BIGINT REFERENCES text_contents(id) UNIQUE,
                                          local_embedding_model               TEXT,
                                          embedding_created_at                TIMESTAMPTZ,
                                          local_embedding_created_at          TIMESTAMPTZ,
                                          created_at                          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Die Tabelle 'telegram_message_logs'
CREATE TABLE IF NOT EXISTS telegram_message_logs (
                                                     id                  BIGSERIAL PRIMARY KEY,
                                                     subscriber_id       BIGINT NOT NULL REFERENCES telegram_subscribers(id),
                                                     message_content     VARCHAR(4096),
                                                     sent_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                                     successfully_sent   BOOLEAN,
                                                     delivery_timestamp  TIMESTAMPTZ
);

-- Die Tabelle 'execution_jobs' (vorher 'ExecutionJob')
CREATE TABLE IF NOT EXISTS execution_jobs (
                                              id             BIGSERIAL PRIMARY KEY,
                                              scheduler_name TEXT,
                                              execution_time TIMESTAMPTZ,
                                              is_completed   BOOLEAN DEFAULT FALSE
);

-- Die Tabelle 'event_plans' (vorher 'event_plans' / 'eventplanentity')
CREATE TABLE IF NOT EXISTS event_plans (
                                           id               BIGSERIAL PRIMARY KEY,
                                           uuid             UUID NOT NULL UNIQUE,
                                           event_type       event_type,
                                           details          VARCHAR(255),
                                           scheduled_time   TIMESTAMPTZ,
                                           is_executed      BOOLEAN NOT NULL DEFAULT FALSE,
                                           executed_at      TIMESTAMPTZ,
                                           result           VARCHAR(255),
                                           created_at       TIMESTAMPTZ DEFAULT NOW()
);

-- Die Tabelle 'gemini_requests'
CREATE TABLE IF NOT EXISTS gemini_requests (
                                               id                BIGSERIAL PRIMARY KEY,
                                               uuid              UUID NOT NULL UNIQUE,
                                               model             VARCHAR(255),
                                               request_text      TEXT,
                                               response_text     TEXT,
                                               total_token_count INTEGER,
                                               created_at        TIMESTAMPTZ DEFAULT NOW()
);

-- Die Tabelle 'mastodon_paging_configs'
CREATE TABLE IF NOT EXISTS mastodon_paging_configs (
                                                       id       BIGSERIAL PRIMARY KEY,
                                                       resource TEXT NOT NULL UNIQUE,
                                                       max_id   TEXT,
                                                       min_id   TEXT,
                                                       since_id TEXT
);

-- Die Tabelle 'posted_entries' (vorher 'postedentry')
CREATE TABLE IF NOT EXISTS posted_entries (
                                              id                 BIGSERIAL PRIMARY KEY,
                                              feed_id            BIGINT NOT NULL REFERENCES monitored_feeds(id),
                                              entry_guid         TEXT NOT NULL,
                                              mastodon_status_id TEXT NOT NULL,
                                              ai_toot            BOOLEAN,
                                              posted_at          TIMESTAMPTZ,
                                              UNIQUE (feed_id, entry_guid)
);


-- ========= Index-Definitionen =========
-- Indizes werden für Fremdschlüssel und häufig gefilterte Spalten erstellt,
-- um die Abfragegeschwindigkeit zu maximieren.

-- Indizes für mastodon_posts
CREATE INDEX IF NOT EXISTS idx_mastodon_posts_cosinus_distance ON mastodon_posts(cosinus_distance);
CREATE INDEX IF NOT EXISTS idx_mastodon_posts_posted_at ON mastodon_posts(posted_at);

-- Indizes für embeddings
CREATE INDEX IF NOT EXISTS idx_embeddings_mastodon_status_id ON embeddings(mastodon_status_id);
CREATE INDEX IF NOT EXISTS idx_embeddings_embedding_created_at ON embeddings(embedding_created_at);

-- Index für telegram_message_logs (wichtiger FK-Index)
CREATE INDEX IF NOT EXISTS idx_telegram_message_logs_subscriber_id ON telegram_message_logs(subscriber_id);

-- Indizes für event_plans
CREATE INDEX IF NOT EXISTS idx_event_plans_event_type ON event_plans(event_type);
CREATE INDEX IF NOT EXISTS idx_event_plans_scheduled_time ON event_plans(scheduled_time);
CREATE INDEX IF NOT EXISTS idx_event_plans_is_executed ON event_plans(is_executed);

-- Indizes für gemini_requests
CREATE INDEX IF NOT EXISTS idx_gemini_requests_model ON gemini_requests(model);
CREATE INDEX IF NOT EXISTS idx_gemini_requests_created_at ON gemini_requests(created_at);

-- Indizes für monitored_feeds
CREATE INDEX IF NOT EXISTS idx_monitored_feeds_is_active ON monitored_feeds(is_active);

-- Indizes für posted_entries (FK-Index)
CREATE INDEX IF NOT EXISTS idx_posted_entries_feed_id ON posted_entries(feed_id);

-- Indizes für prompts
CREATE INDEX IF NOT EXISTS idx_prompts_created_at ON prompts(created_at);

