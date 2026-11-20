alter table clients alter column document type varchar(64);

alter table clients
    add column legal_basis varchar(32) not null default 'CONTRACT_EXECUTION',
    add column privacy_notice_version varchar(32),
    add column consent_recorded_at timestamptz,
    add column anonymized_at timestamptz;

alter table clients drop constraint ck_clients_status;
alter table clients add constraint ck_clients_status
    check (status in ('ACTIVE', 'INACTIVE', 'ANONYMIZED'));

alter table clients drop constraint ck_clients_document;
alter table clients add constraint ck_clients_document
    check (status = 'ANONYMIZED' or document ~ '^[0-9]{11}$|^[0-9]{14}$');

alter table clients drop constraint ck_clients_channel;
alter table clients add constraint ck_clients_channel
    check (status = 'ANONYMIZED' or notify_email or notify_whatsapp);

alter table clients add constraint ck_clients_legal_basis
    check (legal_basis in ('CONTRACT_EXECUTION', 'LEGITIMATE_INTEREST', 'CONSENT'));

alter table processes
    add column confidential boolean not null default false;
