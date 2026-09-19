# Arquitetura de Pacotes — Backend Spring Boot
**Projeto:** ERP Imobiliário MVP  
**Stack:** Spring Boot 3.x · Java 21 · PostgreSQL · Clerk · Cloudflare R2  
**Decisão:** Camadas Limpas por Feature (Clean Layered Package-by-Feature)  
**Data:** Setembro 2026

---

## 1. Padrão Adotado

**Package-by-feature com três sub-camadas internas: `api/`, `domain/`, `infra/`.**

Cada módulo de negócio é auto-contido. As dependências entre camadas fluem em uma única direção:

```
api/ → domain/ → infra/
```

- `api/` conhece `domain/`, mas `domain/` **nunca** conhece `api/`
- `domain/` conhece apenas a interface do repositório que ele mesmo define
- `infra/` implementa a interface definida em `domain/`
- `shared/` e `tenant/` são transversais — qualquer camada pode importá-los

---

## 2. Estrutura Completa de Pacotes

```
com.imobcrm
│
├── config/                          # Configurações globais do Spring
│   ├── SecurityConfig.java          # Spring Security + Clerk JWT filter
│   ├── ClerkJwtConfig.java          # Bean de validação JWT via JWKS
│   ├── WebMvcConfig.java            # Registro do TenantInterceptor + CORS
│   └── SchedulerConfig.java         # Habilita @EnableScheduling
│
├── tenant/                          # Isolamento multi-tenant (cross-cutting)
│   ├── TenantContext.java           # ThreadLocal com tenantId da requisição
│   └── TenantInterceptor.java       # HandlerInterceptor: extrai e valida tenantId do JWT
│
├── shared/                          # Utilitários e contratos compartilhados
│   ├── exception/
│   │   ├── BusinessException.java   # Exceção de domínio base (runtime)
│   │   ├── ResourceNotFoundException.java
│   │   ├── ForbiddenException.java
│   │   └── GlobalExceptionHandler.java  # @RestControllerAdvice → mapeia exceções para HTTP
│   ├── pagination/
│   │   └── PageResponse.java        # Wrapper genérico de resposta paginada
│   ├── audit/
│   │   └── AuditEntity.java         # @MappedSuperclass com createdAt + updatedAt
│   └── security/
│       └── RequiresRole.java        # Anotação customizada para RBAC nos controllers
│
├── storage/                         # Integração com Cloudflare R2
│   ├── R2StorageService.java        # uploadFile(), deleteFile(), generateKey()
│   └── StorageProperties.java       # @ConfigurationProperties para env vars do R2
│
│   ── ── ── MÓDULOS DE NEGÓCIO ── ── ──
│
├── property/                        # Módulo: Imóveis
│   ├── api/
│   │   ├── PropertyController.java  # @RestController — entrada HTTP, validação, delegação
│   │   ├── PropertyRequest.java     # DTO de entrada (criação/edição)
│   │   ├── PropertyStatusRequest.java  # DTO para PATCH /status
│   │   └── PropertyResponse.java   # DTO de saída
│   ├── domain/
│   │   ├── Property.java            # Entidade JPA + regras de estado simples
│   │   ├── PropertyService.java     # Regras de negócio (sem import de Spring Data)
│   │   ├── PropertyRepository.java  # Interface (porta de saída) — sem extends JpaRepository
│   │   └── enums/
│   │       ├── PropertyType.java    # CASA, APARTAMENTO, COMERCIAL, TERRENO
│   │       ├── PropertyStatus.java  # DISPONIVEL, RESERVADO, VENDIDO, ALUGADO
│   │       └── PropertyPurpose.java # VENDA, ALUGUEL, AMBOS
│   └── infra/
│       ├── JpaPropertyRepository.java  # extends JpaRepository — implementação da porta
│       └── PropertyMapper.java         # @Mapper MapStruct: Property ↔ DTO
│
├── user/                            # Módulo: Usuários e Corretores
│   ├── api/
│   │   ├── UserController.java
│   │   ├── UserInviteRequest.java
│   │   ├── UserRoleRequest.java
│   │   └── UserResponse.java
│   ├── domain/
│   │   ├── User.java
│   │   ├── UserService.java
│   │   ├── UserRepository.java      # interface
│   │   └── enums/
│   │       └── Role.java            # ADMIN, CORRETOR, FINANCEIRO
│   └── infra/
│       ├── JpaUserRepository.java
│       └── UserMapper.java
│
├── lead/                            # Módulo: Leads (CRM)
│   ├── api/
│   │   ├── LeadController.java
│   │   ├── LeadRequest.java
│   │   ├── LeadStageRequest.java    # DTO para PATCH /stage
│   │   ├── LeadAssignRequest.java   # DTO para PATCH /assign
│   │   └── LeadResponse.java
│   ├── domain/
│   │   ├── Lead.java
│   │   ├── LeadProperty.java        # Entidade de junção Lead ↔ Property
│   │   ├── LeadService.java         # dispara ContractService.createDraftFromLead() ao fechar
│   │   ├── LeadRepository.java      # interface
│   │   └── enums/
│   │       ├── LeadStage.java       # NOVO, EM_ATENDIMENTO, VISITA_AGENDADA, PROPOSTA, FECHADO, PERDIDO
│   │       └── LeadSource.java      # WHATSAPP, SITE, INDICACAO, PORTAL_ZAP, etc.
│   └── infra/
│       ├── JpaLeadRepository.java
│       ├── JpaLeadPropertyRepository.java
│       └── LeadMapper.java
│
├── visit/                           # Módulo: Visitas (CRM)
│   ├── api/
│   │   ├── VisitController.java
│   │   ├── VisitRequest.java
│   │   ├── VisitResultRequest.java  # DTO para PATCH /result
│   │   ├── VisitStatusRequest.java  # DTO para PATCH /status
│   │   └── VisitResponse.java
│   ├── domain/
│   │   ├── Visit.java
│   │   ├── VisitService.java
│   │   ├── VisitRepository.java     # interface
│   │   └── enums/
│   │       └── VisitStatus.java     # AGENDADA, REALIZADA, CANCELADA
│   └── infra/
│       ├── JpaVisitRepository.java
│       └── VisitMapper.java
│
├── contract/                        # Módulo: Contratos (ERP) — núcleo do fluxo
│   ├── api/
│   │   ├── ContractController.java
│   │   ├── ContractRequest.java
│   │   ├── ContractStatusRequest.java  # DTO para PATCH /status
│   │   └── ContractResponse.java
│   ├── domain/
│   │   ├── Contract.java
│   │   ├── ContractService.java        # activate() é @Transactional: atualiza imóvel + parcelas + comissão
│   │   ├── ContractRepository.java     # interface
│   │   └── enums/
│   │       ├── ContractType.java       # COMPRA_VENDA, LOCACAO
│   │       ├── ContractStatus.java     # RASCUNHO, ATIVO, ENCERRADO, CANCELADO
│   │       └── AdjustmentIndex.java    # IGPM, IPCA, FIXO
│   └── infra/
│       ├── JpaContractRepository.java
│       └── ContractMapper.java
│
├── financial/                       # Módulo: Financeiro (ERP)
│   ├── api/
│   │   ├── FinancialController.java
│   │   ├── FinancialEntryRequest.java
│   │   ├── FinancialEntryResponse.java
│   │   └── FinancialDashboardResponse.java
│   ├── domain/
│   │   ├── FinancialEntry.java
│   │   ├── FinancialService.java        # generateInstallments(), markAsPaid(), markOverdue()
│   │   ├── OverdueJob.java              # @Scheduled — job diário às 6h BRT
│   │   ├── FinancialRepository.java     # interface
│   │   └── enums/
│   │       ├── EntryType.java           # RECEITA, DESPESA
│   │       ├── EntryCategory.java       # ALUGUEL, PARCELA_VENDA, COMISSAO, etc.
│   │       └── EntryStatus.java         # PENDENTE, PAGO, ATRASADO, CANCELADO
│   └── infra/
│       ├── JpaFinancialRepository.java
│       └── FinancialMapper.java
│
└── commission/                      # Módulo: Comissões (ERP)
    ├── api/
    │   ├── CommissionController.java
    │   ├── CommissionResponse.java
    │   └── CommissionReportResponse.java
    ├── domain/
    │   ├── Commission.java
    │   ├── CommissionService.java       # calculate() chamado por ContractService.activate()
    │   ├── CommissionRepository.java    # interface
    │   └── enums/
    │       └── CommissionStatus.java    # PENDENTE, PAGO, PARCELADO
    └── infra/
        ├── JpaCommissionRepository.java
        └── CommissionMapper.java
```

---

## 3. Regras Invioláveis (para o Claude Code seguir)

### 3.1 Fluxo de dependências
```
Controller (api/)  →  Service (domain/)  →  Repository interface (domain/)
                                                      ↑
                                         JpaRepository (infra/) implementa
```

### 3.2 O que cada camada PODE e NÃO PODE importar

| Camada | Pode importar | Nunca importar |
|---|---|---|
| `api/` | `domain/`, `shared/`, `tenant/` | `infra/` diretamente |
| `domain/` | `shared/`, `tenant/`, enums do próprio módulo | `org.springframework.data.*`, `jakarta.persistence.*` (salvo a entidade) |
| `infra/` | `domain/`, `org.springframework.data.*`, `jakarta.persistence.*` | `api/` |
| `shared/` | Apenas Java padrão + Spring MVC para o handler | Nenhum módulo de negócio |

> **Exceção pragmática para MVP:** A entidade JPA (`Property.java`, `Lead.java`, etc.) fica em `domain/` com anotações `@Entity`. Isso evita mapeamento duplo. Se o projeto crescer, migra as entidades JPA para `infra/` e cria objetos de domínio puros em `domain/`.

### 3.3 Service nunca importa JpaRepository diretamente
```java
// ✅ CORRETO — domain depende da interface que ele mesmo define
@Service
public class PropertyService {
    private final PropertyRepository repository; // interface em domain/
}

// ❌ ERRADO — domain não pode conhecer a implementação Spring Data
@Service
public class PropertyService {
    private final JpaPropertyRepository repository; // infra/ — proibido
}
```

### 3.4 Controller nunca contém lógica de negócio
```java
// ✅ CORRETO
@PostMapping
public ResponseEntity<PropertyResponse> create(@Valid @RequestBody PropertyRequest request) {
    return ResponseEntity.status(201).body(propertyService.create(request));
}

// ❌ ERRADO — lógica de negócio no controller
@PostMapping
public ResponseEntity<PropertyResponse> create(@RequestBody PropertyRequest request) {
    if (request.getPrice() <= 0) throw new RuntimeException("...");  // vai pro Service
    property.setTenantId(TenantContext.getTenantId());               // vai pro Service
    return ResponseEntity.ok(mapper.toResponse(repository.save(property)));
}
```

### 3.5 tenantId nunca vem do body da requisição
```java
// ✅ CORRETO — sempre do TenantContext (extraído do JWT)
public PropertyResponse create(PropertyRequest request) {
    UUID tenantId = TenantContext.getTenantId();
    Property property = mapper.toEntity(request);
    property.setTenantId(tenantId);
    return mapper.toResponse(repository.save(property));
}
```

### 3.6 IDs do body sempre validados contra o tenant
Todo `leadId`/`propertyId`/`agentId` recebido em request deve ser conferido com `existsByIdAndTenantId...` antes de persistir; a FK do banco só garante existência, não o tenant. Falha: `ResourceNotFoundException` (404).

### 3.7 DTOs nunca saem do módulo `api/`
- `PropertyRequest` e `PropertyResponse` são usados apenas pelo controller e pelo service como parâmetro/retorno
- A entidade `Property` nunca é retornada diretamente por um endpoint

---

### Desvios conscientes do MVP (estado atual do código)
- `domain/` importa DTOs de `api/` (services retornam DTOs), o mapper de `infra/` e `Page`/`Pageable` do Spring Data
- Services de `lead` e `contract` usam repositórios de outros módulos (`PropertyRepository`, `UserRepository`, `LeadRepository`) em vez de chamar seus services
- Não implementados: `RequiresRole`, `ClerkJwtConfig`, `SchedulerConfig` (o `@EnableScheduling` está em `ImobErpApplication`), entidade `LeadProperty` (o `Lead` usa `@ManyToMany`)
- Autenticação: `ClerkJwtAuthenticationFilter` resolve `users.id` por `clerk_user_id` e valida `iss`/`azp` via `ClerkJwtClaimsVerifier`

---

## 4. Padrão de Nomenclatura

| Artefato | Convenção | Exemplo |
|---|---|---|
| Controller | `{Módulo}Controller` | `PropertyController` |
| Service | `{Módulo}Service` | `PropertyService` |
| Repository (interface) | `{Módulo}Repository` | `PropertyRepository` |
| Repository (impl) | `Jpa{Módulo}Repository` | `JpaPropertyRepository` |
| Entidade JPA | `{Módulo}` (sem sufixo) | `Property` |
| DTO entrada | `{Módulo}Request` | `PropertyRequest` |
| DTO saída | `{Módulo}Response` | `PropertyResponse` |
| DTO parcial | `{Módulo}{Ação}Request` | `PropertyStatusRequest` |
| Mapper | `{Módulo}Mapper` | `PropertyMapper` |
| Enum | PascalCase, plural quando lista de valores | `PropertyStatus`, `Role` |

---

## 5. Fluxo crítico: ativação de contrato

O método mais importante do sistema. Deve ser `@Transactional` e orquestrar três efeitos colaterais:

```java
// ContractService.java (domain/)
@Transactional
public ContractResponse activate(UUID contractId) {
    Contract contract = repository.findByIdAndTenantId(contractId, TenantContext.getTenantId())
        .orElseThrow(ResourceNotFoundException::new);

    if (contract.getStatus() != ContractStatus.RASCUNHO) {
        throw new BusinessException("Contrato já foi ativado ou cancelado");
    }

    // 1. Ativar o contrato
    contract.setStatus(ContractStatus.ATIVO);
    contract.setUpdatedAt(LocalDateTime.now());
    repository.save(contract);

    // 2. Atualizar status do imóvel
    propertyService.updateStatusFromContract(contract);

    // 3. Gerar parcelas financeiras
    financialService.generateInstallments(contract);

    // 4. Calcular e registrar comissão
    commissionService.calculate(contract);

    return mapper.toResponse(contract);
}
```

> Se qualquer etapa falhar, o `@Transactional` garante rollback de tudo.

---

## 6. Variáveis de ambiente esperadas

```bash
# Clerk
CLERK_SECRET_KEY=
CLERK_JWKS_URL=https://<frontend-api>.clerk.accounts.dev/.well-known/jwks.json
CLERK_ISSUER=https://<frontend-api>.clerk.accounts.dev   # obrigatório (fail-fast); valida o claim iss
# CLERK_AUTHORIZED_PARTIES=http://localhost:3000        # opcional; valida azp (padrão: APP_CORS_ORIGIN)

# Banco de dados (Railway)
SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:<port>/<db>
SPRING_DATASOURCE_USERNAME=
SPRING_DATASOURCE_PASSWORD=

# Cloudflare R2
R2_ACCOUNT_ID=
R2_ACCESS_KEY_ID=
R2_SECRET_ACCESS_KEY=
R2_BUCKET_NAME=
R2_PUBLIC_URL=
```

---

## 7. Dependências Maven relevantes

```xml
<!-- Web -->
<dependency>spring-boot-starter-web</dependency>

<!-- Banco -->
<dependency>spring-boot-starter-data-jpa</dependency>
<dependency>postgresql</dependency>
<dependency>flyway-core</dependency>

<!-- Segurança / Auth -->
<dependency>spring-boot-starter-security</dependency>
<dependency>spring-security-oauth2-resource-server</dependency>
<dependency>clerk-sdk-java</dependency>

<!-- Mapeamento -->
<dependency>mapstruct</dependency>
<dependency>lombok</dependency>
<dependency>lombok-mapstruct-binding</dependency>

<!-- Observabilidade -->
<dependency>spring-boot-starter-actuator</dependency>

<!-- Storage (R2 via SDK S3) -->
<dependency>software.amazon.awssdk:s3</dependency>

<!-- Testes -->
<dependency>spring-boot-starter-test</dependency>
```

---

## 8. O que NÃO fazer (anti-padrões a evitar)

| Anti-padrão | Por quê evitar |
|---|---|
| `@Autowired` em campo | Dificulta testes; use injeção via construtor |
| Lógica de negócio no Controller | Viola SRP; torna o controller não-testável unitariamente |
| Retornar entidade JPA no endpoint | Expõe detalhes de infra; risco de serialização lazy/infinita |
| `tenantId` no body da requisição | Vulnerabilidade de segurança; sempre extrair do JWT |
| `Service` importando `JpaRepository` | Quebra o isolamento domain/infra |
| Lançar `RuntimeException` genérica | Sempre usar exceções de domínio específicas (`BusinessException`) |
| `static` no `TenantContext` sem ThreadLocal | Race condition em ambiente multi-threaded |
