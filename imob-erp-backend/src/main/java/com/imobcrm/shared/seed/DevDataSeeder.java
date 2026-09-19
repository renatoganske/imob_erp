package com.imobcrm.shared.seed;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Base minima de dados para desenvolvimento e testes manuais. So roda com app.seed.enabled=true
 * (ligado no perfil dev, nunca em prod/test) e e idempotente: se os corretores de seed ja existem, nao faz nada.
 *
 * Os dados entram no tenant app.seed.tenant-slug (criado se nao existir). Os usuarios de seed usam clerk ids
 * ficticios (seed_*) e nao conseguem fazer login; para logar, use um usuario Clerk ja vinculado ao tenant
 * (ou informe app.seed.admin-clerk-id para o seed criar o admin).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DevDataSeeder implements ApplicationRunner {

    static final String MARKER_CLERK_ID = "seed_corretor_1";

    private final JdbcTemplate jdbc;
    private final String tenantSlug;
    private final String adminClerkId;

    public DevDataSeeder(JdbcTemplate jdbc,
                         @Value("${app.seed.tenant-slug:imobiliaria-dev}") String tenantSlug,
                         @Value("${app.seed.admin-clerk-id:}") String adminClerkId) {
        this.jdbc = jdbc;
        this.tenantSlug = tenantSlug;
        this.adminClerkId = adminClerkId;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed();
    }

    @Transactional
    public boolean seed() {
        UUID tenant = findOrCreateTenant();
        Integer already = jdbc.queryForObject(
                "SELECT count(*) FROM users WHERE clerk_user_id = ?", Integer.class, MARKER_CLERK_ID);
        if (already != null && already > 0) {
            log.info("Seed de desenvolvimento ja aplicado (tenant {}); nada a fazer", tenantSlug);
            return false;
        }

        if (!adminClerkId.isBlank()) {
            user(tenant, adminClerkId, "Admin Seed", "admin.seed@example.com", "ADMIN", null);
        }
        UUID carlos = user(tenant, MARKER_CLERK_ID, "Carlos Corretor", "carlos.seed@example.com", "CORRETOR", "3.00");
        UUID ana = user(tenant, "seed_corretor_2", "Ana Corretora", "ana.seed@example.com", "CORRETOR", "4.00");
        UUID bruno = user(tenant, "seed_corretor_3", "Bruno Corretor", "bruno.seed@example.com", "CORRETOR", "5.00");
        user(tenant, "seed_financeiro_1", "Fernanda Financeiro", "fernanda.seed@example.com", "FINANCEIRO", null);

        UUID apto = property(tenant, "APARTAMENTO", "Apartamento 3 quartos no Batel", "Rua Bispo Dom José, 100", "Batel",
                "850000", "95", 3, 2, 2, "VENDIDO", "VENDA");
        UUID casa = property(tenant, "CASA", "Casa com quintal no Bacacheri", "Rua das Flores, 45", "Bacacheri",
                "1200000", "220", 4, 3, 3, "DISPONIVEL", "VENDA");
        UUID kitnet = property(tenant, "APARTAMENTO", "Kitnet mobiliada no Centro", "Rua XV de Novembro, 300", "Centro",
                "2200", "32", 1, 1, 0, "ALUGADO", "ALUGUEL");
        UUID sala = property(tenant, "COMERCIAL", "Sala comercial no Água Verde", "Av. República Argentina, 1500", "Água Verde",
                "3500", "48", null, 1, 1, "DISPONIVEL", "ALUGUEL");
        UUID terreno = property(tenant, "TERRENO", "Terreno em condomínio fechado", "Estrada da Graciosa, km 8", "Santa Felicidade",
                "430000", "600", null, null, null, "RESERVADO", "VENDA");
        UUID cobertura = property(tenant, "APARTAMENTO", "Cobertura duplex", "Rua Comendador Araújo, 700", "Batel",
                "1850000", "180", 3, 3, 3, "DISPONIVEL", "AMBOS");

        UUID leadCompra = lead(tenant, carlos, "Marcos Oliveira", "41999110001", "marcos@example.com", "INDICACAO", "FECHADO",
                "Comprou o apartamento do Batel", apto);
        UUID leadLocacao = lead(tenant, ana, "Juliana Prado", "41999110002", "juliana@example.com", "SITE", "FECHADO",
                "Alugou a kitnet do Centro", kitnet);
        UUID leadVisita = lead(tenant, carlos, "Rafael Souza", "41999110003", null, "WHATSAPP", "VISITA_AGENDADA",
                "Quer ver a casa no fim de semana", casa);
        lead(tenant, ana, "Patrícia Lima", "41999110004", "patricia@example.com", "PORTAL_ZAP", "PROPOSTA",
                "Proposta de R$ 410 mil no terreno", terreno);
        lead(tenant, bruno, "Diego Ferreira", "41999110005", null, "PORTAL_VIVAREAL", "EM_ATENDIMENTO",
                "Procura sala comercial", sala);
        lead(tenant, bruno, "Camila Rocha", "41999110006", "camila@example.com", "SITE", "NOVO", null, cobertura);
        lead(tenant, carlos, "Eduardo Martins", "41999110007", null, "OUTRO", "NOVO", null, null);
        lead(tenant, ana, "Larissa Gomes", "41999110008", "larissa@example.com", "WHATSAPP", "PERDIDO",
                "Desistiu: financiamento negado", null);

        visit(tenant, leadVisita, casa, carlos, OffsetDateTime.now().plusDays(3), "AGENDADA", null);
        visit(tenant, leadCompra, apto, carlos, OffsetDateTime.now().minusDays(40), "REALIZADA", "Gostou, seguiu para proposta");
        visit(tenant, leadLocacao, kitnet, ana, OffsetDateTime.now().minusDays(20), "REALIZADA", "Fechou locação");
        visit(tenant, leadVisita, cobertura, carlos, OffsetDateTime.now().minusDays(2), "CANCELADA", "Cliente remarcou");

        LocalDate today = LocalDate.now();
        UUID venda = contract(tenant, leadCompra, apto, carlos, "COMPRA_VENDA", "ATIVO", "850000", today.minusDays(30), null, null,
                "Marcos Oliveira", "111.444.777-35", "Helena Duarte", "529.982.247-25");
        UUID locacao = contract(tenant, leadLocacao, kitnet, ana, "LOCACAO", "ATIVO", "2200", today.minusMonths(2), today.plusMonths(10), "IGPM",
                "Juliana Prado", "390.533.447-05", "Roberto Nunes", "168.995.350-09");
        contract(tenant, leadVisita, casa, carlos, "COMPRA_VENDA", "RASCUNHO", "1200000", today.plusDays(15), null, null,
                "Rafael Souza", "295.379.955-93", "Sérgio Barros", "746.971.314-01");

        commission(tenant, venda, carlos, "3.00", "850000", "25500.00", "PENDENTE", null);
        commission(tenant, locacao, ana, "4.00", "2200", "88.00", "PAGO", today.minusDays(20));

        entry(tenant, venda, "RECEITA", "PARCELA_VENDA", "Sinal da venda do apartamento", "85000", today.minusDays(28), today.minusDays(28), "PAGO");
        entry(tenant, venda, "RECEITA", "PARCELA_VENDA", "Parcela 1 da venda do apartamento", "76500", today.plusDays(2), null, "PENDENTE");
        entry(tenant, locacao, "RECEITA", "ALUGUEL", "Aluguel kitnet - mês 1", "2200", today.minusMonths(2), today.minusMonths(2), "PAGO");
        entry(tenant, locacao, "RECEITA", "ALUGUEL", "Aluguel kitnet - mês 2", "2200", today.minusMonths(1), null, "ATRASADO");
        entry(tenant, locacao, "RECEITA", "ALUGUEL", "Aluguel kitnet - mês 3", "2200", today.plusDays(5), null, "PENDENTE");
        entry(tenant, locacao, "DESPESA", "REPASSE_PROPRIETARIO", "Repasse ao proprietário - mês 1", "1900", today.minusMonths(2).plusDays(5), today.minusMonths(2).plusDays(5), "PAGO");
        entry(tenant, null, "DESPESA", "DESPESA_OPERACIONAL", "Aluguel do escritório", "4500", today.plusDays(10), null, "PENDENTE");

        log.info("Seed de desenvolvimento aplicado no tenant {}", tenantSlug);
        return true;
    }

    private UUID findOrCreateTenant() {
        return jdbc.query("SELECT id FROM tenants WHERE slug = ?", (rs, i) -> (UUID) rs.getObject("id"), tenantSlug)
                .stream().findFirst().orElseGet(() -> {
                    UUID id = UUID.randomUUID();
                    jdbc.update("INSERT INTO tenants (id, name, slug, plan) VALUES (?, ?, ?, 'BASIC')", id, "Imobiliária Demo", tenantSlug);
                    return id;
                });
    }

    private UUID user(UUID tenant, String clerkId, String name, String email, String role, String commissionRate) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, tenant_id, clerk_user_id, name, email, role, commission_rate, active) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, TRUE)",
                id, tenant, clerkId, name, email, role, commissionRate == null ? null : new BigDecimal(commissionRate));
        return id;
    }

    private UUID property(UUID tenant, String type, String title, String address, String neighborhood, String price,
                          String area, Integer bedrooms, Integer bathrooms, Integer parking, String status, String purpose) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO properties (id, tenant_id, type, title, address, neighborhood, city, price, area, "
                        + "bedrooms, bathrooms, parking_spots, status, purpose) VALUES (?, ?, ?, ?, ?, ?, 'Curitiba', ?, ?, ?, ?, ?, ?, ?)",
                id, tenant, type, title, address, neighborhood, new BigDecimal(price), new BigDecimal(area),
                bedrooms, bathrooms, parking, status, purpose);
        return id;
    }

    private UUID lead(UUID tenant, UUID agent, String name, String phone, String email, String source, String stage,
                      String notes, UUID property) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO leads (id, tenant_id, assigned_to, name, phone, email, source, stage, notes) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", id, tenant, agent, name, phone, email, source, stage, notes);
        if (property != null) {
            jdbc.update("INSERT INTO lead_property (lead_id, property_id) VALUES (?, ?)", id, property);
        }
        return id;
    }

    private void visit(UUID tenant, UUID lead, UUID property, UUID agent, OffsetDateTime at, String status, String result) {
        jdbc.update("INSERT INTO visits (id, tenant_id, lead_id, property_id, agent_id, scheduled_at, status, result) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)", UUID.randomUUID(), tenant, lead, property, agent, at, status, result);
    }

    private UUID contract(UUID tenant, UUID lead, UUID property, UUID agent, String type, String status, String value,
                          LocalDate start, LocalDate end, String adjustmentIndex,
                          String buyerName, String buyerDocument, String ownerName, String ownerDocument) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO contracts (id, tenant_id, lead_id, property_id, agent_id, type, status, value, signed_at, "
                        + "start_date, end_date, adjustment_index, buyer_name, buyer_document, owner_name, owner_document) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, tenant, lead, property, agent, type, status, new BigDecimal(value),
                "ATIVO".equals(status) ? start : null, start, end, adjustmentIndex,
                buyerName, buyerDocument, ownerName, ownerDocument);
        return id;
    }

    private void commission(UUID tenant, UUID contract, UUID agent, String rate, String base, String value,
                            String status, LocalDate paidAt) {
        jdbc.update("INSERT INTO commissions (id, tenant_id, contract_id, agent_id, rate, base_value, value, status, paid_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), tenant, contract, agent, new BigDecimal(rate), new BigDecimal(base), new BigDecimal(value), status, paidAt);
    }

    private void entry(UUID tenant, UUID contract, String type, String category, String description, String value,
                       LocalDate dueDate, LocalDate paidAt, String status) {
        jdbc.update("INSERT INTO financial_entries (id, tenant_id, contract_id, type, category, description, value, due_date, paid_at, status) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), tenant, contract, type, category, description, new BigDecimal(value), dueDate, paidAt, status);
    }
}
