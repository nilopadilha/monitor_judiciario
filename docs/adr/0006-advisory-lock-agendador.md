# ADR-0006 — Exclusão mútua do agendador via advisory lock do PostgreSQL

- **Status:** aceito · **Data:** 2026-10-23

## Contexto
Com mais de uma réplica da API, `@Scheduled` dispara em todas, gerando execuções duplicadas e o dobro de chamadas ao DJEN.

## Decisão
Antes de iniciar uma execução, `pg_try_advisory_lock(<chave fixa>)` em uma conexão dedicada. Quem não obtém o lock desiste (agendado) ou recebe `409 Conflict` (manual). O lock é liberado ao final ou automaticamente se a conexão cair.

## Alternativas
ShedLock (dependência extra, tabela própria); Quartz cluster (pesado). O advisory lock usa o banco que já temos, sem tabela adicional.

## Consequências
+ Zero dependências novas; liberação automática em falha. − Acoplado ao PostgreSQL (aceitável: banco do projeto).
