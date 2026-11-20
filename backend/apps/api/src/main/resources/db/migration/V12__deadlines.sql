create table deadlines (
    id              uuid primary key,
    process_id      uuid         not null references processes (id),
    publication_id  uuid references publications (id),
    lawyer_id       uuid references lawyers (id),
    title           varchar(200) not null,
    description     text,
    start_date      date         not null,
    due_date        date         not null,
    days_count      integer,
    business_days   boolean      not null default true,
    status          varchar(16)  not null default 'OPEN',
    auto_detected   boolean      not null default false,
    completed_at    timestamptz,
    last_alert_date date,
    created_at      timestamptz  not null,
    updated_at      timestamptz  not null,
    constraint ck_deadlines_status check (status in ('OPEN', 'COMPLETED', 'CANCELED'))
);

create unique index uk_deadlines_publication on deadlines (publication_id) where publication_id is not null;
create index idx_deadlines_status_due on deadlines (status, due_date asc);
create index idx_deadlines_lawyer_status_due on deadlines (lawyer_id, status, due_date asc);
create index idx_deadlines_process on deadlines (process_id, due_date asc);
