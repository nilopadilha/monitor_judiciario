# ADR-0001 — Monólito modular com Arquitetura Limpa

- **Status:** aceito · **Data:** 2026-10-23

## Contexto
O PDF define nove módulos de negócio e exige Clean Architecture/DDD. O MVP tem uma equipe pequena, um único banco e precisa de transações locais para garantir que publicação detectada e evento sejam gravados juntos.

## Decisão
Um único deployable Spring Boot (`apps/api`) composto por módulos Gradle independentes (`modules/*`). Cada módulo segue camadas `api / domain / application / infrastructure / web`. Outros módulos só podem depender do pacote `api` — regra verificada por ArchUnit.

## Alternativas
- **Microsserviços desde o início:** rejeitado — custo operacional (N bancos, sagas, observabilidade distribuída) sem volume que justifique.
- **Monólito em camadas sem módulos:** rejeitado — fronteiras erodem rapidamente; extração futura vira reescrita.

## Consequências
+ Uma transação, um deploy, refatoração barata. + Extração de `notifications` para serviço é mecânica (já se comunica por RabbitMQ).
− Disciplina de fronteira depende do ArchUnit no CI.
