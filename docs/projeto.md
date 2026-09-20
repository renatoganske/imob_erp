# Imob ERP — Documentação do projeto

Guia para quem vai desenvolver, rodar ou dar manutenção no sistema. O que cada módulo faz está em [`funcionalidades.md`](funcionalidades.md); a estrutura de pacotes e as regras de camada estão em [`architecture.md`](architecture.md) (não repetidas aqui).

## 1. Visão do produto

ERP + CRM para pequenas imobiliárias (2 a 10 corretores), em modelo SaaS **multi-tenant**: cada imobiliária é um tenant, com seus dados isolados.

Fluxo principal: **Lead → fechamento → contrato → parcelas financeiras → comissão do corretor.**

Módulos: imóveis (com fotos), leads (kanban), visitas, contratos, financeiro, comissões, usuários e convites, onboarding assistido de imobiliárias e alertas por e-mail.

## 2. Stack

| Camada | Tecnologia |
|---|---|
| Backend | Java 21, Spring Boot 3 (monólito modular `api → domain → infra`), Spring Data JPA, MapStruct, Maven |
| Banco | PostgreSQL + Flyway (migrations em `imob-erp-backend/src/main/resources/db/migration`) |
| Autenticação | Clerk (JWT RS256 via JWKS; papel e tenant no `publicMetadata`) |
| Arquivos | Cloudflare R2 (S3 compatível): fotos de imóveis e PDF de contrato |
| E-mail | SMTP (Mailpit em dev), com outbox transacional |
| Jobs | `@Scheduled` com lock ShedLock (JDBC) |
| Frontend | Next.js 14 (App Router), TypeScript, Tailwind, `@clerk/nextjs`, `@dnd-kit`, Vitest |
| Deploy | Railway (backend) |

## 3. Estrutura do repositório

```
imob_erp/
├── imob-erp-backend/     # API Spring Boot (README próprio)
├── imob-erp-frontend/    # Next.js
├── docs/                 # architecture.md, funcionalidades.md, projeto.md
├── .github/workflows/    # CI (só a raiz é lida pelo GitHub)
└── docker-compose.yml    # Postgres (porta 5434) e Mailpit
```

## 4. Rodando localmente

**Pré-requisitos:** JDK 21 (o JDK 25 quebra o Lombok), Docker, Node.js, uma aplicação Clerk e um bucket R2.

1. **Infra local**
   ```powershell
   docker compose up -d
   ```
   Sobe o Postgres em `localhost:5434` (banco `imobcrm`; as portas 5432 e 5433 costumam ser de outros projetos) e o Mailpit (SMTP `1025`, interface em `http://localhost:8025`).

2. **Backend** (porta `8080`)
   ```powershell
   cd imob-erp-backend
   ./mvnw spring-boot:run
   ```
   As migrations rodam sozinhas na subida. Swagger em `/swagger-ui.html`.

3. **Frontend** (porta `3000`)
   ```powershell
   cd imob-erp-frontend
   npm ci
   npm run dev
   ```

**A origem `http://localhost:3000` é fixa:** o CORS e o `azp` do Clerk esperam essa origem. Um `next dev` esquecido na 3000 faz o novo subir na 3001 e quebra o login. Não rode `npm ci` com o `next dev` ativo (o SWC trava e o `node_modules` fica corrompido).

### Variáveis de ambiente

Os exemplos estão em `imob-erp-backend/.env.example` e `imob-erp-frontend/.env.example`. Nunca commite valores reais.

> **Atenção:** o backend **não lê o arquivo `.env`** (não há biblioteca de dotenv). As variáveis precisam estar no ambiente do processo: na IDE, na Run Configuration; no terminal, exportadas antes do `spring-boot:run`. Se a Run Configuration divergir do `.env`, vale a Run Configuration. Um caso real: `R2_BUCKET_NAME` diferente causou `AccessDenied` no R2.

**Backend**

| Variável | Uso |
|---|---|
| `SPRING_DATASOURCE_URL`, `_USERNAME`, `_PASSWORD` | Banco (padrão aponta para o Postgres do `docker-compose`) |
| `CLERK_SECRET_KEY`, `CLERK_JWKS_URL`, `CLERK_ISSUER` | Validação do JWT e API do Clerk |
| `CLERK_AUTHORIZED_PARTIES` | Origens aceitas no `azp` (padrão: `APP_CORS_ORIGIN`) |
| `APP_CORS_ORIGIN` | Origem do frontend (padrão `http://localhost:3000`) |
| `R2_ACCOUNT_ID`, `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY`, `R2_BUCKET_NAME`, `R2_PUBLIC_URL` | Cloudflare R2 |
| `OPERATOR_API_KEY` | Chave do endpoint de onboarding; vazia desliga o endpoint |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_FROM`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH`, `MAIL_STARTTLS` | SMTP (padrão: Mailpit) |
| `SPRING_PROFILES_ACTIVE` | Perfil (`dev` por padrão) |
| `APP_SEED_ENABLED`, `APP_SEED_TENANT_SLUG`, `APP_SEED_ADMIN_CLERK_ID` | Dados de exemplo (só no perfil `dev`) |
| `PORT` | Porta HTTP (padrão `8080`) |

**Frontend:** `NEXT_PUBLIC_CLERK_PUBLISHABLE_KEY`, `CLERK_SECRET_KEY`, `NEXT_PUBLIC_CLERK_SIGN_IN_URL`, `NEXT_PUBLIC_CLERK_SIGN_UP_URL`, `NEXT_PUBLIC_CLERK_SIGN_IN_FALLBACK_REDIRECT_URL`, `NEXT_PUBLIC_CLERK_SIGN_UP_FALLBACK_REDIRECT_URL` e `NEXT_PUBLIC_API_URL` (URL do backend). Para um build local sem Clerk real, valores fictícios bastam.

### Primeiro acesso (conta nova no Clerk)

Uma conta do Clerk recém-criada não tem tenant nem linha em `users`. No momento, o onboarding é **assistido**: com `OPERATOR_API_KEY` definida, o operador chama `POST /internal/v1/tenants` (header `X-Operator-Key`) com o nome da imobiliária e o e-mail do admin; o endpoint cria o tenant e o ADMIN e grava `tenantId` e `role` no Clerk. Depois, faça logout e login para o token trazer os dados novos. Detalhes em [funcionalidades.md](funcionalidades.md#8-onboarding-de-imobiliária-assistido).

## 5. Papéis, permissões e multi-tenancy

- Três papéis: `ADMIN`, `CORRETOR` e `FINANCEIRO`. A matriz completa por módulo está em [funcionalidades.md](funcionalidades.md#papéis-e-permissões).
- O backend é a fonte da verdade (`@PreAuthorize` mais regras por dono nos services); o frontend só espelha em `lib/permissions.ts` para esconder menus e ações.
- **Isolamento por tenant:** o `tenantId` vem sempre do token (`TenantContext`), nunca do corpo. Toda consulta filtra por ele, e ids recebidos são validados contra o tenant. Corretor só vê os próprios leads e contratos; recurso de outro tenant ou de outro corretor devolve 404.
- Os testes de isolamento (`TenantIsolationTest`) bloqueiam o CI.

## 6. Decisões de arquitetura

A estrutura de pacotes, as regras de camada e o fluxo de ativação de contrato estão em [`architecture.md`](architecture.md). Decisões posteriores, em resumo:

| Decisão | Motivo |
|---|---|
| Monólito modular, package-by-feature | Time pequeno e produto em validação |
| Isolamento multi-tenant coberto por teste de integração (Testcontainers + Flyway) | Vazamento entre tenants é o risco maior |
| Jobs por tenant, fuso `America/Sao_Paulo`, lock ShedLock | Corretude com mais de uma instância |
| Validação de upload por magic bytes, compartilhada por fotos e PDF | Não confiar no `Content-Type` do cliente |
| Operações de foto travam a linha do imóvel (`SELECT ... FOR UPDATE`) | `@ElementCollection` é regravada inteira; uploads paralelos perdiam fotos |
| Convite pelo Clerk Invitations, vínculo no primeiro acesso por e-mail verificado | Sem fluxo próprio de senha e token |
| Onboarding assistido antes do self-serve | Validar com os primeiros clientes antes de automatizar |
| E-mail por outbox transacional | Entrega com retry, sem perder mensagem se o envio falhar |
| `RestClient` do Clerk com `JdkClientHttpRequestFactory` | O `HttpURLConnection` padrão do Boot 3.3 não faz PATCH |
| Código funcional (funções puras, imutabilidade, `map`/`filter`/`Stream`/`Optional`) | Convenção de código do projeto |

## 7. Banco de dados

- Migrations Flyway `V1` a `V5` (schema inicial, `email_outbox`, `shedlock`, `photo_order` em `property_photos`, índices em FKs e únicos). O próximo número livre é `V6`, mas **confira `develop` e os PRs abertos** antes de criar uma: dois PRs com o mesmo número quebram um deles.
- PKs UUID, índices por `tenant_id` e nas FKs, únicos de e-mail por tenant e de comissão por contrato e corretor. Detalhes e pendências (PK de `property_photos`, `CHECK`s) em [funcionalidades.md](funcionalidades.md#10-multi-tenancy-autenticação-e-segurança).
- Uma migration que cria índice único aborta, sem alterar nada, se já houver duplicatas; a mensagem traz a consulta para achá-las.

## 8. Testes e CI

- **Backend:** testes de integração com Testcontainers, então **o Docker precisa estar rodando**. Nomeie as classes `*Test` (o surefire ignora `*IT`). Base compartilhada: `src/test/java/com/imobcrm/support/IntegrationTestBase` (Postgres, Flyway, JWKS local, `token(clerkId, tenantId, role)`); para storage use `@MockBean S3Client`; cada classe semeia o próprio tenant. `@Valid` roda antes do `@PreAuthorize`: em teste de 403, envie um corpo válido.
  ```powershell
  $env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
  ./mvnw test
  ```
- **Frontend:** `npm test` (Vitest), `npm run lint` e `npm run build`.
- **CI:** `.github/workflows/backend-ci.yml` roda `mvn test` em push e PR para `main` e `develop`. **O frontend não tem CI ativo** (o workflow dentro de `imob-erp-frontend/.github` é ignorado pelo GitHub); rode lint, build e testes localmente antes do PR.

## 9. Fluxo de trabalho

O Jira (projeto **IMOB**) é a referência do que e de quando fazer; siga a ordem das labels `fila-N`/`sprint-N`. Para cada tarefa:

1. Mover o card para **Fazendo**.
2. Criar uma branch nova a partir de `develop`.
3. Implementar com testes (unitários e, quando couber, de integração). Teste quebrado se corrige; não se ignora, pula ou apaga.
4. **Atualizar a documentação** (regra abaixo).
5. Com testes verdes e critérios de aceite atendidos, abrir o PR para `develop`; sem push direto em `develop` ou `main`.
6. Só depois de o PR existir, mover o card para **Feito**.

Conflito de PR: resolva com merge de `develop` na branch (sem rebase e sem force push).

### Regra: atualizar a documentação a cada tarefa

Toda tarefa concluída atualiza estes documentos **no mesmo PR**: novo endpoint, tela, regra, permissão, migration, variável de ambiente, limitação conhecida ou mudança de status (validada, sem validação visual, parcial) e o roadmap. Se nada muda, escreva no PR "docs: sem alteração". A atualização é feita com a skill `doc-coauthoring`: edição cirúrgica por seção, sem reescrever o documento inteiro.

## 10. Pendências operacionais conhecidas

- Verificar upload no R2 real em staging (os testes mockam o S3).
- Testar o corretor com login real no Clerk.
- Verificações visuais no navegador (desktop e 375px) de contratos, imóveis, financeiro e comissões.
- Colocar o frontend no CI.
- `app/error.tsx` e `app/not-found.tsx` no frontend.

A lista completa, por módulo, está em [funcionalidades.md](funcionalidades.md).
