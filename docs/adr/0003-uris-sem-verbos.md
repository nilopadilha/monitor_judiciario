# ADR-0003 — URIs orientadas a recurso, sem verbos (correção do PDF)

- **Status:** aceito · **Data:** 2026-10-23

## Contexto
O próprio PDF exige os padrões de API do PJe: recursos no plural, URLs sem verbos, uso correto dos métodos HTTP. Porém lista `POST /api/v1/monitoring/run` e `POST /api/v1/notifications/send`, que contradizem essa regra.

## Decisão
| PDF | Adotado | Semântica |
|---|---|---|
| `GET /monitoring` | `GET /monitoring-executions` | lista execuções |
| `POST /monitoring/run` | `POST /monitoring-executions` | cria execução assíncrona → `202 Accepted` + `Location` |
| `POST /notifications/send` | `POST /notifications` | criar notificação = enviá-la → `201 Created` |
| login / refresh | `POST /tokens` (`grantType`) | emissão de token como recurso (modelo OAuth 2) |
| logout | `POST /token-revocations` | revogação como recurso (RFC 7009) |

## Consequências
+ Coerência com o padrão PJe exigido. − Clientes que esperassem as URIs literais do PDF precisam usar as novas.
