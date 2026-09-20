# Funcionalidades — Imob ERP

Levantamento do que está **implementado** no `develop` (até o PR #25). Baseado na leitura do código; para setup, arquitetura e fluxo de trabalho, ver [`projeto.md`](projeto.md).

## Legenda dos status

| Status | Significado |
|---|---|
| ✅ Validada | Implementada e coberta por testes automatizados |
| 👁️ Sem validação visual | Implementada **e testada** (testes automatizados passam), mas ninguém conferiu a tela rodando no navegador, em desktop e em 375px (celular) |
| 🟡 Parcial | Existe no código, mas está incompleta: falta uma tela, um efeito colateral ou um gatilho. A linha ou a seção diz o que falta |
| ⬜ Não existe | Planejada; ver [Roadmap](#roadmap) |

Caminhos: backend em `imob-erp-backend/src/main/java/com/imobcrm`, frontend em `imob-erp-frontend/src`. Os cards `IMOB-xx` estão no [board do Jira](https://renatoganskejr.atlassian.net/jira/software/projects/IMOB/boards). Termos como RN-xx, outbox e magic bytes estão no glossário de [`projeto.md`](projeto.md#11-glossário).

---

## Papéis e permissões

Papéis: `ADMIN`, `CORRETOR`, `FINANCEIRO` (`user/domain/enums/Role.java`). O papel vem do `publicMetadata.role` do token do Clerk.

| Módulo | Leitura | Escrita |
|---|---|---|
| Imóveis | ADMIN, CORRETOR | ADMIN, CORRETOR (excluir: só ADMIN) |
| Leads | ADMIN, CORRETOR, FINANCEIRO | ADMIN, CORRETOR (reatribuir: só ADMIN) |
| Visitas | ADMIN, CORRETOR | ADMIN, CORRETOR |
| Contratos | ADMIN, FINANCEIRO; CORRETOR só os próprios, sem dados sensíveis (resumo de vencimentos: só ADMIN e FINANCEIRO) | ADMIN, FINANCEIRO |
| Financeiro | ADMIN, FINANCEIRO | ADMIN, FINANCEIRO |
| Comissões | ADMIN, CORRETOR, FINANCEIRO | pagar e relatório: ADMIN, FINANCEIRO |
| Usuários | ADMIN | ADMIN |
| Onboarding (`/internal/v1/tenants`) | chave `X-Operator-Key`, fora do JWT | idem |

---

## 1. Imóveis

Cadastro, busca e galeria de fotos. Rotas `/api/v1/properties`:

| Verbo | Rota | Papéis | O que faz |
|---|---|---|---|
| GET | `/` | ADMIN, CORRETOR | Busca paginada (tipo, status, bairro, preço mín./máx.) |
| GET | `/{id}` | ADMIN, CORRETOR | Detalhe |
| POST | `/` | ADMIN, CORRETOR | Cria (status inicial `DISPONIVEL`) |
| PUT | `/{id}` | ADMIN, CORRETOR | Edita |
| PATCH | `/{id}/status` | ADMIN, CORRETOR | Muda status |
| DELETE | `/{id}` | ADMIN | Soft delete (`active=false`) |
| POST | `/{id}/photos` | ADMIN, CORRETOR | Envia uma foto |
| DELETE | `/{id}/photos/{key}` | ADMIN, CORRETOR | Remove foto (banco e R2) |
| PUT | `/{id}/photos/order` | ADMIN, CORRETOR | Reordena (`{"keys":[...]}`; a 1ª é a capa) |

**Modelo:** tipo (`CASA`, `APARTAMENTO`, `COMERCIAL`, `TERRENO`), finalidade (`VENDA`, `ALUGUEL`, `AMBOS`), status (`DISPONIVEL`, `RESERVADO`, `VENDIDO`, `ALUGADO`), título, descrição, endereço, bairro, cidade, preço, área, quartos, banheiros, vagas e fotos.

**Regras** (`property/domain/PropertyService.java`)
- RN-04: imóvel `RESERVADO`, `VENDIDO` ou `ALUGADO` só muda de status pelo fluxo de contrato (400 `PROPERTY_STATUS_LOCKED`).
- RN-10: exclusão é soft delete; imóvel inativo some das buscas e de novos vínculos.
- RN-11 (fotos): máximo de 20 (`MAX_PHOTOS_EXCEEDED`); JPG, PNG ou WebP até 10 MB, validados por magic bytes (415 e 413).
- Operações de foto travam a linha do imóvel (`SELECT ... FOR UPDATE`): uploads paralelos não perdem nem duplicam fotos.
- Reordenar exige exatamente o mesmo conjunto de fotos, sem repetição (422 `PHOTO_ORDER_MISMATCH`). A ordem fica em `property_photos.photo_order` (V4).
- Chave no R2: `{tenantId}/properties/{propertyId}/{arquivo}`.
- Falha do R2 devolve 502 `STORAGE_UNAVAILABLE`.

**Telas:** `/imoveis` (cards e filtros), `/imoveis/novo`, `/imoveis/[id]` (detalhe e galeria), `/imoveis/[id]/editar`. Em `PhotoUpload.tsx`: envio de várias fotos (3 em paralelo, limite e validação no cliente), exclusão com confirmação e arrastar para reordenar com `@dnd-kit` (mouse, touch e teclado; a primeira foto mostra "Capa"). Preço com máscara de R$.

| Item | Status |
|---|---|
| CRUD, busca e regras de status | ✅ |
| Upload, exclusão e reordenação de fotos (API) | ✅ |
| Upload múltiplo, exclusão e drag and drop (tela) | 👁️ |
| Excluir imóvel e mudar status pela tela | 🟡 os hooks existem em `hooks/useProperties.ts`; nenhuma tela os usa |
| Campos por tipo de imóvel (área de terreno/construída etc.) | ⬜ IMOB-46 |
| Verificar os critérios da tela de imóveis rodando o app (spike) | ⬜ IMOB-32 |

**Limitações conhecidas**
- `canManagePhotos` (só ADMIN e CORRETOR) esconde as ações de **excluir** e **reordenar** para FINANCEIRO, mas **não cobre o envio**: o botão "Adicionar fotos" ainda aparece para ele e o backend responde 403.
- Os testes mockam o S3. O upload real no R2 funciona no ambiente local, mas não foi verificado em staging.

**Testes:** `PropertyServiceTest`, `PropertyPhotoUploadTest`, `PropertyPhotoOrderTest`, `PropertyServiceReorderTest`, `UploadValidatorTest`; no frontend, `PhotoUpload.test.tsx` e `PhotoUpload.reorder.test.tsx`.

---

## 2. Leads e CRM

Rotas `/api/v1/leads`: `GET /` e `GET /{id}` (ADMIN, CORRETOR, FINANCEIRO); `POST /`, `PUT /{id}`, `PATCH /{id}/stage`, `POST /{id}/properties` e `DELETE /{id}/properties/{propertyId}` (ADMIN, CORRETOR); `PATCH /{id}/assign` (só ADMIN).

**Modelo:** nome, telefone, e-mail, origem (`WHATSAPP`, `SITE`, `INDICACAO`, `PORTAL_ZAP`, `PORTAL_VIVAREAL`, `OUTRO`), etapa (`NOVO`, `EM_ATENDIMENTO`, `VISITA_AGENDADA`, `PROPOSTA`, `FECHADO`, `PERDIDO`), corretor responsável, observações e imóveis de interesse.

**Regras** (`lead/domain/LeadService.java`)
- Corretor só vê e altera os **próprios leads**; lead de outro corretor devolve 404. O filtro `assignedTo` enviado por ele é ignorado.
- Corretor não atribui lead a outro corretor (403); reatribuir é do ADMIN.
- Lead novo entra em `NOVO`; sem `assignedTo`, fica com quem criou.
- Fechar (`FECHADO`) exige ao menos um imóvel de interesse (422 `LEAD_FECHADO_SEM_IMOVEL`).
- RN-02: ao fechar, o backend **cria um contrato em rascunho** (tipo pela finalidade do imóvel, valor igual ao preço, documentos e nomes `PENDENTE`).
- Qualquer transição de etapa é aceita (não há máquina de estados).

**Telas:** `/leads` com kanban por etapa (`LeadKanban.tsx`, `LeadCard.tsx`), modal de detalhe (`LeadModal.tsx`; em lead `FECHADO` mostra "Criar contrato" para quem gerencia contratos, com o formulário pré-preenchido) e formulário de novo lead com máscara de telefone.

| Item | Status |
|---|---|
| Criar, listar, mudar etapa, escopo por corretor, fechamento | ✅ |
| Kanban com mudança de etapa por botão ou menu | 👁️ |
| Drag and drop, confirmação e toast no kanban | ⬜ IMOB-34 |
| Reatribuir lead e associar/remover imóveis de interesse pela tela | 🟡 endpoints e hook existem; sem tela |
| Editar lead pela tela; histórico de atividades | ⬜ |

**Testes:** `LeadServiceTest`, `LeadAuthorizationTest`; no frontend, `LeadModal.test.tsx`.

---

## 3. Visitas

Rotas `/api/v1/visits` (ADMIN, CORRETOR): `GET /` (filtro `agentId`, paginado), `GET /{id}`, `POST /` (cria `AGENDADA`), `PATCH /{id}/status` e `PATCH /{id}/result` (grava o resultado e marca `REALIZADA`).

Status: `AGENDADA`, `REALIZADA`, `CANCELADA`. Lead, imóvel e corretor do corpo são validados contra o tenant.

**Telas:** `/visitas` com lista e cards (`VisitList.tsx`, `VisitCard.tsx`), agendamento (`VisitFormModal.tsx`) e registro de resultado (`VisitModal.tsx`).

| Item | Status |
|---|---|
| Agendar, listar, registrar resultado | ✅ (backend) · 👁️ (tela) |
| Cancelar visita pela tela | 🟡 endpoint e hook existem; sem tela |
| Filtros por data/status e regra de imóvel vendido | ⬜ IMOB-24 |
| Conflito de horário | ⬜ |
| Adicionar ao Google Agenda / integração real | ⬜ IMOB-47 / IMOB-48 |

**Limitações conhecidas**
- **O backend não escopa visitas por corretor:** qualquer corretor lista todas as do tenant (diferente de leads e contratos). Confirmar se é intencional.
- A agenda é uma lista, não um calendário.

**Testes:** `VisitServiceTest`.

---

## 4. Contratos

Rotas `/api/v1/contracts`: `GET /` (filtros `status`, `type` e `expiringInDays`) e `GET /{id}` (ADMIN, FINANCEIRO, CORRETOR); `GET /expiring-summary` (ADMIN, FINANCEIRO); `POST /`, `PUT /{id}`, `PATCH /{id}/status` e `POST /{id}/document` (ADMIN, FINANCEIRO).

**Modelo:** tipo (`COMPRA_VENDA`, `LOCACAO`), status (`RASCUNHO`, `ATIVO`, `ENCERRADO`, `CANCELADO`), lead (opcional), imóvel, corretor, valor, datas (assinatura, início, fim), índice de reajuste (`IGPM`, `IPCA`, `FIXO`), comprador e proprietário (nome e documento), override de comissão, observações e URL do PDF.

**Regras** (`contract/domain/ContractService.java`)
- Criação sempre em `RASCUNHO`; lead, imóvel e corretor são validados contra o tenant. Só rascunho é editável (`CONTRACT_NOT_EDITABLE`).
- **Ativação (RN-03)**, só a partir de rascunho (`CONTRACT_NOT_DRAFT`), tudo na mesma transação:
  - exige taxa de comissão, do contrato ou do corretor (`AGENT_WITHOUT_COMMISSION_RATE`);
  - marca o imóvel `ALUGADO` (locação) ou `VENDIDO`;
  - gera 12 parcelas de aluguel ou 1 lançamento de venda;
  - gera a comissão do corretor.
- Voltar para rascunho é proibido (`INVALID_STATUS_TRANSITION`).
- **Alertas de vencimento (IMOB-28):** só contratos de **locação** `ATIVO` com `endDate` definido, cujo fim cai entre hoje (fuso de Brasília) e hoje + N dias, ambos inclusivos. Contratos que já passaram do fim e continuam `ATIVO` **não** entram.
  - `GET /?expiringInDays=N` aceita de 1 a 365 (`INVALID_EXPIRY_WINDOW`, 422, fora disso), ordena pelo vencimento mais próximo e recusa combinar com outro `status` ou `type` (`INVALID_EXPIRY_FILTER`, 422). O escopo do corretor continua valendo.
  - `GET /expiring-summary` devolve a contagem **acumulada** em até 30, 60 e 90 dias (`within30Days`, `within60Days`, `within90Days`): um contrato que vence em 20 dias conta nos três, e cada número é igual ao total do filtro correspondente.
  - Níveis na tela: até 30 dias vermelho, até 60 amarelo, até 90 informativo.
- **Corretor** lê só os contratos em que é o agente (404 nos demais), sem CPF/CNPJ e sem o PDF, e não escreve.
- PDF: um por contrato, só PDF de até 10 MB, validado por magic bytes. O anterior é apagado do R2 após o commit; se a transação reverte, apaga o novo.

**Telas:** `/contratos` (lista com filtros de status, tipo e período; para ADMIN e FINANCEIRO, três cartões com a contagem de locações vencendo em 30/60/90 dias que abrem `/contratos?expiringInDays=N`, e um selo "Vence em N dias" nas linhas dentro de 90 dias), o "Resumo" (página inicial) com a seção "Locações vencendo" para os mesmos papéis, `/contratos/novo` (bloqueada para corretor) e `/contratos/[id]` (detalhe, "Ativar contrato" com diálogo de resumo, upload de PDF e link "Ver parcelas em Financeiro"). Máscaras de R$ e CPF/CNPJ, este com validação dos dígitos.

| Item | Status |
|---|---|
| Criar, editar rascunho, ativar (imóvel, parcelas, comissão), PDF | ✅ |
| Acesso restrito do corretor | ✅ (testes) · login real no Clerk não testado |
| Fluxo criar → ativar → ver parcelas na tela | 👁️ IMOB-29 |
| Encerrar/cancelar | 🟡 IMOB-33: parcial no efeito. A API muda o status, mas não reverte o status do imóvel nem cancela as parcelas futuras; também não há tela |
| Cálculo de reajuste | 🟡 `adjustmentIndex` é só um campo |
| Alertas de vencimento 30/60/90 dias (filtro, resumo, cartões e selos) | ✅ (backend) · 👁️ (telas) IMOB-28 |

**Limitações conhecidas**
- Os alertas de vencimento aparecem só na tela: **não há e-mail nem notificação**, e locação `ATIVA` cujo fim já passou não gera alerta (nem é encerrada automaticamente). A "hoje" da tela é a data do navegador; a do backend é a de Brasília.
- Encerrar ou cancelar muda o status, mas **não reverte o imóvel nem as parcelas futuras**, e não há tela para isso. As transições a partir de `RASCUNHO` e `ATIVO` para `ENCERRADO`/`CANCELADO` não são restringidas.

**Testes:** `ContractServiceTest`, `ContractCorretorAccessTest`, `ContractDocumentUploadTest`, `ContractExpiryAlertsTest`, `ExpiryWindowTest`; no frontend, `ContractDetailClient.test.tsx`, `ActivateContractDialog.test.tsx`, `ContractDocumentUpload.test.tsx`, `ContractFilters.test.tsx`, `ExpiringContractsAlerts.test.tsx`, `lib/contracts.test.ts`, `lib/expiry.test.ts`.

---

## 5. Financeiro

Rotas `/api/v1/financial` (todas ADMIN e FINANCEIRO): `GET /entries` (tipo, status, categoria, período), `GET /entries/{id}`, `POST /entries` (lançamento manual, `PENDENTE`), `PATCH /entries/{id}/pay`, `PATCH /entries/{id}/cancel` e `GET /dashboard?month=YYYY-MM`.

**Modelo:** tipo (`RECEITA`, `DESPESA`), categoria (`ALUGUEL`, `PARCELA_VENDA`, `TAXA_ADMINISTRACAO`, `REPASSE_PROPRIETARIO`, `COMISSAO`, `DESPESA_OPERACIONAL`, `OUTRO`), status (`PENDENTE`, `PAGO`, `ATRASADO`, `CANCELADO`), valor, vencimento, data de pagamento, contrato (opcional) e recorrente.

**Regras**
- RN-05: locação gera 12 parcelas mensais a partir do início do contrato; venda gera 1 lançamento.
- RN-07: pagar lançamento já pago devolve `ENTRY_ALREADY_PAID`.
- **Dashboard:** saldo do mês (recebido menos pago), a receber e a pagar no mês, e **inadimplência acumulada** (atrasados até o fim do mês consultado). O "mês corrente" usa o fuso de Brasília.
- **Job de atrasados (RN-06)** (`OverdueJob` e `OverdueService`): cron `0 0 6 * * *` com `zone = "America/Sao_Paulo"` (todo dia às 6h de Brasília). "Vencido" é `status = PENDENTE` **e** `dueDate < hoje` (estritamente menor: a parcela que vence hoje ainda não é marcada; "hoje" é a data atual no `Clock` da aplicação). Marca como `ATRASADO`. Uma transação por tenant, com falha isolada; idempotente; lock ShedLock (JDBC, migration V3, `lockAtMostFor` de 10 min) para várias instâncias.

**Telas:** `/financeiro` (dashboard), `/financeiro/receber` e `/financeiro/pagar` (formulário de lançamento e lista com Pagar e Cancelar).

| Item | Status |
|---|---|
| Lançamentos, pagar, cancelar, dashboard, job de atrasados | ✅ |
| Telas de financeiro | 👁️ IMOB-30 |
| Seletor de mês no dashboard | 🟡 o backend aceita `month`; a tela não tem seletor |
| Repasse ao proprietário e taxa de administração automáticos, conciliação, exportação | ⬜ |

**Limitações conhecidas**
- Cancelar não bloqueia lançamento já pago (só pagar é bloqueado). Lançamento manual não pode ser editado.
- O critério "log visível no Railway" do job só se confirma depois de um deploy.

**Testes:** `FinancialDashboardTest`, `OverdueJobTest`, `OverdueServiceTest`. No frontend não há testes deste módulo.

---

## 6. Comissões

Rotas `/api/v1/commissions`: `GET /` e `GET /{id}` (ADMIN, CORRETOR, FINANCEIRO); `PATCH /{id}/pay` e `GET /report` (ADMIN, FINANCEIRO).

Status: `PENDENTE`, `PAGO`, `PARCELADO`. A comissão é criada na ativação do contrato: valor do contrato × taxa (override do contrato ou taxa do corretor) ÷ 100 (RN-08 e RN-09). Pagar duas vezes devolve `COMMISSION_ALREADY_PAID`. O relatório traz o total por corretor no período. É única por contrato e corretor no banco (V5).

**Telas:** `/comissoes` com relatório por corretor (`CommissionReport.tsx`) e lista com botão Pagar (`CommissionList.tsx`; cards no celular).

| Item | Status |
|---|---|
| Geração, pagamento e relatório | ✅ |
| Tela | 👁️ IMOB-31 |
| Fluxo de comissão `PARCELADO` | 🟡 existe no enum, sem fluxo que o use |

**Limitações conhecidas**
- **Sem escopo por corretor:** `GET /commissions` devolve todas as comissões do tenant a qualquer corretor (o filtro `agentId` é opcional). Confirmar se é intencional.
- O botão Pagar não é ocultado por papel na tela; o backend responde 403 ao corretor.

**Testes:** `CommissionServiceTest`.

---

## 7. Usuários e convites

Rotas `/api/v1/users` (todas ADMIN): `GET /`, `POST /invite`, `PATCH /{id}/role`, `PATCH /{id}/commission-rate` e `DELETE /{id}` (desativa).

**Regras** (`user/domain/UserService.java` e `InviteAcceptanceService.java`)
- E-mail único por tenant (409 `EMAIL_IN_USE`).
- **Conta nova:** cria o convite no Clerk (Invitations API, com `tenantId` e `role` no metadata) e um registro local pendente (`clerk_user_id = pending:<uuid>`, inativo).
- **Conta que já existe no Clerk:** vincula na hora e grava o metadata; se pertence a outro tenant, 409.
- **Primeiro acesso:** o filtro JWT vincula o convite pendente do tenant do token, casando pelo **e-mail verificado** da conta no Clerk.
- Falha do Clerk desfaz o registro local.
- Papel e taxa de comissão são editáveis pelo ADMIN. Excluir é desativar (`active=false`).

**Tela:** `/configuracoes/usuarios` com formulário de convite, tabela (papel, ativo ou pendente), edição da taxa de comissão e aviso "Sem taxa".

| Item | Status |
|---|---|
| Convite, vínculo no primeiro acesso, taxa de comissão | ✅ |
| Alterar papel e desativar usuário pela tela | 🟡 endpoints existem; sem tela |
| Webhook `user.created` | ⬜ IMOB-26 |

**Limitações conhecidas**
- A rota `/configuracoes/usuarios` aparece no menu para todos os papéis; só o backend bloqueia (403).
- Alterar o papel no banco **não atualiza** o `publicMetadata` do Clerk, e o papel do token vem do Clerk.
- IMOB-42: o health de e-mail derruba `/actuator/health` no dev quando o Mailpit está parado.

**Testes:** `UserInviteAcceptanceTest`, `ClerkBackendApiClientTest`, `ClerkBackendApiClientHttpTest`.

---

## 8. Onboarding de imobiliária (assistido)

`POST /internal/v1/tenants` (IMOB-38), protegido pelo header `X-Operator-Key` (comparação em tempo constante; sem `OPERATOR_API_KEY` configurada o endpoint fica desligado).

Recebe `{ "tenantName", "adminEmail" }` (corpo JSON e exemplo de `curl` em [`projeto.md`](projeto.md#primeiro-acesso-conta-nova-no-clerk)). Localiza a conta do Clerk pelo e-mail verificado (`CLERK_USER_NOT_FOUND` se não houver), cria o tenant (slug derivado do nome, plano `STARTER`) e o usuário ADMIN em uma transação, e grava `tenantId` e `role` no `publicMetadata` do Clerk fora da transação. É **idempotente**: repetir a chamada re-sincroniza o metadata. Conflitos tratados: `EMAIL_IN_USE`, `TENANT_SLUG_TAKEN`, `TENANT_MISMATCH`, `CLERK_TENANT_INVALID` e `CLERK_TENANT_NOT_FOUND` (IMOB-41: reaproveita o tenant já indicado no Clerk).

| Item | Status |
|---|---|
| Criação assistida de tenant + admin | ✅ |
| Self-serve, webhook do Clerk e tela de criação | ⬜ IMOB-26 |
| Gestão de tenant (editar, suspender, planos, cobrança) | ⬜ |

**Limitações conhecidas**
- Depende de o operador definir `OPERATOR_API_KEY` e chamar o endpoint manualmente.

**Testes:** `OperatorOnboardingTest`.

---

## 9. Alertas e e-mail

- **Outbox transacional** (V2 `email_outbox`, pacote `notification/`): `EmailOutboxService.enqueue` exige transação ativa. O `EmailDispatcher` roda a cada 10 s, envia em lotes de 20 (com `lockDue`, contra duplicidade entre instâncias) e faz backoff exponencial de 1, 2, 4… minutos, até 5 tentativas, depois `FAILED`. Envio por SMTP; templates no código (`EmailTemplate`, hoje só `USER_INVITE`).
- **Job de atrasados:** ver [Financeiro](#5-financeiro).

| Item | Status |
|---|---|
| Outbox, dispatcher, retry | ✅ |
| Gatilhos que usem a outbox | 🟡 **nada enfileira e-mail hoje**; o convite usa o Clerk |
| Alertas de vencimento de contrato (30/60/90 dias) | ✅ (backend) · 👁️ (telas), **só na tela, sem e-mail** (IMOB-28); ver [Contratos](#4-contratos) |
| Alertas de parcelas a vencer | ⬜ |
| Serviço de notificações separado com RabbitMQ | ⬜ desenhado, não iniciado |

**Testes:** `EmailOutboxTest`.

---

## 10. Multi-tenancy, autenticação e segurança

- **Autenticação:** `config/ClerkJwtAuthenticationFilter.java` valida o JWT do Clerk (RS256, JWKS, issuer e `azp`), lê `publicMetadata.tenantId` e `role` e resolve o usuário local por `clerk_user_id`. Usuário inexistente, inativo ou com tenant divergente do token não é autenticado.
- **Isolamento:** toda consulta filtra pelo `tenant_id` do contexto (nunca do corpo); ids do corpo são validados contra o tenant. `TenantIsolationTest` cobre vazamento entre tenants.
- **Autorização:** `@PreAuthorize` por endpoint, mais regras por dono nos services (leads e contratos).
- **Erros** (`GlobalExceptionHandler`): corpo `{error, code}`. 400 `VALIDATION_ERROR`/`DATA_INTEGRITY_VIOLATION`, 403 `FORBIDDEN`, 404, 409 `DUPLICATE_RESOURCE` e conflitos de negócio, 413, 415, 422 (regras de negócio), 502 `STORAGE_UNAVAILABLE` e 500 `INTERNAL_ERROR`.
- **Demais:** CORS por origem configurável, CSRF desligado (API stateless), `/actuator/health` e Swagger públicos, log de requisição com `requestId`.
- **Dados:** PKs UUID nas tabelas de negócio, índices por `tenant_id`, `(tenant_id, status)` e nas FKs (V5), auditoria `created_at`/`updated_at`. `DevDataSeeder` (só no perfil dev) cria dados de exemplo.

**Limitações conhecidas**
- Corretor com **login real no Clerk** não foi testado (depende de `publicMetadata.role` no token de sessão).
- `property_photos` não tem chave primária e não há `CHECK`s de valores e datas (a "migration B", ainda sem card).
- Sem rate limiting. O Swagger é público, e o perfil de produção não foi verificado quanto a isso.

---

## 11. Frontend transversal

- Autenticação e proteção de rotas em `middleware.ts` (Clerk); `/sign-in` e `/sign-up`.
- `lib/permissions.ts` espelha o backend (`canManageContracts`, `canReadContracts`, `canManagePhotos`, `canAccessPath`). O menu e os atalhos são filtrados por papel, hoje só para as rotas de contratos.
- Layout: sidebar, cabeçalho, navegação móvel, tema claro/escuro, página inicial "Resumo" com atalhos, financeiro do mês e, para ADMIN e FINANCEIRO, locações vencendo em 30/60/90 dias.
- Componentes próprios em `components/ui` (botão, card, diálogo, confirmação, selects, estados vazio/erro/carregando, badges).
- **Máscaras** (IMOB-45), em `lib/masks.ts` e `components/ui/masked-input.tsx`: R$ (imóvel, contrato, lançamentos), CPF/CNPJ com validação de dígitos e telefone.
- Cliente HTTP `lib/api.ts` (token do Clerk, erro tipado `{error, code}`) e hooks por módulo em `hooks/`.

**Limitações conhecidas**
- Não há `app/error.tsx` nem `app/not-found.tsx`: falha de fetch fora dos componentes cai na tela de erro padrão.
- Máscaras: sem verificação visual (digitar, colar, editar registro salvo); backspace sobre um caractere da máscara só apaga o dígito na tecla seguinte.
- Verificações em 375px pendentes (contratos, imóveis, financeiro, comissões).

---

## 12. Testes e CI

- **Backend:** 28 classes `*Test` (Testcontainers e Flyway via `IntegrationTestBase`, JWKS local, `@MockBean S3Client`); 186 testes passando no PR do IMOB-28.
- **Frontend:** 14 arquivos Vitest; 93 testes no PR do IMOB-28. Cobrem contratos, fotos, permissões, máscaras, navegação e leads. **Não há testes** de financeiro, comissões, visitas, usuários nem da lista e do formulário de imóveis.
- **CI:** `.github/workflows/backend-ci.yml` (Java 21, Postgres, `mvn test` com relatório) em push e PR para `main` e `develop`.
- **O frontend não tem CI ativo:** o workflow em `imob-erp-frontend/.github/workflows/ci.yml` é ignorado, porque o GitHub só lê `.github/workflows` da raiz. Lint, build e Vitest do frontend não rodam nos PRs.
- Sem testes end-to-end.

---

## Roadmap

O que ainda **não** existe (cards do Jira, projeto IMOB):

| Card | Assunto |
|---|---|
| IMOB-24 | Visitas: filtros e regra de imóvel vendido |
| IMOB-26 | Onboarding self-serve e webhook do Clerk |
| IMOB-33 | Encerrar/cancelar contrato revertendo imóvel e parcelas |
| IMOB-34 | Kanban de leads com drag and drop, confirmação e toast |
| IMOB-46 | Campos específicos por tipo de imóvel |
| IMOB-47 / IMOB-48 | Adicionar visita ao Google Agenda (link/.ics) e integração real com o Google Calendar |
| IMOB-30 / 31 / 32 | Spikes de validação das telas de financeiro, comissões e imóveis |
| IMOB-42 | Health de e-mail derrubando `/actuator/health` no dev |
| — | Migration B do banco (PK de `property_photos`, `CHECK`s), sem card ainda |
