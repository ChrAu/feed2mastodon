create table ha_entities
(
    entity_id    varchar(255) not null,
    attributes   TEXT,
    last_changed timestamp(6) with time zone,
    last_updated timestamp(6) with time zone,
    state        varchar(255),
    primary key (entity_id)
);
create table ha_state_history
(
    id           bigint                      not null,
    attributes   TEXT,
    entity_id    varchar(255)                not null,
    last_changed timestamp(6) with time zone not null,
    state        varchar(255),
    primary key (id)
);
create table url_mapping
(
    id          bigint        not null,
    createdAt   timestamp(6) with time zone,
    originalUrl varchar(2048) not null,
    shortKey    varchar(255)  not null,
    shortUrl    varchar(2048) not null,
    primary key (id)
);
alter table if exists url_mapping
    drop constraint if exists UK9de0aie70tnbpesk9a3rlj92s;
alter table if exists url_mapping
    add constraint UK9de0aie70tnbpesk9a3rlj92s unique (shortKey);
create sequence ha_state_history_id_seq start with 1 increment by 1;
create sequence url_mapping_SEQ start with 1 increment by 50;
