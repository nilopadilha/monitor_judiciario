# ADR-0007 — Movimentações derivadas do DJEN no MVP; PJe/MNI atrás de uma porta

- **Status:** aceito · **Data:** 2026-10-23

## Contexto
O PDF pede comparar publicações do DJEN com tramitações do PJe. Não há API REST pública de tramitação do PJe: o acesso é via MNI (SOAP, credenciado por tribunal) ou consulta pública web sem contrato.

## Decisão
No MVP, cada publicação nova gera uma `Movement` classificada por `MovementClassifier` (tipo de documento/comunicação do DJEN). A porta `CourtMovementSource` é definida no domínio de `monitoring` com uma implementação vazia; o adapter MNI entra na fase 2 e passa a alimentar movimentações com `source = PJE`, permitindo a comparação DJEN × PJe pedida.

## Consequências
+ MVP entregável sem dependência de credenciais de tribunal. − A comparação DJEN × PJe real fica para a fase 2; o painel já distingue a origem (`source`).
