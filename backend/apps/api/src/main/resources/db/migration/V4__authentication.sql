create table users (
    id             uuid primary key,
    name           varchar(120) not null,
    email          varchar(254) not null,
    password_hash  varchar(100) not null,
    role           varchar(16)  not null,
    client_id      uuid references clients (id),
    active         boolean      not null default true,
    created_at     timestamptz  not null,
    updated_at     timestamptz  not null,
    constraint uk_users_email unique (email),
    constraint ck_users_role check (role in ('ADMIN', 'MANAGER', 'CUSTOMER')),
    constraint ck_users_customer_client check ((role = 'CUSTOMER') = (client_id is not null))
);

create table refresh_tokens (
    id              uuid primary key,
    user_id         uuid        not null references users (id),
    token_hash      varchar(64) not null,
    family_id       uuid        not null,
    expires_at      timestamptz not null,
    created_at      timestamptz not null,
    revoked_at      timestamptz,
    replaced_by_id  uuid,
    constraint uk_refresh_tokens_hash unique (token_hash)
);

create index idx_refresh_tokens_family on refresh_tokens (family_id) where revoked_at is null;
