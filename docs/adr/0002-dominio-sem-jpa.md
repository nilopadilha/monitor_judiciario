# ADR-0002 — Domínio independente de JPA; mapeamento com MapStruct

- **Status:** aceito · **Data:** 2026-10-23

## Contexto
O PDF exige "Repository Port → Infrastructure Adapter" e proíbe expor estrutura de banco pelas APIs. Anotar o domínio com JPA acopla invariantes ao ciclo de vida do Hibernate (proxies, construtores vazios, setters).

## Decisão
Modelos de domínio são classes Java puras com invariantes no construtor/fábricas e acessores no estilo de records (`name()`). Entidades JPA (`*Entity`) ficam em `infrastructure.persistence`, com Lombok. Os mappers são interfaces `@Mapper` do MapStruct (componente Spring); a conversão entidade ↔ domínio é escrita explicitamente nos métodos `default` — reidratar pelo construtor garante que nenhum dado inválido do banco entre no domínio, e o MapStruct não detecta acessores fluentes. A geração automática do MapStruct é usada nos mapeamentos entre records (DTOs de integração ↔ comandos). Portas (`*Repository`) são interfaces no domínio; adapters implementam usando Spring Data.

## Consequências
+ Domínio testável sem Spring, invariantes garantidas. + Esquema evolui sem tocar no domínio.
− Mais classes (entidade + mapper por agregado). Aceito pelo ganho de isolamento.
