# imob-erp-backend

Backend do ERP + CRM para pequenas imobiliárias. API REST construída com Spring Boot 3 (Java 21), autenticação via Clerk e banco PostgreSQL.

---

## Visão Geral do Sistema

Plataforma SaaS multi-tenant que integra CRM (pipeline de leads, visitas) e ERP (contratos, financeiro, comissões) em um único sistema vertical para imobiliárias de 2 a 10 corretores.

**Fluxo principal:** Lead → Fechamento → Contrato → Parcelas Financeiras → Comissão do Corretor

---

## Stack

| Camada | Tecnologia | Versão |
|---|---|---|
| Linguagem | Java | 21 |
| Framework | Spring Boot | 3.x |
| Banco de dados | PostgreSQL | 15+ |
| ORM | Spring Data JPA | — |
| Migrations | Flyway | — |
| Autenticação | Clerk (JWT) | — |
| Storage | Cloudflare R2 (S3-compatible) | — |
| Build | Maven | 3.9+ |
| Deploy | Railway | — |

---

## Arquitetura

Monólito modular, **package-by-feature**, com três sub-camadas por módulo (detalhes em `docs/architecture.md`):

```
api/      →  Controller + DTOs (XRequest / XResponse): entrada HTTP, validação, serialização
domain/   →  Entidade JPA, Service (regras de negócio), interface XRepository (porta), enums/
infra/    →  JpaXRepository (implementa a porta via Spring Data) e XMapper (MapStruct)
```

**Regras inegociáveis:**
- `Service` depende apenas da interface `XRepository` (em `domain/`), nunca de `JpaXRepository`
- `Controller` nunca contém regra de negócio — apenas delega ao `Service`
- Entidades JPA nunca são expostas na API — use DTOs em todos os endpoints
- Conversão entidade ↔ DTO via **MapStruct** (sem mapeamento manual)
- `tenantId` vem sempre do `TenantContext` (JWT), nunca do body

**Desvios conscientes do MVP:** as entidades JPA ficam em `domain/`; os Services retornam DTOs de `api/` e usam o mapper de `infra/`; as interfaces de repositório usam `Page`/`Pageable` do Spring Data.

---

## Estrutura de Pacotes

```
src/main/java/com/imobcrm/
├── config/                          # SecurityConfig, ClerkJwtAuthenticationFilter, RequestLoggingFilter, WebMvcConfig, ...
├── tenant/                          # TenantContext (ThreadLocal) e TenantInterceptor
├── shared/
│   ├── exception/                   # ApiException, BusinessException, GlobalExceptionHandler, ...
│   ├── pagination/                  # PageResponse<T>
│   ├── audit/                       # AuditEntity (createdAt, updatedAt)
│   └── dto/                         # ErrorResponse
├── storage/                         # R2StorageService, StorageProperties, R2ClientConfig
│
└── {property,user,lead,visit,contract,financial,commission}/   # Módulos de negócio
    ├── api/                         # {X}Controller, {X}Request, {X}Response, {X}{Acao}Request
    ├── domain/                      # {X} (entidade), {X}Service, {X}Repository (interface)
    │   └── enums/                   # Enums do módulo
    └── infra/                       # Jpa{X}Repository (implementa a interface), {X}Mapper (MapStruct)
                                     # financial/domain também contém OverdueJob (@Scheduled)
src/main/resources/
├── application.yml
├── application-dev.yml
├── application-prod.yml
└── db/migration/
    └── V1__create_initial_schema.sql
```

---

## Pré-requisitos

- Java 21 (JDK 21 exato: com o JDK 25 o Lombok não compila)
- Maven 3.9+
- Docker (para rodar PostgreSQL localmente)
- Conta no [Clerk](https://clerk.com) (gratuita)
- Conta no [Cloudflare R2](https://www.cloudflare.com/products/r2/) (gratuita)

---

## Configuração Local

### 1. Clone o repositório

```bash
git clone https://github.com/seu-usuario/imob-erp-backend.git
cd imob-erp-backend
```

### 2. Suba o PostgreSQL com Docker

```bash
docker run --name imob-postgres \
  -e POSTGRES_DB=imobcrm \
  -e POSTGRES_USER=imobuser \
  -e POSTGRES_PASSWORD=imobpass \
  -p 5432:5432 \
  -d postgres:15
```

> Alternativa: `docker compose up -d postgres` na raiz do monorepo (Postgres 16 na porta **5434**, usuário `imobuser`, senha `imobpass`, banco `imobcrm`). É o padrão do `application.yml`.

### 3. Configure as variáveis de ambiente

Crie um arquivo `.env` na raiz do projeto (nunca commitar):

```env
# Banco de dados
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/imobcrm
SPRING_DATASOURCE_USERNAME=imobuser
SPRING_DATASOURCE_PASSWORD=imobpass

# Clerk
CLERK_SECRET_KEY=sk_test_...
CLERK_JWKS_URL=https://<seu-frontend-api>.clerk.accounts.dev/.well-known/jwks.json
CLERK_ISSUER=https://<seu-frontend-api>.clerk.accounts.dev

# Cloudflare R2
R2_ACCOUNT_ID=
R2_ACCESS_KEY_ID=
R2_SECRET_ACCESS_KEY=
R2_BUCKET_NAME=imobcrm-dev
R2_PUBLIC_URL=https://pub-xxx.r2.dev

# E-mail (SMTP). Em dev, o padrao aponta para o Mailpit (docker compose up -d mailpit; UI em http://localhost:8025)
MAIL_HOST=localhost
MAIL_PORT=1025
MAIL_FROM=no-reply@imob-erp.local
# Em producao (ex.: Resend: smtp.resend.com:587, usuario "resend", senha = API key)
# MAIL_USERNAME=
# MAIL_PASSWORD=
# MAIL_SMTP_AUTH=true
# MAIL_STARTTLS=true
```

> **Como obter as credenciais do Clerk:**
> 1. Acesse [dashboard.clerk.com](https://dashboard.clerk.com)
> 2. Selecione seu app → API Keys
> 3. Copie `Secret key` e o JWKS URL em `Advanced → JWKS URL`

### 4. Execute a aplicação

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

A API estará disponível em `http://localhost:8080`.

Documentação Swagger: `http://localhost:8080/swagger-ui.html`

Health check: `http://localhost:8080/actuator/health`

---

## Variáveis de Ambiente (referência completa)

| Variável | Descrição | Obrigatória |
|---|---|---|
| `SPRING_DATASOURCE_URL` | URL JDBC do PostgreSQL | ✅ |
| `SPRING_DATASOURCE_USERNAME` | Usuário do banco | ✅ |
| `SPRING_DATASOURCE_PASSWORD` | Senha do banco | ✅ |
| `CLERK_SECRET_KEY` | Chave secreta do Clerk | ✅ |
| `CLERK_JWKS_URL` | URL do JWKS para validação de JWT | ✅ |
| `CLERK_ISSUER` | Issuer esperado no claim `iss` do JWT (Frontend API URL do Clerk, sem barra final) | ✅ |
| `CLERK_AUTHORIZED_PARTIES` | Origens aceitas no claim `azp`, separadas por vírgula (padrão: `APP_CORS_ORIGIN`) | ❌ |
| `OPERATOR_API_KEY` | Chave do operador do produto para criar imobiliárias (`POST /internal/v1/tenants`). Vazia = endpoint desligado | ❌ |
| `R2_ACCOUNT_ID` | ID da conta Cloudflare | ✅ |
| `R2_ACCESS_KEY_ID` | Access Key do R2 | ✅ |
| `R2_SECRET_ACCESS_KEY` | Secret Key do R2 | ✅ |
| `R2_BUCKET_NAME` | Nome do bucket R2 | ✅ |
| `R2_PUBLIC_URL` | URL pública do bucket R2 | ✅ |
| `APP_SEED_ENABLED` | Aplica os dados de exemplo ao subir no perfil `dev` (padrão: `true`; nunca roda em prod/test) | ❌ |
| `APP_SEED_TENANT_SLUG` | Tenant que recebe os dados de exemplo (padrão: `imobiliaria-dev`; criado se não existir) | ❌ |
| `APP_SEED_ADMIN_CLERK_ID` | Se informado, o seed também cria um admin com esse `clerk_user_id` | ❌ |
| `MAIL_HOST` | Servidor SMTP (padrão: `localhost`, o Mailpit do `docker-compose`) | ❌ (✅ em produção) |
| `MAIL_PORT` | Porta SMTP (padrão: `1025`) | ❌ (✅ em produção) |
| `MAIL_USERNAME` | Usuário SMTP | ❌ (✅ em produção) |
| `MAIL_PASSWORD` | Senha ou API key do SMTP | ❌ (✅ em produção) |
| `MAIL_SMTP_AUTH` | `true` para autenticar no SMTP (padrão: `false`) | ❌ |
| `MAIL_STARTTLS` | `true` para usar STARTTLS (padrão: `false`) | ❌ |
| `MAIL_FROM` | Remetente dos e-mails (padrão: `no-reply@imob-erp.local`); em produção use um endereço do domínio com SPF/DKIM/DMARC | ❌ (✅ em produção) |

### Dados de exemplo (seed de desenvolvimento)

No perfil `dev`, ao subir o backend é aplicada uma base mínima no tenant `APP_SEED_TENANT_SLUG`: 3 corretores (comissão 3%, 4% e 5%) e 1 financeiro, 6 imóveis (venda, aluguel e ambos), 8 leads em todas as etapas, 4 visitas, 3 contratos (venda ativa, locação ativa e rascunho), 2 comissões e 7 lançamentos financeiros (pago, pendente e atrasado).

- É **idempotente**: se o corretor `seed_corretor_1` já existe, nada é feito. Para reaplicar, apague os dados do tenant (ou recrie o banco) e suba de novo.
- Os usuários de seed usam `clerk_user_id` fictício (`seed_*`) e **não fazem login**. Para navegar na UI, use o seu usuário Clerk já vinculado ao tenant (o `publicMetadata.tenantId` do Clerk deve ser o id do tenant `APP_SEED_TENANT_SLUG`).
- Código em `shared/seed/DevDataSeeder.java`.

---

## Autenticação

Todas as rotas (exceto `/actuator/health`) exigem o header:

```
Authorization: Bearer <clerk_jwt>
```

O JWT do Clerk deve conter em `publicMetadata`:

```json
{
  "tenantId": "uuid-da-imobiliaria",
  "role": "ADMIN | CORRETOR | FINANCEIRO"
}
```

O `ClerkJwtAuthenticationFilter` valida o token a cada requisição:

1. **Assinatura** (RS256) via JWKS do Clerk (`CLERK_JWKS_URL`) e expiração (`exp`/`nbf`)
2. **`iss`** igual a `CLERK_ISSUER` e presença de `sub` (`ClerkJwtClaimsVerifier`)
3. **`azp`**, quando presente, dentro de `CLERK_AUTHORIZED_PARTIES` (padrão: `APP_CORS_ORIGIN`)
4. **Usuário local:** o `sub` é resolvido em `users` por `clerk_user_id`. O `userId` do contexto é o `users.id` real. Usuário inexistente, inativo ou de tenant diferente do token **não é autenticado**

> O usuário precisa existir na tabela `users` (previsto: webhook `user.created` do Clerk, ainda não implementado — por ora crie a linha manualmente ou via convite).

O `TenantInterceptor` popula o `TenantContext` e o MDC de logs; o `RequestLoggingFilter` limpa ambos ao fim da requisição.

---

## Criar uma imobiliária (onboarding assistido)

Enquanto o cadastro aberto não existe (IMOB-26), quem opera o produto cria a imobiliária de cada cliente, sem SQL manual:

1. O cliente se cadastra no Clerk (a tela de login do app) e **verifica o e-mail**.
2. Defina `OPERATOR_API_KEY` no backend (uma chave longa e aleatória; sem ela o endpoint fica desligado) e chame:

```bash
curl -X POST http://localhost:8080/internal/v1/tenants   -H "X-Operator-Key: $OPERATOR_API_KEY" -H "Content-Type: application/json"   -d '{"tenantName": "Imobiliária Exemplo", "adminEmail": "dono@exemplo.com"}'
```

3. O backend cria o tenant (plano `STARTER`) e o usuário `ADMIN` com o `clerk_user_id` real e grava `tenantId` e `role` no `publicMetadata` do Clerk. Retorna `201` (criado) ou `200` (já existia).
4. O cliente precisa **sair e entrar de novo** para receber um token com o metadata novo.

- **Idempotente:** repetir a chamada não duplica nada e re-sincroniza o metadata; se o Clerk falhar (`502`), basta repetir.
- **Usuário que já tem `tenantId` no metadata do Clerk:** o admin é vinculado **àquela** imobiliária (sem criar outra), desde que o `tenantName` corresponda a ela (`409 TENANT_MISMATCH` se não) e o tenant exista (`422 CLERK_TENANT_NOT_FOUND` / `CLERK_TENANT_INVALID` caso contrário).
- **Erros:** `403` chave inválida · `422 CLERK_USER_NOT_FOUND` (sem conta verificada no Clerk) · `409 EMAIL_IN_USE` / `TENANT_SLUG_TAKEN`.
- O vínculo só usa e-mail **verificado** no Clerk, para ninguém assumir o e-mail de outra pessoa.

---

## Isolamento Multi-tenant

Cada imobiliária é um **tenant** isolado. Nenhum dado de um tenant é visível para outro.

- Todas as queries incluem `WHERE tenant_id = ?`
- IDs de outras entidades recebidos no body (`leadId`, `propertyId`, `agentId` em contratos e visitas) são validados contra o tenant da requisição; ID inexistente ou de outro tenant retorna `404`
- O `TenantContext` (ThreadLocal) injeta o `tenantId` automaticamente via interceptor
- Violação de acesso retorna `403 Forbidden`

---

## Módulos e Endpoints

### CRM — Imóveis
```
GET    /api/v1/properties
POST   /api/v1/properties
GET    /api/v1/properties/{id}
PUT    /api/v1/properties/{id}
PATCH  /api/v1/properties/{id}/status
DELETE /api/v1/properties/{id}
POST   /api/v1/properties/{id}/photos
DELETE /api/v1/properties/{id}/photos/{key}
```

### CRM — Leads
```
GET    /api/v1/leads
POST   /api/v1/leads
GET    /api/v1/leads/{id}
PUT    /api/v1/leads/{id}
PATCH  /api/v1/leads/{id}/stage
PATCH  /api/v1/leads/{id}/assign
POST   /api/v1/leads/{id}/properties
DELETE /api/v1/leads/{id}/properties/{propertyId}
```

### CRM — Visitas
```
GET    /api/v1/visits
POST   /api/v1/visits
GET    /api/v1/visits/{id}
PATCH  /api/v1/visits/{id}/status
PATCH  /api/v1/visits/{id}/result
```

### ERP — Contratos
```
GET    /api/v1/contracts
POST   /api/v1/contracts
GET    /api/v1/contracts/{id}
PUT    /api/v1/contracts/{id}
PATCH  /api/v1/contracts/{id}/status
POST   /api/v1/contracts/{id}/document
```

### ERP — Financeiro
```
GET    /api/v1/financial/entries
POST   /api/v1/financial/entries
GET    /api/v1/financial/entries/{id}
PATCH  /api/v1/financial/entries/{id}/pay
PATCH  /api/v1/financial/entries/{id}/cancel
GET    /api/v1/financial/dashboard?month=YYYY-MM
```

`month` é opcional (padrão: mês corrente no fuso de Brasília; formato inválido retorna `400`). A resposta traz `saldoDoMes`, `totalAReceber`, `totalAPagar`, `inadimplencia` e `month`. A **inadimplência é acumulada**: soma as receitas `ATRASADO` com vencimento até o fim do mês consultado, então atrasos de meses anteriores continuam aparecendo. Só ADMIN e FINANCEIRO.

### ERP — Comissões
```
GET    /api/v1/commissions
GET    /api/v1/commissions/{id}
PATCH  /api/v1/commissions/{id}/pay
GET    /api/v1/commissions/report
```

### Usuários
```
GET    /api/v1/users
POST   /api/v1/users/invite
PATCH  /api/v1/users/{id}/role
PATCH  /api/v1/users/{id}/commission-rate
DELETE /api/v1/users/{id}
```

**Convite (`POST /api/v1/users/invite`, só ADMIN):**

- **E-mail sem conta no Clerk:** o backend cria o convite no Clerk (e-mail enviado pelo próprio Clerk) com `tenantId` e `role` no metadata e o link aponta para `<primeira origem de APP_CORS_ORIGIN>/sign-up`. O usuário fica **pendente** (`active=false`, `clerk_user_id = pending:…`) e aparece como "Pendente" na tela de Usuários.
- **Aceite:** no primeiro acesso do convidado, se não há usuário local para o `clerk_user_id`, o filtro procura um convite pendente **do tenant do token** cujo e-mail esteja **verificado** na conta do Clerk; então grava o `clerk_user_id` real e ativa o usuário. E-mail não verificado, e-mail diferente ou tenant diferente seguem sem acesso.
- **E-mail que já tem conta no Clerk:** é vinculado na hora (metadata gravado; a pessoa só precisa sair e entrar de novo). Se o metadata dela já aponta para outra imobiliária, retorna `409`.
- **Erros:** `409 EMAIL_IN_USE` (e-mail já vinculado) · `409 INVITE_REJECTED` (Clerk recusou o convite) · `502 CLERK_UNAVAILABLE` (Clerk fora do ar; o registro local é desfeito, basta repetir).

---

## Migrations

As migrations ficam em `src/main/resources/db/migration/` e seguem o padrão Flyway:

```
V1__create_initial_schema.sql
V2__add_commission_rate_override.sql
```

O Flyway roda automaticamente na inicialização da aplicação.

---

## Testes

```bash
# Rodar todos os testes
./mvnw test

# Rodar testes de um módulo específico
./mvnw test -Dtest="ContractServiceTest"

# Rodar com relatório de cobertura
./mvnw verify
```

Cobertura mínima exigida: **80% nas camadas de service**.

Suíte atual (unitários com Mockito, sem banco): `ClerkJwtAuthenticationFilterTest`, `ClerkJwtClaimsVerifierTest`, `ContractServiceTest`, `VisitServiceTest`, `PropertyServiceTest`. Rode com o JDK 21 (`JAVA_HOME` apontando para ele).

---

## Deploy (Railway)

O deploy é automático via GitHub Actions ao fazer push na branch `main`:

1. GitHub Actions roda `./mvnw verify`
2. Se os testes passarem, build do JAR é gerado
3. Railway detecta o push e faz deploy do JAR
4. Flyway roda as migrations pendentes na inicialização

Health check pós-deploy: `GET /actuator/health`

---

## Convenções de Código

- **Branches:** `feature/nome-da-feature`, `fix/nome-do-bug`
- **Commits:** mensagens em português, prefixo convencional: `feat:`, `fix:`, `refactor:`, `test:`
- **PRs:** sempre para `develop`; `develop` → `main` apenas para releases

---

## Repositórios relacionados

- **Frontend:** [imob-erp-frontend](https://github.com/seu-usuario/imob-erp-frontend)
