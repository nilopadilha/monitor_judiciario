# ADR-0004 — Transactional Outbox para eventos de monitoramento

- **Status:** aceito · **Data:** 2026-10-23

## Contexto
O fluxo do PDF é "Persistir Evento → Disparar Mensageria". Gravar no PostgreSQL e publicar no RabbitMQ são duas escritas não atômicas: uma queda entre elas perde a notificação (ou duplica, se a ordem for inversa).

## Decisão
Na mesma transação que grava `publications`/`movements`, grava-se uma linha em `outbox_events`. Um relay (`@Scheduled`, `SELECT … FOR UPDATE SKIP LOCKED`) publica no exchange `monitor.events` com publisher confirms e marca `published_at`. O consumidor é idempotente (`UNIQUE(event_id, channel)` em `notifications`). Mensagens que falham 3 vezes vão para a DLQ `notifications.process-evolution.dlq`.

## Consequências
+ Entrega *at-least-once* garantida; nenhum evento perdido. + Funciona com várias réplicas (SKIP LOCKED).
− Latência de até `outbox.relay-interval` (2 s). − Tabela precisa de limpeza periódica (fase 2).
