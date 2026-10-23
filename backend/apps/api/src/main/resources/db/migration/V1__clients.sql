create table clients (
    id               uuid primary key,
    name             varchar(200) not null,
    document         varchar(14)  not null,
    email            varchar(254) not null,
    phone            varchar(16),
    status           varchar(16)  not null,
    notify_email     boolean      not null default true,
    notify_whatsapp  boolean      not null default false,
    created_at       timestamptz  not null,
    updated_at       timestamptz  not null,
    constraint uk_clients_document unique (document),
    constraint ck_clients_status check (status in ('ACTIVE', 'INACTIVE')),
    constraint ck_clients_document check (document ~ '^[0-9]{11}$|^[0-9]{14}$'),
    constraint ck_clients_channel check (notify_email or notify_whatsapp),
    constraint ck_clients_whatsapp_phone check (not notify_whatsapp or phone is not null)
);

create index idx_clients_name on clients (lower(name));
