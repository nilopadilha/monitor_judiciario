create table notifications (
    id             uuid primary key,
    event_id       uuid,
    process_id     uuid         not null references processes (id),
    client_id      uuid         not null references clients (id),
    channel        varchar(16)  not null,
    recipient      varchar(254) not null,
    subject        varchar(300),
    message        text         not null,
    status         varchar(16)  not null,
    attempts       integer      not null default 0,
    error_message  varchar(500),
    created_at     timestamptz  not null,
    sent_at        timestamptz,
    constraint uk_notifications_event_channel unique (event_id, channel),
    constraint ck_notifications_channel check (channel in ('EMAIL', 'WHATSAPP')),
    constraint ck_notifications_status check (status in ('PENDING', 'SENT', 'FAILED'))
);

create index idx_notifications_created on notifications (created_at desc);
create index idx_notifications_client on notifications (client_id, created_at desc);
