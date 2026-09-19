# SPECS — ERP para Pequenas Imobiliárias
**Versão:** 0.2 (MVP)  
**Status:** Rascunho  
**Última atualização:** Março 2026  
**Changelog:** v0.2 — Turso substituído por PostgreSQL; arquitetura hexagonal descartada em favor de camadas limpas; seção de arquitetura adicionada

---

## 1. Stack Tecnológica

| Camada | Tecnologia | Justificativa |
|---|---|---|
| Backend | Spring Boot 3.x (Java 21) | Preferência definida; ecossistema maduro |
| Frontend | Next.js 14 (App Router) + TypeScript | SSR para SEO futuro; ecossistema React |
| Estilização | Tailwind CSS + shadcn/ui | Velocidade de desenvolvimento |
| Banco de dados | PostgreSQL (Railway) | Suporte nativo Spring Data JPA; confiável |
| ORM | Spring Data JPA + Flyway | Integração nativa; migrations versionadas |
| Autenticação | Clerk | SDK Java oficial; free tier até 50k usuários + 100 orgs |
| Storage de arquivos | Cloudflare R2 | Free tier generoso (10 GB); sem custo de egress |
| Hospedagem backend | Railway | Hobby $5/mês; deploy via Git; PostgreSQL incluso |
| Hospedagem frontend | Vercel | Free tier; integração nativa Next.js |
| Reverse proxy | Caddy (fase 2) | SSL automático para sites das imobiliárias |

---

## 2. Decisões de Arquitetura

### Padrão adotado: Camadas Limpas (não hexagonal)
Arquitetura hexagonal foi avaliada e descartada para o MVP — o custo de boilerplate não justifica o benefício nesse estágio. O padrão adotado é **camadas limpas com separação explícita**, que entrega 80% dos benefícios com 20% do custo:

```
Controller       → entrada HTTP, validação de request, serialização
Service          → regras de negócio, sem dependência direta de JPA
Repository       → interface (porta), sem lógica de negócio
JPA Repository   → implementação Spring Data (adaptador)
```

**Regras inegociáveis:**
- `Service` nunca importa classes do Spring Data diretamente — apenas interfaces de repositório
- `Controller` nunca contém lógica de negócio — apenas delega para `Service`
- Entidades JPA nunca saem do backend — `DTOs` são usados em todos os endpoints
- `MapStruct` para conversão entre entidade ↔ DTO (sem mapeamento manual)

**Migração futura:** a separação já existente facilita migração para hexagonal quando a equipe crescer ou a complexidade justificar.

---

## 3. Modelagem de Dados

```
Tenant (Imobiliária)
├── id: UUID
├── name: String
├── slug: String (único)
├── plan: Enum (STARTER, PRO, PRO_SITE)
├── createdAt: DateTime
└── active: Boolean

User (Corretor / Admin / Financeiro)
├── id: UUID
├── tenantId: UUID (FK → Tenant)
├── clerkUserId: String
├── name: String
├── email: String
├── role: Enum (ADMIN, CORRETOR, FINANCEIRO)
├── commissionRate: Decimal (% padrão de comissão)
└── active: Boolean

Property (Imóvel)
├── id: UUID
├── tenantId: UUID (FK → Tenant)
├── type: Enum (CASA, APARTAMENTO, COMERCIAL, TERRENO)
├── title: String
├── description: Text
├── address: String
├── neighborhood: String
├── city: String
├── price: Decimal
├── area: Decimal (m²)
├── bedrooms: Integer
├── bathrooms: Integer
├── parkingSpots: Integer
├── status: Enum (DISPONIVEL, RESERVADO, VENDIDO, ALUGADO)
├── purpose: Enum (VENDA, ALUGUEL, AMBOS)
├── photos: String[] (URLs no R2)
├── createdAt: DateTime
└── updatedAt: DateTime

Lead (CRM)
├── id: UUID
├── tenantId: UUID (FK → Tenant)
├── assignedTo: UUID (FK → User)
├── name: String
├── phone: String
├── email: String (nullable)
├── source: Enum (WHATSAPP, SITE, INDICACAO, PORTAL_ZAP, PORTAL_VIVAREAL, OUTRO)
├── stage: Enum (NOVO, EM_ATENDIMENTO, VISITA_AGENDADA, PROPOSTA, FECHADO, PERDIDO)
├── notes: Text (nullable)
├── createdAt: DateTime
└── updatedAt: DateTime

LeadProperty (Lead ↔ Imóvel de interesse)
├── leadId: UUID (FK → Lead)
└── propertyId: UUID (FK → Property)

Visit (Visita — CRM)
├── id: UUID
├── tenantId: UUID (FK → Tenant)
├── leadId: UUID (FK → Lead)
├── propertyId: UUID (FK → Property)
├── agentId: UUID (FK → User)
├── scheduledAt: DateTime
├── status: Enum (AGENDADA, REALIZADA, CANCELADA)
├── result: Text (nullable)
└── createdAt: DateTime

Contract (Contrato — ERP)
├── id: UUID
├── tenantId: UUID (FK → Tenant)
├── leadId: UUID (FK → Lead, nullable — pode existir sem lead no CRM)
├── propertyId: UUID (FK → Property)
├── agentId: UUID (FK → User)
├── type: Enum (COMPRA_VENDA, LOCACAO)
├── status: Enum (RASCUNHO, ATIVO, ENCERRADO, CANCELADO)
├── value: Decimal (valor total ou mensal)
├── signedAt: Date (nullable)
├── startDate: Date
├── endDate: Date (nullable — para locação)
├── adjustmentIndex: Enum (IGPM, IPCA, FIXO, nullable)
├── buyerName: String (nome do comprador/locatário)
├── buyerDocument: String (CPF/CNPJ)
├── ownerName: String (nome do proprietário)
├── ownerDocument: String (CPF/CNPJ)
├── documentUrl: String (URL do PDF no R2, nullable)
├── notes: Text (nullable)
├── createdAt: DateTime
└── updatedAt: DateTime

FinancialEntry (Lançamento Financeiro — ERP)
├── id: UUID
├── tenantId: UUID (FK → Tenant)
├── contractId: UUID (FK → Contract, nullable)
├── type: Enum (RECEITA, DESPESA)
├── category: Enum (ALUGUEL, PARCELA_VENDA, TAXA_ADMINISTRACAO, REPASSE_PROPRIETARIO, COMISSAO, DESPESA_OPERACIONAL, OUTRO)
├── description: String
├── value: Decimal
├── dueDate: Date
├── paidAt: Date (nullable)
├── status: Enum (PENDENTE, PAGO, ATRASADO, CANCELADO)
├── recurrent: Boolean
└── createdAt: DateTime

Commission (Comissão — ERP)
├── id: UUID
├── tenantId: UUID (FK → Tenant)
├── contractId: UUID (FK → Contract)
├── agentId: UUID (FK → User)
├── rate: Decimal (% aplicado)
├── baseValue: Decimal (valor base do cálculo)
├── value: Decimal (valor calculado)
├── status: Enum (PENDENTE, PAGO, PARCELADO)
├── paidAt: Date (nullable)
└── createdAt: DateTime
```

---

## 4. Arquitetura de Execução

### Isolamento multi-tenant
- `tenantId` extraído do JWT do Clerk (`publicMetadata`) a cada requisição
- Todas as queries incluem `WHERE tenant_id = ?` — sem exceção
- `TenantInterceptor` Spring valida `tenantId` antes de qualquer controller
- Isolamento garantido na camada de aplicação
- IDs referenciados no body (leadId, propertyId, agentId) são validados contra o tenant da requisição (404 se não pertencerem)

### Fluxo de autenticação
```
1. Usuário faz login via Clerk (frontend)
2. Clerk retorna JWT com userId + tenantId + role em publicMetadata
3. Frontend envia JWT no header Authorization: Bearer <token>
4. Spring verifica assinatura via Clerk JWKS endpoint, expiração, `iss` (CLERK_ISSUER) e `azp` (origens autorizadas, se presente)
5. Extrai tenantId e role de publicMetadata; resolve o userId local (users.id) por clerk_user_id
6. Rejeita usuário inexistente, inativo ou de tenant diferente do token
7. Injeta no contexto da requisição via ThreadLocal (TenantContext)
```

### Fluxo de integração CRM → ERP
```
1. Corretor move lead para "Fechado" no CRM
2. Sistema cria rascunho de Contrato com dados do lead + imóvel
3. Admin/Financeiro revisa e ativa o contrato
4. Ao ativar:
   a. Status do imóvel é atualizado (Vendido ou Alugado)
   b. Parcelas financeiras são geradas automaticamente
   c. Comissão do corretor é calculada e registrada
```

### Estrutura de pacotes (backend)
```
com.imobcrm
├── config/           # Spring Security, Clerk JWT (filtro + verificador de claims), CORS, logging
├── tenant/           # TenantContext, TenantInterceptor
├── storage/          # R2StorageService
├── shared/           # exception/, pagination/, audit/, dto/
└── {property, lead, visit, contract, financial, commission, user}/
    ├── api/          # Controller + DTOs (XRequest / XResponse)
    ├── domain/       # Entidade, Service, interface XRepository, enums/
    └── infra/        # JpaXRepository, XMapper (MapStruct)
```
Detalhes e regras de dependência entre camadas: `docs/architecture.md`.

### Estrutura de pastas (frontend)
```
app/
├── (auth)/
├── (dashboard)/
│   ├── imoveis/
│   ├── leads/           # Kanban + lista
│   ├── visitas/
│   ├── contratos/
│   ├── financeiro/
│   │   ├── receber/
│   │   └── pagar/
│   ├── comissoes/
│   └── configuracoes/
└── layout.tsx
components/
├── ui/                  # shadcn/ui base
├── imoveis/
├── leads/
├── contratos/
├── financeiro/
└── comissoes/
```

---

## 5. API REST

### Convenções
- Base URL: `/api/v1`
- Autenticação: `Authorization: Bearer <clerk_jwt>` em todos endpoints
- Paginação: `?page=0&size=20`
- Erro: `{ "error": "mensagem", "code": "CODIGO" }`

### Endpoints — CRM

#### Imóveis
```
GET    /api/v1/properties
POST   /api/v1/properties
GET    /api/v1/properties/{id}
PUT    /api/v1/properties/{id}
PATCH  /api/v1/properties/{id}/status
DELETE /api/v1/properties/{id}           # soft delete
POST   /api/v1/properties/{id}/photos
DELETE /api/v1/properties/{id}/photos/{key}
```

#### Leads
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

#### Visitas
```
GET    /api/v1/visits
POST   /api/v1/visits
GET    /api/v1/visits/{id}
PATCH  /api/v1/visits/{id}/status
PATCH  /api/v1/visits/{id}/result
```

### Endpoints — ERP

#### Contratos
```
GET    /api/v1/contracts
POST   /api/v1/contracts
GET    /api/v1/contracts/{id}
PUT    /api/v1/contracts/{id}
PATCH  /api/v1/contracts/{id}/status     # ativa, encerra, cancela
POST   /api/v1/contracts/{id}/document   # upload PDF
```

#### Financeiro
```
GET    /api/v1/financial/entries         # lista com filtros (type, status, period)
POST   /api/v1/financial/entries         # lançamento manual
GET    /api/v1/financial/entries/{id}
PATCH  /api/v1/financial/entries/{id}/pay
PATCH  /api/v1/financial/entries/{id}/cancel
GET    /api/v1/financial/dashboard       # saldo, a receber, a pagar, inadimplência
```

#### Comissões
```
GET    /api/v1/commissions               # lista com filtros (agentId, status, period)
GET    /api/v1/commissions/{id}
PATCH  /api/v1/commissions/{id}/pay
GET    /api/v1/commissions/report        # relatório por corretor no período
```

#### Usuários
```
GET    /api/v1/users
POST   /api/v1/users/invite
PATCH  /api/v1/users/{id}/role
PATCH  /api/v1/users/{id}/commission-rate
DELETE /api/v1/users/{id}
```

---

## 6. Regras de Negócio Críticas

### Isolamento
- **RN-01:** Todo endpoint valida `tenantId` do recurso antes de retornar ou modificar. Violação → `403 Forbidden`.

### Integração CRM → ERP
- **RN-02:** Ao mover lead para "Fechado", sistema cria automaticamente rascunho de contrato. Admin deve ativar manualmente.
- **RN-03:** Ao ativar contrato, sistema gera parcelas financeiras e comissão. Ação irreversível sem cancelamento explícito.
- **RN-04:** Imóvel com contrato ativo não pode ter status alterado manualmente — apenas via fluxo de contrato.

### Financeiro
- **RN-05:** Parcelas de aluguel são geradas para os próximos 12 meses ao ativar contrato de locação. Novas parcelas geradas automaticamente a cada mês.
- **RN-06:** Status "Atrasado" é atualizado automaticamente por job diário (Spring Scheduler) para entradas com `dueDate` passado e status "Pendente".
- **RN-07:** Lançamento pago não pode ser editado — apenas cancelado (com estorno manual).

### Comissões
- **RN-08:** Comissão calculada automaticamente: `contract.value * agent.commissionRate / 100`.
- **RN-09:** Taxa de comissão do corretor pode ser sobrescrita no contrato (campo opcional).

### Imóveis
- **RN-10:** Soft delete — imóvel nunca apagado do banco. Contratos associados mantêm histórico.
- **RN-11:** Máximo 20 fotos por imóvel. JPG/PNG/WebP. Máximo 10MB por arquivo.

### Permissões
| Ação | Admin | Corretor | Financeiro |
|---|---|---|---|
| Cadastrar imóvel | ✅ | ✅ | ❌ |
| Excluir imóvel | ✅ | ❌ | ❌ |
| Ver todos os leads | ✅ | ❌ (só seus) | ❌ |
| Criar contrato | ✅ | ❌ | ✅ |
| Ativar contrato | ✅ | ❌ | ✅ |
| Ver financeiro | ✅ | ❌ | ✅ |
| Lançar financeiro | ✅ | ❌ | ✅ |
| Ver comissões | ✅ | ✅ (só suas) | ✅ |
| Pagar comissão | ✅ | ❌ | ✅ |
| Convidar usuário | ✅ | ❌ | ❌ |

---

## 7. Integrações

### Clerk
- SDK: `clerk-sdk-java` (oficial, Maven)
- `publicMetadata`: `{ "tenantId": "uuid", "role": "ADMIN|CORRETOR|FINANCEIRO" }`
- `CLERK_ISSUER` (obrigatória): Frontend API URL do Clerk, validada contra o claim `iss`
- `CLERK_AUTHORIZED_PARTIES` (opcional): origens aceitas no claim `azp` (padrão: `APP_CORS_ORIGIN`)
- Webhook: sincroniza criação/remoção de usuário com tabela local (**pendente** — a autenticação exige que o usuário exista em `users`)

### Cloudflare R2
- SDK: AWS S3 Java SDK (R2 é compatível)
- Chave de arquivo (fotos): `{tenantId}/properties/{propertyId}/{uuid}.{ext}`
- Chave de arquivo (contratos): `{tenantId}/contracts/{contractId}/{uuid}.pdf`

### PostgreSQL (Railway)
- Driver: `spring-boot-starter-data-jpa` + `postgresql`
- Migrations: Flyway (`db/migration/V{n}__{descricao}.sql`)
- Connection pool: HikariCP (padrão Spring Boot)

---

## 8. Segurança
- JWT validado via Clerk JWKS a cada requisição: assinatura, exp/nbf, `iss` e `azp`
- Usuário do token deve existir, estar ativo e pertencer ao mesmo tenant do token
- IDs de relacionamento vindos no body são checados contra o tenant (evita referência cruzada entre imobiliárias)
- HTTPS obrigatório (Railway provisiona automaticamente)
- Secrets via env vars — nunca no código
- Validação de tipo MIME no upload (não apenas extensão)
- Rate limiting: 100 req/min por IP
- CORS: apenas domínio do frontend

---

## 9. Observabilidade
- Logs JSON via Logback (Railway coleta automaticamente)
- Erro 5xx: log com stack trace + tenantId + userId (sem dados pessoais)
- Health check: `GET /actuator/health`
- Job financeiro: log de execução com quantidade de registros atualizados

---

## 10. Variáveis de Ambiente

### Backend
```
CLERK_SECRET_KEY=
CLERK_JWKS_URL=
CLERK_ISSUER=
SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:<port>/<db>
SPRING_DATASOURCE_USERNAME=
SPRING_DATASOURCE_PASSWORD=
R2_ACCOUNT_ID=
R2_ACCESS_KEY_ID=
R2_SECRET_ACCESS_KEY=
R2_BUCKET_NAME=
R2_PUBLIC_URL=
```

### Frontend
```
NEXT_PUBLIC_CLERK_PUBLISHABLE_KEY=
CLERK_SECRET_KEY=
NEXT_PUBLIC_API_URL=
```

---

## 11. Estimativa de Esforço (dev sênior solo)

| Módulo | Estimativa |
|---|---|
| Setup infra + autenticação + multi-tenant | 3 dias |
| Gestão de imóveis (CRUD + fotos) | 5 dias |
| CRM — Pipeline de leads + visitas | 5 dias |
| ERP — Contratos | 5 dias |
| ERP — Financeiro (lançamentos + geração automática) | 6 dias |
| ERP — Comissões | 3 dias |
| Integração CRM → ERP | 3 dias |
| Frontend — todas as telas | 15 dias |
| Testes + ajustes + deploy | 5 dias |
| **Total estimado** | **~50 dias úteis (10 semanas)** |

---

## 12. Estimativa de Custos de Infraestrutura

| Fase | Clientes | Custo infra/mês |
|---|---|---|
| Desenvolvimento | 0 | ~R$ 35 |
| Beta | 1–5 imobiliárias | ~R$ 50–80 |
| Produto no ar | 10–20 imobiliárias | ~R$ 100–180 |
| Escala | 50+ imobiliárias | ~R$ 300–500 |

---

## 13. Critérios de Qualidade para Entrega

- [ ] Todos os endpoints cobertos por testes de integração
- [ ] Nenhum dado de um tenant visível para outro (teste de isolamento)
- [ ] Job financeiro validado (atualização de status "Atrasado")
- [ ] Fluxo completo lead → contrato → financeiro → comissão testado end-to-end
- [ ] Upload de foto e PDF funcionando em mobile
- [ ] Kanban e dashboard financeiro funcionando em 375px
- [ ] Tempo de resposta < 500ms para listagens com 100+ registros
- [ ] Deploy zero-downtime configurado no Railway
