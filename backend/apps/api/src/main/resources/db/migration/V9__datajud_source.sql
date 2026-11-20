alter table publications alter column source type varchar(16);
alter table movements alter column source type varchar(16);

alter table publications drop constraint ck_publications_source;
alter table publications add constraint ck_publications_source
    check (source in ('DJEN', 'DATAJUD', 'PJE'));
