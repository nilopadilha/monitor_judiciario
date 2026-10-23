create table processes (
    id               uuid primary key,
    client_id        uuid         not null references clients (id),
    process_number   varchar(20)    not null,
    court_code       varchar(16)  not null references tribunals (code),
    subject          varchar(300),
    status           varchar(16)  not null,
    last_checked_at  timestamptz,
    created_at       timestamptz  not null,
    updated_at       timestamptz  not null,
    constraint uk_processes_number unique (process_number),
    constraint ck_processes_number check (process_number ~ '^[0-9]{20}$'),
    constraint ck_processes_status check (status in ('ACTIVE', 'PAUSED', 'ARCHIVED'))
);

create index idx_processes_client on processes (client_id);
create index idx_processes_status_checked on processes (status, last_checked_at nulls first);
