create table audit_logs (
    id              uuid primary key,
    actor_id        uuid,
    actor_email     varchar(254) not null,
    action          varchar(64)  not null,
    resource_type   varchar(64)  not null,
    resource_id     varchar(64)  not null,
    details         jsonb        not null default '{}'::jsonb,
    correlation_id  varchar(64),
    occurred_at     timestamptz  not null
);

create index idx_audit_resource on audit_logs (resource_type, resource_id);
create index idx_audit_occurred on audit_logs (occurred_at desc);
