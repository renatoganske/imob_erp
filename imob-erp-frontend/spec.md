# imob-erp-frontend

Frontend do ERP + CRM para pequenas imobiliárias. Aplicação web construída com Next.js 14 (App Router), TypeScript, Tailwind CSS e autenticação via Clerk.

---

## Visão Geral do Sistema

Interface do usuário do ERP + CRM para imobiliárias. Consome a API REST do [imob-erp-backend](https://github.com/renatoganske/imob-erp-backend) e oferece três perfis de acesso com visões distintas:

| Perfil | Acesso |
|---|---|
| **Admin** | Tudo — imóveis, leads, visitas, contratos, financeiro, comissões, usuários |
| **Corretor** | Imóveis, seus próprios leads e visitas, suas próprias comissões |
| **Financeiro** | Contratos, financeiro, comissões |

---

## Stack

| Camada | Tecnologia | Versão |
|---|---|---|
| Framework | Next.js (App Router) | 14.x |
| Linguagem | TypeScript | 5.x |
| Estilização | Tailwind CSS | 3.x |
| Componentes | shadcn/ui | — |
| Autenticação | Clerk | — |
| HTTP Client | fetch nativo / axios | — |
| Deploy | Vercel | — |

---

## Estrutura de Pastas

```
src/
├── app/
│   ├── (auth)/
│   │   ├── sign-in/
│   │   │   └── page.tsx               # Página de login (Clerk)
│   │   └── sign-up/
│   │       └── page.tsx               # Página de cadastro (Clerk)
│   ├── (dashboard)/
│   │   ├── layout.tsx                 # Layout com sidebar + header
│   │   ├── page.tsx                   # Página inicial / resumo
│   │   ├── imoveis/
│   │   │   ├── page.tsx               # Listagem de imóveis
│   │   │   ├── novo/
│   │   │   │   └── page.tsx           # Formulário de cadastro
│   │   │   └── [id]/
│   │   │       ├── page.tsx           # Detalhe do imóvel
│   │   │       └── editar/
│   │   │           └── page.tsx       # Edição do imóvel
│   │   ├── leads/
│   │   │   └── page.tsx               # Kanban de leads
│   │   ├── visitas/
│   │   │   └── page.tsx               # Agenda de visitas
│   │   ├── contratos/
│   │   │   ├── page.tsx               # Listagem de contratos
│   │   │   ├── novo/
│   │   │   │   └── page.tsx           # Formulário de contrato
│   │   │   └── [id]/
│   │   │       └── page.tsx           # Detalhe do contrato
│   │   ├── financeiro/
│   │   │   ├── page.tsx               # Dashboard financeiro
│   │   │   ├── receber/
│   │   │   │   └── page.tsx           # Contas a receber
│   │   │   └── pagar/
│   │   │       └── page.tsx           # Contas a pagar
│   │   ├── comissoes/
│   │   │   └── page.tsx               # Comissões dos corretores
│   │   └── configuracoes/
│   │       └── usuarios/
│   │           └── page.tsx           # Gestão de usuários
│   ├── layout.tsx                     # Layout raiz com ClerkProvider
│   └── middleware.ts                  # Proteção de rotas via Clerk
├── components/
│   ├── ui/                            # Componentes base do shadcn/ui
│   │   ├── button.tsx
│   │   ├── input.tsx
│   │   ├── dialog.tsx
│   │   ├── badge.tsx
│   │   └── ...
│   ├── layout/
│   │   ├── Sidebar.tsx                # Menu lateral com navegação
│   │   └── Header.tsx                 # Cabeçalho com usuário logado
│   ├── imoveis/
│   │   ├── PropertyCard.tsx           # Card de imóvel na listagem
│   │   ├── PropertyForm.tsx           # Formulário de cadastro/edição
│   │   ├── PropertyFilters.tsx        # Filtros da listagem
│   │   └── PhotoUpload.tsx            # Upload de fotos
│   ├── leads/
│   │   ├── LeadKanban.tsx             # Board kanban
│   │   ├── LeadCard.tsx               # Card do lead no kanban
│   │   └── LeadModal.tsx              # Modal de detalhe/criação
│   ├── visitas/
│   │   ├── VisitList.tsx              # Lista de visitas por dia/semana
│   │   ├── VisitCard.tsx              # Card de visita
│   │   └── VisitModal.tsx             # Modal de agendamento/resultado
│   ├── contratos/
│   │   ├── ContractList.tsx
│   │   ├── ContractForm.tsx
│   │   └── ContractStatusBadge.tsx
│   ├── financeiro/
│   │   ├── FinancialDashboard.tsx     # Cards de resumo financeiro
│   │   ├── EntryList.tsx              # Lista de lançamentos
│   │   └── EntryForm.tsx              # Formulário de lançamento manual
│   └── comissoes/
│       ├── CommissionList.tsx
│       └── CommissionReport.tsx
├── lib/
│   ├── api.ts                         # Cliente HTTP base (fetch + auth header)
│   └── utils.ts                       # Utilitários (cn, formatters)
├── hooks/
│   ├── useProperties.ts               # Hooks de imóveis
│   ├── useLeads.ts                    # Hooks de leads
│   ├── useVisits.ts                   # Hooks de visitas
│   ├── useContracts.ts                # Hooks de contratos
│   ├── useFinancial.ts                # Hooks de financeiro
│   └── useCommissions.ts             # Hooks de comissões
└── types/
    ├── property.ts                    # Tipos de imóvel
    ├── lead.ts                        # Tipos de lead
    ├── visit.ts                       # Tipos de visita
    ├── contract.ts                    # Tipos de contrato
    ├── financial.ts                   # Tipos financeiros
    └── commission.ts                  # Tipos de comissão
```

---

## Pré-requisitos

- Node.js 20+
- npm ou pnpm
- Backend rodando localmente ou na Railway ([imob-erp-backend](https://github.com/renatoganske/imob-erp-backend))
- Conta no [Clerk](https://clerk.com) (gratuita) — o mesmo app usado no backend

---

## Configuração Local

### 1. Clone o repositório

```bash
git clone https://github.com/renatoganske/imob-erp-frontend.git
cd imob-erp-frontend
```

### 2. Instale as dependências

```bash
npm install
```

### 3. Configure as variáveis de ambiente

Crie um arquivo `.env.local` na raiz do projeto (nunca commitar):

```env
# Clerk — mesma conta usada no backend
NEXT_PUBLIC_CLERK_PUBLISHABLE_KEY=pk_test_...
CLERK_SECRET_KEY=sk_test_...

# Rotas do Clerk
NEXT_PUBLIC_CLERK_SIGN_IN_URL=/sign-in
NEXT_PUBLIC_CLERK_SIGN_UP_URL=/sign-up
NEXT_PUBLIC_CLERK_AFTER_SIGN_IN_URL=/
NEXT_PUBLIC_CLERK_AFTER_SIGN_UP_URL=/

# URL do backend
NEXT_PUBLIC_API_URL=http://localhost:8080
```

> **Como obter as chaves do Clerk:**
> 1. Acesse [dashboard.clerk.com](https://dashboard.clerk.com)
> 2. Selecione o mesmo app usado no backend
> 3. Vá em **API Keys** → copie `Publishable key` e `Secret key`

### 4. Execute a aplicação

```bash
npm run dev
```

A aplicação estará disponível em `http://localhost:3000`.

---

## Variáveis de Ambiente (referência completa)

| Variável | Descrição | Obrigatória |
|---|---|---|
| `NEXT_PUBLIC_CLERK_PUBLISHABLE_KEY` | Chave pública do Clerk (exposta no browser) | ✅ |
| `CLERK_SECRET_KEY` | Chave secreta do Clerk (apenas servidor) | ✅ |
| `NEXT_PUBLIC_CLERK_SIGN_IN_URL` | Rota da página de login | ✅ |
| `NEXT_PUBLIC_CLERK_SIGN_UP_URL` | Rota da página de cadastro | ✅ |
| `NEXT_PUBLIC_CLERK_AFTER_SIGN_IN_URL` | Redirecionamento após login | ✅ |
| `NEXT_PUBLIC_CLERK_AFTER_SIGN_UP_URL` | Redirecionamento após cadastro | ✅ |
| `NEXT_PUBLIC_API_URL` | URL base da API do backend | ✅ |

---

## Autenticação

A autenticação é gerenciada pelo **Clerk**. O fluxo funciona assim:

1. Usuário faz login na página `/sign-in` (gerenciada pelo Clerk)
2. Clerk retorna um JWT com `tenantId` e `role` em `publicMetadata`
3. O cliente HTTP (`lib/api.ts`) anexa o token em todas as requisições:

```ts
// lib/api.ts
const token = await getToken(); // hook do Clerk
fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/v1/...`, {
  headers: {
    Authorization: `Bearer ${token}`,
    'Content-Type': 'application/json',
  },
});
```

O `middleware.ts` protege todas as rotas do dashboard — usuário não autenticado é redirecionado para `/sign-in`.

---

## Proteção de Rotas por Perfil

Cada perfil vê apenas o que tem permissão:

| Rota | Admin | Corretor | Financeiro |
|---|---|---|---|
| `/imoveis` | ✅ | ✅ | ❌ |
| `/leads` | ✅ | ✅ (só seus) | ❌ |
| `/visitas` | ✅ | ✅ (só suas) | ❌ |
| `/contratos` | ✅ | ❌ | ✅ |
| `/financeiro` | ✅ | ❌ | ✅ |
| `/comissoes` | ✅ | ✅ (só suas) | ✅ |
| `/configuracoes/usuarios` | ✅ | ❌ | ❌ |

A verificação de perfil é feita no backend — o frontend esconde os itens do menu, mas o acesso real é bloqueado pela API.

---

## Módulos e Telas

### Imóveis
- Listagem paginada com filtros (tipo, status, faixa de preço, bairro)
- Cadastro e edição com upload de até 20 fotos
- Status com badge colorido: Disponível / Reservado / Vendido / Alugado

### Leads (CRM)
- Kanban com 6 colunas: Novo → Em atendimento → Visita agendada → Proposta → Fechado → Perdido
- Criação rápida de lead em menos de 3 cliques
- Drag-and-drop entre colunas
- Modal de detalhe com histórico de visitas e imóveis associados

### Visitas (CRM)
- Agenda com visão diária e semanal
- Agendamento vinculado a lead + imóvel + corretor
- Registro de resultado pós-visita

### Contratos (ERP)
- Listagem com alertas de vencimento (locação)
- Formulário completo para Compra e Venda e Locação
- Upload de PDF do contrato assinado
- Ativação com confirmação (ação irreversível)

### Financeiro (ERP)
- Dashboard: saldo do mês, a receber, a pagar, inadimplência
- Contas a receber e a pagar com filtros por período, categoria e status
- Lançamento manual de receitas e despesas
- Entradas atrasadas destacadas em vermelho

### Comissões (ERP)
- Listagem de comissões por corretor
- Filtros por corretor, status e período
- Relatório de total por corretor

---

## CI — GitHub Actions

O workflow de CI roda automaticamente em PRs e pushes para `main` e `develop`:

```bash
# O workflow faz:
# 1. npm install
# 2. npm run lint
# 3. npm run build
```

O arquivo está em `.github/workflows/ci.yml`.

---

## Deploy (Vercel)

O deploy é automático via integração do Vercel com o GitHub:

1. Push para `main` → Vercel detecta e faz deploy de produção
2. Push para outras branches → Vercel cria preview URL automática

Configure as variáveis de ambiente no painel da Vercel em **Settings → Environment Variables**.

---

## Convenções de Código

- **Branches:** `feature/nome-da-feature`, `fix/nome-do-bug`
- **Commits:** mensagens em português, prefixo convencional: `feat:`, `fix:`, `refactor:`, `style:`
- **PRs:** sempre para `develop`; `develop` → `main` apenas para releases
- **Componentes:** PascalCase — `PropertyCard.tsx`
- **Hooks:** camelCase prefixado com `use` — `useProperties.ts`
- **Tipos:** PascalCase no arquivo e nas interfaces — `Property`, `Lead`

---

## Repositórios relacionados

- **Backend:** [imob-erp-backend](https://github.com/renatoganske/imob-erp-backend)
