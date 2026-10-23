create table tribunals (
    id      uuid primary key,
    code    varchar(16)  not null,
    name    varchar(200) not null,
    state   varchar(2),
    active  boolean      not null default true,
    constraint uk_tribunals_code unique (code)
);

-- Os 27 Tribunais de Justiça (premissa P2). UUIDs fixos para ambientes reproduzíveis.
insert into tribunals (id, code, name, state) values
    ('01930000-0000-7000-8000-000000000001', 'TJAC', 'Tribunal de Justiça do Acre', 'AC'),
    ('01930000-0000-7000-8000-000000000002', 'TJAL', 'Tribunal de Justiça de Alagoas', 'AL'),
    ('01930000-0000-7000-8000-000000000003', 'TJAP', 'Tribunal de Justiça do Amapá', 'AP'),
    ('01930000-0000-7000-8000-000000000004', 'TJAM', 'Tribunal de Justiça do Amazonas', 'AM'),
    ('01930000-0000-7000-8000-000000000005', 'TJBA', 'Tribunal de Justiça da Bahia', 'BA'),
    ('01930000-0000-7000-8000-000000000006', 'TJCE', 'Tribunal de Justiça do Ceará', 'CE'),
    ('01930000-0000-7000-8000-000000000007', 'TJDFT', 'Tribunal de Justiça do Distrito Federal e dos Territórios', 'DF'),
    ('01930000-0000-7000-8000-000000000008', 'TJES', 'Tribunal de Justiça do Espírito Santo', 'ES'),
    ('01930000-0000-7000-8000-000000000009', 'TJGO', 'Tribunal de Justiça de Goiás', 'GO'),
    ('01930000-0000-7000-8000-000000000010', 'TJMA', 'Tribunal de Justiça do Maranhão', 'MA'),
    ('01930000-0000-7000-8000-000000000011', 'TJMT', 'Tribunal de Justiça de Mato Grosso', 'MT'),
    ('01930000-0000-7000-8000-000000000012', 'TJMS', 'Tribunal de Justiça de Mato Grosso do Sul', 'MS'),
    ('01930000-0000-7000-8000-000000000013', 'TJMG', 'Tribunal de Justiça de Minas Gerais', 'MG'),
    ('01930000-0000-7000-8000-000000000014', 'TJPA', 'Tribunal de Justiça do Pará', 'PA'),
    ('01930000-0000-7000-8000-000000000015', 'TJPB', 'Tribunal de Justiça da Paraíba', 'PB'),
    ('01930000-0000-7000-8000-000000000016', 'TJPR', 'Tribunal de Justiça do Paraná', 'PR'),
    ('01930000-0000-7000-8000-000000000017', 'TJPE', 'Tribunal de Justiça de Pernambuco', 'PE'),
    ('01930000-0000-7000-8000-000000000018', 'TJPI', 'Tribunal de Justiça do Piauí', 'PI'),
    ('01930000-0000-7000-8000-000000000019', 'TJRJ', 'Tribunal de Justiça do Rio de Janeiro', 'RJ'),
    ('01930000-0000-7000-8000-000000000020', 'TJRN', 'Tribunal de Justiça do Rio Grande do Norte', 'RN'),
    ('01930000-0000-7000-8000-000000000021', 'TJRS', 'Tribunal de Justiça do Rio Grande do Sul', 'RS'),
    ('01930000-0000-7000-8000-000000000022', 'TJRO', 'Tribunal de Justiça de Rondônia', 'RO'),
    ('01930000-0000-7000-8000-000000000023', 'TJRR', 'Tribunal de Justiça de Roraima', 'RR'),
    ('01930000-0000-7000-8000-000000000024', 'TJSC', 'Tribunal de Justiça de Santa Catarina', 'SC'),
    ('01930000-0000-7000-8000-000000000025', 'TJSE', 'Tribunal de Justiça de Sergipe', 'SE'),
    ('01930000-0000-7000-8000-000000000026', 'TJSP', 'Tribunal de Justiça de São Paulo', 'SP'),
    ('01930000-0000-7000-8000-000000000027', 'TJTO', 'Tribunal de Justiça do Tocantins', 'TO');
