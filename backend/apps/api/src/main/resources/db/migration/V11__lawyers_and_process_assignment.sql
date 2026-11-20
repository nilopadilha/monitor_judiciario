create table lawyers (
    id               uuid primary key,
    name             varchar(200) not null,
    oab_number       varchar(20)  not null,
    oab_uf           varchar(2)   not null,
    practice_area    varchar(32)  not null default 'CIVEL',
    email            varchar(254) not null,
    phone            varchar(16),
    user_id          uuid references users (id),
    notify_email     boolean      not null default true,
    notify_whatsapp  boolean      not null default false,
    active           boolean      not null default true,
    created_at       timestamptz  not null,
    updated_at       timestamptz  not null,
    constraint uk_lawyers_oab unique (oab_number, oab_uf),
    constraint ck_lawyers_uf check (oab_uf ~ '^[A-Z]{2}$'),
    constraint ck_lawyers_whatsapp_phone check (not notify_whatsapp or phone is not null)
);

create index idx_lawyers_name on lawyers (lower(name));
create index idx_lawyers_area on lawyers (practice_area);

alter table processes
    add column lawyer_id uuid references lawyers (id),
    add column practice_area varchar(32);

create index idx_processes_lawyer on processes (lawyer_id);
