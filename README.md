# Monitor Judicial Inteligente

SaaS que monitora processos judiciais no **DJEN** (Diário de Justiça Eletrônico Nacional), detecta publicações novas, classifica a evolução processual e notifica os clientes por **e-mail** e **WhatsApp** — de forma totalmente automatizada.

- **Arquitetura e decisões:** [`docs/ARQUITETURA.md`](docs/ARQUITETURA.md) (15 seções, no formato do prompt mestre) e [`docs/adr/`](docs/adr/)
- **Contrato da API:** [`docs/openapi/openapi.yaml`](docs/openapi/openapi.yaml) (gerado pelos testes) · Swagger UI em `/swagger-ui.html`

| Camada | Stack |
|---|---|
| Backend | Java 21 (Virtual Threads), Spring Boot 3.5, Security (JWT + refresh rotativo), Data JPA, Flyway, OpenFeign, MapStruct, Lombok, RabbitMQ, Redis, PostgreSQL 17 |
| Frontend | Vue 3 + Composition API, TypeScript estrito, Vite, Pinia, Vue Router, Axios, VueUse, Tailwind CSS v4, componentes no padrão ShadCN Vue (reka-ui), ECharts |
| Qualidade | JUnit 5, Mockito, Testcontainers, WireMock, ArchUnit, JaCoCo (≥ 90 %), Vitest, Vue Test Utils |
| Operação | Docker, Micrometer/Prometheus, Grafana, OpenTelemetry/Jaeger, logs ECS, GitHub Actions |

## Como rodar

### Tudo em contêineres

```bash
cp .env.example .env            # ajuste os segredos
docker compose -f docker/docker-compose.yml up --build
```

| Serviço | URL / Porta | Credenciais padrão (dev / Docker) |
|---|---|---|
| Aplicação web | http://localhost:8081 (Docker) · http://localhost:5173 (Vite) | **ADMIN:** `admin@monitorjudicial.local` / `admin-dev-12345`<br>**CUSTOMER (após seed):** `maria@cliente.example` / `cliente-dev-123` |
| API / Swagger | http://localhost:8080/swagger-ui.html | Autenticação via Bearer Token JWT |
| PostgreSQL 17 | `localhost:5432` (banco: `monitor_judicial`) | `monitor` / `monitor` |
| RabbitMQ (Management) | http://localhost:15672 (AMQP: `5672`) | `guest` / `guest` |
| Redis 7 | `localhost:6379` | *(sem senha)* |
| E-mails enviados (Mailpit) | http://localhost:8025 (SMTP: `1025`) | *(aberto)* |
| Grafana · Prometheus · Jaeger | http://localhost:3000 · :9090 · :16686 | Grafana: `admin` / `admin` (ou acesso anônimo Viewer) |

Sem saída de rede para `comunicaapi.pje.jus.br`, use o simulador do DJEN: defina `DJEN_BASE_URL=http://djen-mock:8089` no `.env` e suba com `--profile mock`.

### Desenvolvimento (Linux / macOS)

```bash
# infraestrutura
docker compose -f docker/docker-compose.yml up -d postgres redis rabbitmq mailpit

# API (perfil dev: segredos de exemplo e ADMIN inicial)
cd backend && ./gradlew :apps:api:bootRun --args='--spring.profiles.active=dev'

# simulador do DJEN (opcional) — então rode a API com DJEN_BASE_URL=http://localhost:8089
node scripts/djen-mock.mjs

# frontend (proxy de /api para :8080)
cd frontend && npm install && npm run dev        # http://localhost:5173

# dados de exemplo (3 clientes, 6 processos, 1 usuário CUSTOMER)
scripts/seed-dev.sh
```

### Desenvolvimento no Windows (PowerShell)

```powershell
# 1. Infraestrutura
docker compose -f docker/docker-compose.yml up -d postgres redis rabbitmq mailpit

# 2. Simulador do DJEN (em um terminal separado)
node scripts/djen-mock.mjs 8089

# 3. API (perfil dev apontando para o simulador local)
cd backend
$env:DJEN_BASE_URL="http://localhost:8089"
.\gradlew.bat :apps:api:bootRun --args="--spring.profiles.active=dev"

# 4. Frontend (em outro terminal — http://localhost:5173)
cd frontend
npm install
npm run dev

# 5. Dados de exemplo e disparo inicial de monitoramento (em outro terminal, na raiz do projeto)
.\scripts\seed-dev.ps1
```

> **Nota sobre o primeiro acesso:** No primeiro boot, o Flyway e o `AdminBootstrap` criam apenas os 27 Tribunais de Justiça e o usuário `ADMIN`. Execute `.\scripts\seed-dev.ps1` (Windows) ou `scripts/seed-dev.sh` (Linux/macOS) para popular os 3 clientes, 6 processos, o usuário `CUSTOMER` e disparar a primeira execução de monitoramento.

## Testes

```bash
cd backend && ./gradlew check     # unitários + integração (Testcontainers: requer Docker) + ArchUnit + cobertura ≥ 90 %
cd frontend && npm run typecheck && npm run lint && npm test
```

Estado na entrega: backend com **160 testes** e **96,3 % de cobertura de linhas** (agregada, JaCoCo; entidades JPA, configuração e DTOs de requisição excluídos); frontend com **24 testes**, tipagem e lint sem erros. Relatório de cobertura: `backend/apps/api/build/reports/jacoco/coverageReport/html/index.html`.

## Estrutura

```
backend/   apps/api (composição, Flyway, testes de integração) · modules/* (um por contexto) · shared · buildSrc
frontend/  src/{features,shared,layouts,router,stores}
docs/      arquitetura, ADRs, OpenAPI
docker/    Dockerfiles, nginx, docker-compose
infra/     Prometheus, Grafana (dashboard provisionado)
scripts/   simulador do DJEN, dados de exemplo (seed-dev.sh / seed-dev.ps1)
```

## Onde este projeto diverge do prompt (e por quê)

Detalhado na seção 2.1 do documento de arquitetura. Resumo:

1. `POST /monitoring/run` e `POST /notifications/send` violam o padrão PJe exigido pelo próprio prompt (URLs sem verbos) → `POST /monitoring-executions` e `POST /notifications` (ADR-0003).
2. Não há API pública de tramitação do PJe → no MVP as movimentações derivam do DJEN; a porta `CourtMovementSource` recebe o adapter MNI na fase 2 (ADR-0007).
3. SaaS sem tenant no modelo de dados → MVP single-tenant por implantação; RLS na fase 2 (ADR-0008).
4. Publicar no RabbitMQ após o commit perde eventos → Transactional Outbox (ADR-0004).
5. Hash do DJEN não detecta retificações → `content_fingerprint` separa republicação de novidade.

## Pendências e riscos conhecidos

- **Contrato do DJEN não validado contra o Swagger oficial.** O ambiente de desenvolvimento não tinha acesso a `comunicaapi.pje.jus.br`; o adapter foi modelado a partir da documentação pública, com caminhos configuráveis (`djen.paths.*`) e testes de contrato em WireMock. Validar as fixtures antes do go-live (risco R1).
- **WhatsApp** exige conta na Meta, número verificado e o template `nova_publicacao` aprovado (parâmetros: processo, data, tipo, trecho). Sem credenciais, os envios ficam `FAILED` com o motivo.
- **Refresh token no `localStorage`** do navegador: o access token fica só em memória e o refresh é rotativo com detecção de reuso, mas um XSS poderia lê-lo. Evolução recomendada: cookie `HttpOnly; Secure; SameSite=Strict` emitido pela API.
- **Dependências do frontend** fixadas em linhas estáveis anteriores às majors mais recentes (TypeScript 7, Vite 8, Pinia 4, Vue Router 5). `eslint@9` e `lucide-vue-next` aparecem como descontinuados no npm (substitutos: `eslint@10`, `@lucide/vue`).
- O grupo de health `readiness` responde normalmente na aplicação, mas não dentro do contexto `@SpringBootTest`; o teste de plataforma verifica o health agregado.
