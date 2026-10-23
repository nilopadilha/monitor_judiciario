# ADR-0009 — JWT HS256 stateless com refresh token opaco e rotativo

- **Status:** aceito · **Data:** 2026-10-23

## Decisão
- Access token JWT HS256, 15 min, claims `sub`, `role`, `clientId`.
- Refresh token **opaco** (256 bits aleatórios), persistido **apenas como SHA-256**, 7 dias, rotacionado a cada uso; cada cadeia tem `family_id`. Reapresentar um refresh já rotacionado revoga toda a família (indicador de roubo).
- Segredo HS256 via variável de ambiente (`JWT_SECRET`, ≥ 256 bits). A aplicação recusa iniciar com segredo curto.

## Alternativas
RS256/JWKS — melhor quando outros serviços validarem tokens; adotado na fase 2 junto com a extração de serviços.

## Consequências
+ Revogação efetiva de sessões (refresh), access curto limita a janela de abuso. − Access token não é revogável antes de expirar (15 min).
