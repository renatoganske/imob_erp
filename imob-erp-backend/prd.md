# PRD — ERP para Pequenas Imobiliárias
**Versão:** 0.1 (MVP)  
**Status:** Rascunho  
**Última atualização:** Março 2026

---

## 1. Visão Geral

### Problema
Imobiliárias pequenas (2–10 corretores) operam com sistemas fragmentados: um CRM genérico, planilhas de controle financeiro, contratos gerenciados por e-mail e comissões calculadas manualmente. O resultado é retrabalho, erros de pagamento e falta de visibilidade do negócio como um todo.

### Solução
Um ERP vertical para imobiliárias pequenas, integrado ao CRM, cobrindo os 4 pilares operacionais do negócio: imóveis, contratos, financeiro e comissões de corretores. Uma única plataforma que conecta o lead ao contrato e o contrato ao pagamento.

### Proposta de valor
> "Do lead ao recebimento, sem sair do sistema."

### Público-alvo
Imobiliárias com 2 a 10 corretores. Decisor de compra: dono da imobiliária. Usuários: corretores e financeiro (pode ser o próprio dono).

---

## 2. Produtos e sua relação

O sistema é composto por dois produtos integrados:

| Produto | Foco | Usuário principal |
|---|---|---|
| **CRM** | Captação e gestão de leads até o fechamento | Corretor |
| **ERP** | Gestão operacional e financeira pós-fechamento | Dono / Financeiro |

O CRM alimenta o ERP: quando um lead é marcado como "Fechado" no CRM, o ERP é acionado para criar o contrato e registrar as obrigações financeiras.

---

## 3. Módulos do ERP — Escopo do MVP

### 3.1 Gestão de Imóveis
- Cadastro completo do imóvel (tipo, endereço, valor, área, fotos, descrição)
- Status do imóvel: Disponível / Reservado / Vendido / Alugado
- Histórico de negociações por imóvel
- Filtros por tipo, status, faixa de valor, bairro

> **Integração com CRM:** imóvel cadastrado no ERP aparece disponível para associação no CRM.

### 3.2 Gestão de Contratos
- Criação de contrato vinculado a: imóvel + lead (comprador/locatário) + corretor responsável
- Tipos de contrato: Compra e Venda / Locação
- Campos essenciais: valor, data de assinatura, vigência (para locação), índice de reajuste (IGPM/IPCA), partes envolvidas
- Status do contrato: Rascunho / Ativo / Encerrado / Cancelado
- Upload de documento (PDF do contrato assinado)
- Alertas de vencimento de contrato de locação (30/60/90 dias antes)

> **Fora do MVP:** assinatura eletrônica, geração automática de documento de contrato.

### 3.3 Financeiro (Contas a Pagar / Receber)
- **Contas a receber:** aluguel mensal, parcelas de venda, taxas de administração
- **Contas a pagar:** repasse ao proprietário, despesas operacionais da imobiliária
- Lançamento manual de receitas e despesas
- Status de cada lançamento: Pendente / Pago / Atrasado / Cancelado
- Geração automática de parcelas recorrentes (aluguel mensal) a partir do contrato
- Dashboard financeiro: saldo do mês, total a receber, total a pagar, inadimplência
- Filtro por período, categoria, status

> **Fora do MVP:** integração bancária (OFX/PIX), NF-e, conciliação automática.

### 3.4 RH / Comissões de Corretores
- Cadastro de corretores com taxa de comissão padrão (% sobre valor do negócio)
- Cálculo automático de comissão a partir do contrato fechado
- Status da comissão: Pendente / Pago / Parcelado
- Histórico de comissões por corretor
- Relatório simples: comissão total por corretor no período

> **Fora do MVP:** holerite, eSocial, FGTS, férias.

---

## 4. Módulos do CRM — Escopo do MVP

### 4.1 Pipeline de Leads
- Cadastro rápido: nome, telefone, e-mail (opcional), origem, imóvel de interesse
- Kanban: Novo → Em atendimento → Visita agendada → Proposta → Fechado → Perdido
- Atribuição de lead a corretor
- Anotações por lead

### 4.2 Agenda de Visitas
- Visita vinculada a lead + imóvel + corretor
- Status: Agendada / Realizada / Cancelada
- Registro de resultado pós-visita

---

## 5. Integrações entre CRM e ERP

| Gatilho (CRM) | Ação (ERP) |
|---|---|
| Lead movido para "Fechado" | Cria rascunho de contrato com dados do lead + imóvel |
| Contrato criado | Atualiza status do imóvel para Reservado/Vendido/Alugado |
| Contrato ativado | Gera parcelas financeiras automaticamente |
| Contrato ativado | Calcula e registra comissão do corretor |

---

## 6. Fora do Escopo do MVP

- Site da imobiliária (fase 2)
- Integração com portais (ZAP, Viva Real) — fase 2
- Assinatura eletrônica de contratos — fase 2
- Integração bancária / PIX — fase 2
- NF-e / NFSe — fase 2
- eSocial / holerite — fase 2
- App mobile nativo — fase 2
- Relatórios avançados / BI — fase 2

---

## 7. Personas

### Carlos — Dono da imobiliária
- 42 anos, 6 corretores
- Quer saber: quanto vai entrar esse mês, quais contratos vencem, qual corretor está performando
- Dor: informação espalhada em planilhas, WhatsApp e e-mail

### Ana — Corretora
- 34 anos, foco em atendimento
- Quer cadastrar leads rápido e não perder histórico
- Dor: sistema atual é lento e não conecta com o contrato

### Roberto — Financeiro (pode ser o próprio Carlos)
- Controla entradas e saídas manualmente em planilha
- Quer saber o que está atrasado e o que precisa ser pago essa semana
- Dor: não tem visão consolidada do caixa

---

## 8. Requisitos Não-Funcionais

| Requisito | Critério |
|---|---|
| Performance | Página carrega em < 2s em conexão 4G |
| Disponibilidade | ≥ 99% uptime |
| Segurança | Dados isolados por tenant; autenticação via Clerk |
| Responsividade | Funcional em mobile (mínimo 375px) |
| LGPD | Dados pessoais armazenados no Brasil ou com garantias equivalentes |

---

## 9. Modelo de Negócio (hipótese)

| Plano | Perfil | Preço hipotético |
|---|---|---|
| Starter | Até 2 corretores | R$ 147/mês |
| Pro | Até 10 corretores | R$ 297/mês |
| Pro + Site | Até 10 corretores + site próprio | R$ 397/mês |

---

## 10. Estimativa de Custo de Infra

| Fase | Clientes | Custo infra/mês |
|---|---|---|
| Desenvolvimento | 0 | ~R$ 35 |
| Beta | 1–5 imobiliárias | ~R$ 50–80 |
| Produto no ar | 10–20 imobiliárias | ~R$ 100–180 |

---

## 11. Critérios de Sucesso do MVP

- [ ] 3 imobiliárias pagantes após 60 dias de trial
- [ ] Fluxo completo (lead → contrato → financeiro) funcionando sem intervenção manual
- [ ] NPS ≥ 30 após primeiro mês
- [ ] Churn < 20% no segundo mês
