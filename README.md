# Imob ERP

ERP + CRM SaaS multi-tenant para pequenas imobiliárias: imóveis, leads, visitas, contratos, financeiro e comissões.

Fluxo principal: **Lead → fechamento → contrato → parcelas financeiras → comissão do corretor.**

| Pasta | Conteúdo |
|---|---|
| `imob-erp-backend/` | API Spring Boot 3 / Java 21 (ver [`imob-erp-backend/README.md`](imob-erp-backend/README.md)) |
| `imob-erp-frontend/` | Next.js 14 + Clerk |
| `docs/` | Documentação |

## Documentação

- [`docs/projeto.md`](docs/projeto.md): visão, stack, como rodar, variáveis de ambiente, papéis, testes, CI e fluxo de trabalho.
- [`docs/funcionalidades.md`](docs/funcionalidades.md): o que está implementado, por módulo, com status e limitações.
- [`docs/architecture.md`](docs/architecture.md): estrutura de pacotes e regras de camada do backend.

## Início rápido

```powershell
docker compose up -d                       # Postgres (5434) e Mailpit (8025)
cd imob-erp-backend; ./mvnw spring-boot:run # API em :8080 (JDK 21)
cd imob-erp-frontend; npm ci; npm run dev   # app em :3000
```

Antes de rodar, configure as variáveis de ambiente (exemplos em `.env.example` de cada pasta); o backend não lê o `.env`, ver [`docs/projeto.md`](docs/projeto.md#4-rodando-localmente).
