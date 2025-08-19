-- V3: Create the public_mastodon_posts table

CREATE TABLE IF NOT EXISTS public_mastodon_posts (
    id BIGINT NOT NULL,
    mastodon_id TEXT,
    post_text TEXT,
    url_text TEXT,
    cosinus_distance DOUBLE PRECISION,
    embedding_vector_string TEXT,
    create_at TIMESTAMP,
    status_original_url TEXT,
    negative_weight DOUBLE PRECISION,
    no_url BOOLEAN,
    viki_commented BOOLEAN,
    PRIMARY KEY (id)
);

-- Create a sequence for the primary key
CREATE SEQUENCE if not exists public_mastodon_posts_seq START 1 INCREMENT 50;

-- Create indexes for performance
CREATE INDEX if not exists idx_Embedding_cosDistance ON public_mastodon_posts (cosinus_distance);
CREATE INDEX if not exists idx_Embedding_create_at ON public_mastodon_posts (create_at);
CREATE UNIQUE INDEX if not exists idx_Embedding_mastodonId ON public_mastodon_posts (mastodon_id);


create table if not exists  embedding
(
    id                            bigint       not null
        primary key,
    created_at                    timestamp(6) not null,
    embedding_created_at          timestamp(6),
    embedding_vector_string       text,
    mastodon_status_id            text,
    resource                      text         not null
        constraint idx_embedding_resource
            unique,
    text                          text,
    uuid                          text         not null
        constraint uk1mhegu6yqtfkd0a21o2g0vhv7
            unique
        constraint idx_embedding_uuid
            unique,
    local_embedding_created_at    timestamp(6),
    local_embedding_vector_string text,
    url                           text,
    negative_weight               double precision,
    status_original_url           text
);
CREATE SEQUENCE if not exists embedding_seq START 1 INCREMENT 50;


create index if not exists  idx_embedding_mastodon_status_id
    on embedding (mastodon_status_id);

create index if not exists  idx_embedding_embedding_created_at
    on embedding (embedding_created_at);

create index if not exists  idx_embedding_local_embedding_created_at
    on embedding (local_embedding_created_at);


-- auto-generated definition
create table  if not exists eventplanentity
(
    id            bigint  not null
        primary key,
    createdat     timestamp(6),
    details       varchar(255),
    eventtype     varchar(255)
        constraint eventplanentity_eventtype_check
            check ((eventtype)::text = ANY
                   ((ARRAY ['MASTODON_NOTIFICATION'::character varying, 'LITTLE_VIKI'::character varying, 'BABY_VIKI'::character varying])::text[])),
    executed      boolean not null,
    executedat    timestamp(6),
    result        varchar(255),
    scheduledtime timestamp(6),
    uuid          varchar(255)
        constraint idx_eventplanentity_uuid
            unique
);
CREATE SEQUENCE if not exists eventplanentity_seq START 1 INCREMENT 50;


create index  if not exists idx_eventplanentity_createdat
    on eventplanentity (createdat);

create index  if not exists idx_eventplanentity_eventtype
    on eventplanentity (eventtype);

create index  if not exists idx_eventplanentity_scheduledtime
    on eventplanentity (scheduledtime);

create index  if not exists idx_eventplanentity_executed
    on eventplanentity (executed);

-- auto-generated definition
create table  if not exists geminirequestentity
(
    id              bigint not null
        primary key,
    model           varchar(255),
    text            text,
    timestamp       timestamp(6),
    uuid            varchar(255)
        constraint idx_requestentity_uuid
            unique,
    totaltokencount integer,
    response        text
);
CREATE SEQUENCE if not exists geminirequestentity_seq START 1 INCREMENT 50;



create index if not exists  idx_requestentity_model
    on geminirequestentity (model);

create index if not exists  idx_requestentity_timestamp
    on geminirequestentity (timestamp);



-- auto-generated definition
create table  if not exists mastodon_paging_config
(
    id       bigint not null
        primary key,
    max_id   text,
    min_id   text,
    resource text   not null
        constraint idx_pagingconfigentity_resource
            unique,
    sinceid  text
);
CREATE SEQUENCE if not exists mastodon_paging_config_seq START 1 INCREMENT 50;

-- auto-generated definition
create table  if not exists monitoredfeed
(
    id          bigint       not null
        primary key,
    adddate     timestamp(6) not null,
    defaulttext text,
    feedurl     text         not null
        constraint ukle7yaucmo69mv9b2ui4r4wuu1
            unique,
    isactive    boolean      not null,
    title       text,
    tryai       boolean
);

CREATE SEQUENCE if not exists monitoredfeed_seq START 1 INCREMENT 50;



create index  if not exists idx_monitoredfeed_isactive
    on monitoredfeed (isactive);

create index  if not exists idx_monitoredfeed_feedurl
    on monitoredfeed (feedurl);


-- auto-generated definition
create table  if not exists postedentry
(
    id               bigint not null
        primary key,
    entryguid        text   not null,
    mastodonstatusid text   not null,
    postedat         timestamp(6) with time zone,
    feed_id          bigint not null
        constraint fkgj0wj2m7vv4756whefts4pyrm
            references monitoredfeed,
    aitoot           boolean,
    constraint ukrext8lm2fokqnkks3uo7ygjlx
        unique (feed_id, entryguid)
);
CREATE SEQUENCE if not exists postedentry_seq START 1 INCREMENT 50;


create index  if not exists idx_postedentry_entryguid_feedid
    on postedentry (entryguid, feed_id);

-- auto-generated definition
create table  if not exists promptentity
(
    id        bigint       not null
        primary key,
    createdat timestamp(6) not null,
    prompt    text,
    uuid      varchar(255) not null
        constraint idx_promptentity_uuid
            unique
);

CREATE SEQUENCE if not exists promptentity_seq START 1 INCREMENT 50;

create index if not exists  idx_promptentity_createdat
    on promptentity (createdat);




-- auto-generated definition
create table themenentity
(
    id       bigint not null
        primary key,
    lastpost date,
    thema    varchar(255)
        constraint idx_themenentity_thema
            unique,
    uuid     varchar(255)
        constraint idx_themenentity_uuid
            unique,
    last     date
);


CREATE SEQUENCE if not exists themenentity_seq START 1 INCREMENT 50;
