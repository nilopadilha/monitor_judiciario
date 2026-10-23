# ADR-0008 — MVP single-tenant; multi-tenant por RLS na fase 2

- **Status:** aceito · **Data:** 2026-10-23

## Contexto
O produto é SaaS, mas o modelo de dados do PDF não tem tenant. Introduzir multi-tenancy pela metade (coluna sem enforcement) é pior que não ter.

## Decisão
O MVP roda uma implantação por escritório. Na fase 2: coluna `tenant_id` em todas as tabelas de negócio, `ALTER TABLE … ENABLE ROW LEVEL SECURITY` com política `tenant_id = current_setting('app.tenant_id')::uuid`, definida por conexão a partir do JWT.

## Consequências
+ MVP simples e seguro (isolamento físico). − Custo de infraestrutura por cliente até a fase 2; migração planejada e documentada.
