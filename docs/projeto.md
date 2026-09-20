# Imob ERP — Documentação do projeto

Guia para quem vai desenvolver, rodar ou dar manutenção no sistema. O que cada módulo faz está em [`funcionalidades.md`](funcionalidades.md); a estrutura de pacotes e as regras de camada estão em [`architecture.md`](architecture.md) (não repetidas aqui).

## 1. Visão do produto

ERP + CRM para pequenas imobiliárias (2 a 10 corretores), em modelo SaaS **multi-tenant**: cada imobiliária é um tenant, com seus dados isolados.

Fluxo principal: **Lead → fechamento → contrato → parcelas financeiras → comissão do corretor.**

Módulos: imóveis (com fotos), leads (kanban), visitas, contratos, financeiro, comissões, usuários e convites e onboarding assistido de imobiliárias. A infraestrutura de e-mail (outbox e envio por SMTP) está pronta, mas **nada a usa ainda**: os alertas de vencimento (IMOB-28) são o primeiro gatilho previsto, e o convite de corretor passa pelo Clerk.

Repositório: <https://github.com/renatoganske/imob_erp>. Backlog: [board do Jira (projeto IMOB)](https://renatoganskejr.atlassian.net/jira/software/projects/IMOB/boards); os cards `IMOB-xx` citados nos docs estão lá.

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
   Sobe o Postgres em `localhost:5434` (banco `imobcrm`; as portas 5432 e 5433 costumam ser de outros projetos) e o Mailpit (SMTP `1025`, interface em `http://localhost:8025`). No perfil `dev` o health de e-mail fica desligado (`management.health.mail.enabled=false`), então `/actuator/health` continua `UP` com o Mailpit parado; em produção o indicador segue ligado.

2. **Backend** (porta `8080`). Aponte o `JAVA_HOME` para o JDK 21 e suba:

   PowerShell:
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
   cd imob-erp-backend
   ./mvnw spring-boot:run
   ```
   bash (Git Bash, Linux, macOS):
   ```bash
   export JAVA_HOME="/c/Program Files/Java/jdk-21"   # ajuste o caminho ao seu JDK 21
   cd imob-erp-backend
   ./mvnw spring-boot:run
   ```
   As migrations rodam sozinhas na subida. Swagger em `/swagger-ui.html`. O mesmo vale para as variáveis de ambiente da seção abaixo: `$env:NOME = "valor"` no PowerShell, `export NOME=valor` no bash.

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

**Backend — obrigatórias** (sem padrão; a aplicação não sobe ou a função quebra sem elas)

| Variável | Uso |
|---|---|
| `CLERK_SECRET_KEY` | Chave secreta do Clerk (API de backend) |
| `CLERK_JWKS_URL` | URL das chaves públicas para validar o JWT |
| `CLERK_ISSUER` | Issuer do JWT (Frontend API do Clerk). O padrão é vazio, mas o filtro JWT lança `IllegalStateException` sem ela |
| `R2_ACCOUNT_ID`, `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY`, `R2_BUCKET_NAME`, `R2_PUBLIC_URL` | Cloudflare R2 (fotos e PDFs) |

**Backend — opcionais** (têm padrão)

| Variável | Padrão | Uso |
|---|---|---|
| `SPRING_DATASOURCE_URL`, `_USERNAME`, `_PASSWORD` | Postgres do `docker-compose` (`localhost:5434`) | Banco |
| `SPRING_PROFILES_ACTIVE` | `dev` | Perfil |
| `PORT` | `8080` | Porta HTTP |
| `APP_CORS_ORIGIN` | `http://localhost:3000` | Origem do frontend (CORS) |
| `CLERK_AUTHORIZED_PARTIES` | valor de `APP_CORS_ORIGIN` | Origens aceitas no `azp` do token |
| `OPERATOR_API_KEY` | vazia | Chave do onboarding assistido; vazia **desliga** o endpoint |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_FROM` | Mailpit (`localhost:1025`), `no-reply@imob-erp.local` | SMTP |
| `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH`, `MAIL_STARTTLS` | vazias / `false` | Autenticação e TLS do SMTP (produção) |
| `APP_SEED_ENABLED`, `APP_SEED_TENANT_SLUG`, `APP_SEED_ADMIN_CLERK_ID` | `true` / `imobiliaria-dev` / vazia | Dados de exemplo (só no perfil `dev`) |

**Frontend — obrigatórias:** `NEXT_PUBLIC_CLERK_PUBLISHABLE_KEY`, `CLERK_SECRET_KEY` e `NEXT_PUBLIC_API_URL` (URL do backend, por exemplo `http://localhost:8080`; lida sem valor padrão em `lib/api.ts`).
**Frontend — de rotas do Clerk** (o `.env.example` traz os valores, como `/sign-in`): `NEXT_PUBLIC_CLERK_SIGN_IN_URL`, `NEXT_PUBLIC_CLERK_SIGN_UP_URL`, `NEXT_PUBLIC_CLERK_SIGN_IN_FALLBACK_REDIRECT_URL` e `NEXT_PUBLIC_CLERK_SIGN_UP_FALLBACK_REDIRECT_URL`. Nenhuma é lida no código do projeto; são configuração do próprio Clerk, então copie-as do `.env.example`.

**Onde obter os valores**
- **Clerk:** no dashboard do Clerk, na sua aplicação, em *API Keys* ficam a chave pública (publishable) e a secreta; a URL do JWKS e o issuer aparecem em *API Keys* → *Show API URLs* (o issuer é a Frontend API URL).
- **R2:** no dashboard da Cloudflare, em *R2*: o Account ID e o bucket aparecem na visão geral; o par `ACCESS_KEY_ID`/`SECRET_ACCESS_KEY` vem de *Manage API tokens* (o token precisa cobrir o bucket usado); `R2_PUBLIC_URL` é o domínio público do bucket.
- Os nomes dos menus nos painéis do Clerk e da Cloudflare mudam com o tempo; use estes como ponto de partida.

### Primeiro acesso (conta nova no Clerk)

Uma conta do Clerk recém-criada não tem tenant nem linha em `users`. No momento, o onboarding é **assistido**: com `OPERATOR_API_KEY` definida, o operador chama `POST /internal/v1/tenants` (header `X-Operator-Key`) com o nome da imobiliária e o e-mail do admin; o endpoint cria o tenant e o ADMIN e grava `tenantId` e `role` no Clerk. A conta do admin **já precisa existir no Clerk** (com o e-mail verificado): faça o cadastro em `/sign-up` antes.

Corpo JSON (`TenantOnboardingRequest`; os dois campos são obrigatórios e `adminEmail` precisa ser um e-mail válido):

```json
{ "tenantName": "Nome da Imobiliária", "adminEmail": "admin@exemplo.com" }
```

bash:
```bash
curl -X POST http://localhost:8080/internal/v1/tenants \
  -H "Content-Type: application/json" \
  -H "X-Operator-Key: <OPERATOR_API_KEY>" \
  -d '{"tenantName":"Nome da Imobiliária","adminEmail":"admin@exemplo.com"}'
```
PowerShell (use `curl.exe`; no PowerShell 5.1 `curl` é alias de `Invoke-WebRequest`):
```powershell
$body = '{"tenantName":"Nome da Imobiliária","adminEmail":"admin@exemplo.com"}'
$body | curl.exe -X POST http://localhost:8080/internal/v1/tenants `
  -H "Content-Type: application/json" -H "X-Operator-Key: <OPERATOR_API_KEY>" --data-binary "@-"
```

Resposta (`TenantOnboardingResponse`): `{ tenantId, slug, adminUserId, adminClerkUserId, created }`. Status **201** quando criou e **200** quando só re-sincronizou um tenant existente (a chamada é idempotente); **403** se a chave estiver errada ou o endpoint desligado. Depois, faça logout e login para o token trazer os dados novos. Detalhes em [funcionalidades.md](funcionalidades.md#8-onboarding-de-imobiliária-assistido).

## 5. Papéis, permissões e multi-tenancy

- Três papéis: `ADMIN`, `CORRETOR` e `FINANCEIRO`. A matriz completa por módulo está em [funcionalidades.md](funcionalidades.md#papéis-e-permissões).
- O backend é a fonte da verdade (`@PreAuthorize` mais regras por dono nos services); o frontend só espelha em `lib/permissions.ts` para esconder menus e ações.
- **Isolamento por tenant:** o `tenantId` vem sempre do token (`TenantContext`), nunca do corpo. Toda consulta filtra por ele, e ids recebidos são validados contra o tenant. Corretor só vê os próprios leads e contratos; recurso de outro tenant ou de outro corretor devolve 404.
- **Visitas e comissões não têm escopo por corretor:** qualquer corretor lista todas as visitas e comissões do tenant (a confirmar com o produto se é intencional). Ver [Visitas](funcionalidades.md#3-visitas) e [Comissões](funcionalidades.md#6-comissões).
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

- Migrations Flyway em `imob-erp-backend/src/main/resources/db/migration`:

  | Versão | Arquivo | O que faz |
  |---|---|---|
  | V1 | `V1__create_initial_schema.sql` | Schema inicial: tenants, users, properties, `property_photos`, leads, `lead_property`, visits, contracts, `financial_entries`, commissions |
  | V2 | `V2__create_email_outbox.sql` | Tabela `email_outbox` (outbox de e-mail) |
  | V3 | `V3__create_shedlock.sql` | Tabela `shedlock` (lock dos jobs agendados) |
  | V4 | `V4__add_property_photo_order.sql` | Coluna `photo_order` em `property_photos` (ordem das fotos; IMOB-50) |
  | V5 | `V5__add_fk_indexes_and_unique_constraints.sql` | Índices nas FKs, índice composto do job de atrasados e únicos de e-mail por tenant e de comissão por contrato e corretor (IMOB-52) |

  O próximo número livre é `V6`, mas **confira `develop` e os PRs abertos** antes de criar uma: dois PRs com o mesmo número quebram um deles.
- PKs UUID, índices por `tenant_id` e nas FKs, únicos de e-mail por tenant e de comissão por contrato e corretor. Detalhes e pendências (PK de `property_photos`, `CHECK`s) em [funcionalidades.md](funcionalidades.md#10-multi-tenancy-autenticação-e-segurança).
- Uma migration que cria índice único aborta, sem alterar nada, se já houver duplicatas; a mensagem traz a consulta para achá-las.

## 8. Testes e CI

- **Backend:** testes de integração com Testcontainers, então **o Docker precisa estar rodando**. Nomeie as classes `*Test` (o surefire ignora `*IT`). Base compartilhada: `src/test/java/com/imobcrm/support/IntegrationTestBase` (Postgres, Flyway, JWKS local, `token(clerkId, tenantId, role)`); para storage use `@MockBean S3Client`; cada classe semeia o próprio tenant. `@Valid` roda antes do `@PreAuthorize`: em teste de 403, envie um corpo válido.
  PowerShell:
  ```powershell
  $env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
  cd imob-erp-backend; ./mvnw test
  ```
  bash:
  ```bash
  export JAVA_HOME="/c/Program Files/Java/jdk-21"   # ajuste o caminho ao seu JDK 21
  cd imob-erp-backend && ./mvnw test
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

Toda tarefa concluída atualiza a documentação **no mesmo PR**. Quais arquivos:

| Arquivo | Atualize quando |
|---|---|
| `docs/funcionalidades.md` | Muda endpoint, tela, regra de negócio, permissão, status de um item (✅ 👁️ 🟡 ⬜), limitação conhecida ou a lista de testes; e o **Roadmap** (tirar o card entregue, incluir os novos) |
| `docs/projeto.md` | Muda variável de ambiente, passo para rodar, migration (tabela da seção 7), decisão de arquitetura, CI, fluxo de trabalho ou termo do glossário |
| `README.md` | Muda o início rápido (comandos, portas, pré-requisitos) ou a lista de documentos |

Se nada muda, escreva no PR "docs: sem alteração". `docs/architecture.md` só muda se a estrutura de pacotes ou as regras de camada mudarem.

A skill `doc-coauthoring` é um roteiro de coautoria de documentos usado pelo Claude Code: define o que mudou, edita só as seções afetadas (sem reescrever o arquivo inteiro) e confere se um leitor sem contexto entenderia o resultado.

## 10. Pendências operacionais conhecidas

- Verificar upload no R2 real em staging (os testes mockam o S3).
- Testar o corretor com login real no Clerk.
- Verificações visuais no navegador (desktop e 375px) de contratos, imóveis, financeiro e comissões.
- Colocar o frontend no CI.
- `app/error.tsx` e `app/not-found.tsx` no frontend.

A lista completa, por módulo, está em [funcionalidades.md](funcionalidades.md).

## 11. Glossário

| Termo | Significado |
|---|---|
| **Tenant** | Uma imobiliária cliente. Todos os dados de negócio carregam o `tenant_id` e nunca aparecem para outro tenant. |
| **RN-xx** | Número de uma regra de negócio do backlog original (por exemplo, RN-03: ativação de contrato; RN-06: job de atrasados), citado nos comentários do código e nos docs. |
| **Outbox** | Padrão em que o e-mail é gravado numa tabela (`email_outbox`) na mesma transação da operação de negócio e enviado depois por um job, com retry. Não se perde mensagem se o envio falhar. |
| **Spike** | Card de investigação ou validação com tempo limitado (por exemplo, IMOB-30, 31 e 32: conferir as telas rodando o app), não uma entrega de funcionalidade. |
| **Magic bytes** | Os primeiros bytes de um arquivo, que revelam o formato real (JPEG, PNG, WebP, PDF). O upload confere isso em vez de confiar no `Content-Type` enviado pelo cliente. |
| **Idempotente** | Operação que pode ser repetida sem efeito extra: a segunda execução dá o mesmo resultado (o job de atrasados e o onboarding assistido são assim). |
| **ShedLock** | Biblioteca que usa uma tabela (`shedlock`) como lock, para que, com mais de uma instância do backend, só uma execute cada job agendado. |
| **Testcontainers** | Biblioteca de testes que sobe um Postgres real em Docker para os testes de integração; por isso o Docker precisa estar rodando. |
