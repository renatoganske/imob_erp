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

O projeto segue uma arquitetura de **camadas limpas**, com separação explícita de responsabilidades:

```
Controller   →  entrada HTTP, validação de request, serialização de resposta
Service      →  regras de negócio (sem dependência direta de Spring Data)
Repository   →  interface (porta) — implementada via Spring Data JPA
```

**Regras inegociáveis:**
- `Service` nunca importa repositórios JPA diretamente — apenas interfaces
- `Controller` nunca contém regra de negócio — apenas delega ao `Service`
- Entidades JPA nunca são expostas na API — use DTOs em todos os endpoints
- Conversão entidade ↔ DTO via **MapStruct** (sem mapeamento manual)

---

## Estrutura de Pacotes

```
src/main/java/com/imobcrm/
├── config/
│   ├── SecurityConfig.java          # Spring Security + CORS
│   ├── ClerkJwtConfig.java          # Configuração JWKS do Clerk
│   └── SchedulerConfig.java         # @EnableScheduling
├── tenant/
│   ├── TenantContext.java           # ThreadLocal com tenantId, userId, role
│   └── TenantInterceptor.java       # Extrai e valida tenant a cada request
├── property/                        # Módulo: Imóveis
│   ├── PropertyController.java
│   ├── PropertyService.java
│   ├── PropertyRepository.java
│   ├── Property.java                # Entidade JPA
│   └── dto/
│       ├── PropertyRequestDTO.java
│       └── PropertyResponseDTO.java
├── lead/                            # Módulo CRM: Leads
│   ├── LeadController.java
│   ├── LeadService.java
│   ├── LeadRepository.java
│   ├── Lead.java
│   └── dto/
├── visit/                           # Módulo CRM: Visitas
│   ├── VisitController.java
│   ├── VisitService.java
│   ├── VisitRepository.java
│   ├── Visit.java
│   └── dto/
├── contract/                        # Módulo ERP: Contratos
│   ├── ContractController.java
│   ├── ContractService.java
│   ├── ContractRepository.java
│   ├── Contract.java
│   └── dto/
├── financial/                       # Módulo ERP: Financeiro
│   ├── FinancialController.java
│   ├── FinancialService.java
│   ├── FinancialRepository.java
│   ├── FinancialEntry.java
│   ├── FinancialScheduler.java      # Job diário de atualização de status
│   └── dto/
├── commission/                      # Módulo ERP: Comissões
│   ├── CommissionController.java
│   ├── CommissionService.java
│   ├── CommissionRepository.java
│   ├── Commission.java
│   └── dto/
├── user/                            # Usuários e corretores
│   ├── UserController.java
│   ├── UserService.java
│   ├── UserRepository.java
│   ├── User.java
│   └── dto/
├── storage/
│   └── R2StorageService.java        # Upload/remoção de arquivos no R2
└── shared/
    ├── exception/                   # GlobalExceptionHandler, exceções customizadas
    ├── pagination/                  # PageResponse<T> padrão
    └── audit/                       # createdAt, updatedAt automáticos
src/main/resources/
├── application.yml
├── application-dev.yml
├── application-prod.yml
└── db/migration/
    └── V1__create_initial_schema.sql
```

---

## Pré-requisitos

- Java 21+
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

# Cloudflare R2
R2_ACCOUNT_ID=
R2_ACCESS_KEY_ID=
R2_SECRET_ACCESS_KEY=
R2_BUCKET_NAME=imobcrm-dev
R2_PUBLIC_URL=https://pub-xxx.r2.dev
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
| `R2_ACCOUNT_ID` | ID da conta Cloudflare | ✅ |
| `R2_ACCESS_KEY_ID` | Access Key do R2 | ✅ |
| `R2_SECRET_ACCESS_KEY` | Secret Key do R2 | ✅ |
| `R2_BUCKET_NAME` | Nome do bucket R2 | ✅ |
| `R2_PUBLIC_URL` | URL pública do bucket R2 | ✅ |

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

O `TenantInterceptor` extrai e valida esses valores a cada requisição.

---

## Isolamento Multi-tenant

Cada imobiliária é um **tenant** isolado. Nenhum dado de um tenant é visível para outro.

- Todas as queries incluem `WHERE tenant_id = ?`
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
GET    /api/v1/financial/dashboard
```

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
