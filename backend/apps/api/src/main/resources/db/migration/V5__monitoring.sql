create table publications (
    id                   uuid primary key,
    process_id           uuid         not null references processes (id),
    client_id            uuid         not null references clients (id),
    publication_hash     varchar(128) not null,
    content_fingerprint  varchar(64)  not null,
    djen_id              bigint,
    publication_date     date         not null,
    communication_type   varchar(120),
    document_type        varchar(120),
    organ_name           varchar(300),
    content              text         not null,
    link                 varchar(1000),
    source               varchar(8)   not null,
    read_at              timestamptz,
    created_at           timestamptz  not null,
    constraint uk_publications_process_hash unique (process_id, publication_hash),
    constraint ck_publications_source check (source in ('DJEN', 'PJE'))
);

create index idx_publications_process_fingerprint on publications (process_id, content_fingerprint);
create index idx_publications_date on publications (publication_date desc);
create index idx_publications_unread on publications (process_id) where read_at is null;
create index idx_publications_client on publications (client_id, publication_date desc);

create table movements (
    id              uuid primary key,
    process_id      uuid        not null references processes (id),
    client_id       uuid        not null references clients (id),
    publication_id  uuid references publications (id),
    movement_type   varchar(16) not null,
    description     text        not null,
    movement_date   date        not null,
    source          varchar(8)  not null,
    created_at      timestamptz not null,
    constraint ck_movements_type check (movement_type in
        ('SENTENCA', 'ACORDAO', 'DECISAO', 'DESPACHO', 'CITACAO', 'INTIMACAO', 'EDITAL', 'OUTROS'))
);

create index idx_movements_process_date on movements (process_id, movement_date desc);

create table monitoring_executions (
    id                 uuid primary key,
    trigger_type       varchar(16)  not null,
    triggered_by       varchar(254) not null,
    status             varchar(32)  not null,
    started_at         timestamptz  not null,
    finished_at        timestamptz,
    processes_checked  integer      not null default 0,
    new_publications   integer      not null default 0,
    failed_jobs        integer      not null default 0,
    error_message      varchar(500),
    constraint ck_executions_trigger check (trigger_type in ('SCHEDULED', 'MANUAL')),
    constraint ck_executions_status check (status in
        ('RUNNING', 'COMPLETED', 'COMPLETED_WITH_FAILURES', 'FAILED', 'ABORTED'))
);

create index idx_executions_started on monitoring_executions (started_at desc);

create table monitoring_jobs (
    id                 uuid primary key,
    execution_id       uuid        not null references monitoring_executions (id),
    process_id         uuid        not null references processes (id),
    execution_date     date        not null,
    status             varchar(16) not null,
    execution_time_ms  bigint      not null,
    new_publications   integer     not null default 0,
    error_message      varchar(500),
    created_at         timestamptz not null,
    constraint ck_jobs_status check (status in ('SUCCESS', 'FAILED'))
);

create index idx_jobs_execution on monitoring_jobs (execution_id);
create index idx_jobs_process_created on monitoring_jobs (process_id, created_at desc);

create table outbox_events (
    id              uuid primary key,
    aggregate_type  varchar(64)  not null,
    aggregate_id    uuid         not null,
    event_type      varchar(128) not null,
    payload         jsonb        not null,
    correlation_id  varchar(64),
    created_at      timestamptz  not null,
    published_at    timestamptz,
    attempts        integer      not null default 0,
    last_error      varchar(500)
);

create index idx_outbox_pending on outbox_events (created_at) where published_at is null;
