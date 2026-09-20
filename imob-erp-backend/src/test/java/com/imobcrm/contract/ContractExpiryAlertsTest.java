package com.imobcrm.contract;

import com.imobcrm.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Alertas de vencimento (IMOB-28): somente locacoes ATIVAS com end_date de hoje ate hoje + N dias, com o mesmo
 * criterio no filtro {@code expiringInDays} e no resumo 30/60/90 do dashboard. Datas sao relativas a hoje em Brasilia.
 */
class ContractExpiryAlertsTest extends IntegrationTestBase {

    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

    private static UUID tenantId, corretorAId, corretorBId;
    private static UUID in10, in45, in80, endsToday, in30Exactly;
    // fora de qualquer janela ou de outro tipo/status/tenant: nunca devem aparecer
    private static UUID in91, yesterday, noEndDate, sale, draft, ended, foreignTenant;

    @BeforeAll
    static void seed(@Autowired JdbcTemplate jdbc) {
        if (tenantId != null) {
            return;
        }
        tenantId = UUID.randomUUID();
        corretorAId = UUID.randomUUID();
        corretorBId = UUID.randomUUID();
        jdbc.update("INSERT INTO tenants (id, name, slug, plan) VALUES (?, 'Imobiliaria Vencimentos', 'imob-expiry-alerts', 'BASIC')", tenantId);
        insertUser(jdbc, tenantId, corretorAId, "ex_corretor_a", "CORRETOR");
        insertUser(jdbc, tenantId, corretorBId, "ex_corretor_b", "CORRETOR");
        insertUser(jdbc, tenantId, UUID.randomUUID(), "ex_admin", "ADMIN");
        insertUser(jdbc, tenantId, UUID.randomUUID(), "ex_financeiro", "FINANCEIRO");
        UUID property = insertProperty(jdbc, tenantId);

        in10 = insertContract(jdbc, tenantId, property, corretorAId, "LOCACAO", "ATIVO", TODAY.plusDays(10));
        in45 = insertContract(jdbc, tenantId, property, corretorBId, "LOCACAO", "ATIVO", TODAY.plusDays(45));
        in80 = insertContract(jdbc, tenantId, property, corretorAId, "LOCACAO", "ATIVO", TODAY.plusDays(80));
        endsToday = insertContract(jdbc, tenantId, property, corretorAId, "LOCACAO", "ATIVO", TODAY);
        in30Exactly = insertContract(jdbc, tenantId, property, corretorAId, "LOCACAO", "ATIVO", TODAY.plusDays(30));

        in91 = insertContract(jdbc, tenantId, property, corretorAId, "LOCACAO", "ATIVO", TODAY.plusDays(91));
        yesterday = insertContract(jdbc, tenantId, property, corretorAId, "LOCACAO", "ATIVO", TODAY.minusDays(1));
        noEndDate = insertContract(jdbc, tenantId, property, corretorAId, "LOCACAO", "ATIVO", null);
        sale = insertContract(jdbc, tenantId, property, corretorAId, "COMPRA_VENDA", "ATIVO", TODAY.plusDays(10));
        draft = insertContract(jdbc, tenantId, property, corretorAId, "LOCACAO", "RASCUNHO", TODAY.plusDays(10));
        ended = insertContract(jdbc, tenantId, property, corretorAId, "LOCACAO", "ENCERRADO", TODAY.plusDays(10));

        UUID otherTenant = UUID.randomUUID();
        jdbc.update("INSERT INTO tenants (id, name, slug, plan) VALUES (?, 'Outra', 'imob-expiry-foreign', 'BASIC')", otherTenant);
        UUID otherAgent = UUID.randomUUID();
        insertUser(jdbc, otherTenant, otherAgent, "ex_foreign_corretor", "CORRETOR");
        foreignTenant = insertContract(jdbc, otherTenant, insertProperty(jdbc, otherTenant), otherAgent, "LOCACAO", "ATIVO", TODAY.plusDays(10));
    }

    private static void insertUser(JdbcTemplate jdbc, UUID tenant, UUID id, String clerkId, String role) {
        jdbc.update("INSERT INTO users (id, tenant_id, clerk_user_id, name, email, role, commission_rate) "
                + "VALUES (?, ?, ?, ?, ?, ?, 5.00)", id, tenant, clerkId, clerkId, clerkId + "@x.com", role);
    }

    private static UUID insertProperty(JdbcTemplate jdbc, UUID tenant) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO properties (id, tenant_id, type, title, address, city, price, status, purpose) "
                + "VALUES (?, ?, 'APARTAMENTO', 'Imovel', 'Rua 1', 'Curitiba', 2000, 'ALUGADO', 'ALUGUEL')", id, tenant);
        return id;
    }

    private static UUID insertContract(JdbcTemplate jdbc, UUID tenant, UUID property, UUID agent,
                                       String type, String status, LocalDate endDate) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO contracts (id, tenant_id, property_id, agent_id, type, status, value, start_date, end_date, "
                + "buyer_name, buyer_document, owner_name, owner_document) "
                + "VALUES (?, ?, ?, ?, ?, ?, 2000, current_date, ?, 'Locatario', '111', 'Dono', '222')",
                id, tenant, property, agent, type, status, endDate);
        return id;
    }

    private MockHttpServletRequestBuilder as(String clerkId, String role, MockHttpServletRequestBuilder r) throws Exception {
        return withToken(token(clerkId, tenantId, role), r);
    }

    private MockHttpServletRequestBuilder asFinanceiro(MockHttpServletRequestBuilder r) throws Exception {
        return as("ex_financeiro", "FINANCEIRO", r);
    }

    @Test
    void filterByThirtyDaysReturnsOnlyActiveRentalsEndingInWindowSoonestFirst() throws Exception {
        mvc.perform(asFinanceiro(get("/api/v1/contracts").param("expiringInDays", "30").param("size", "100")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", contains(
                        endsToday.toString(), in10.toString(), in30Exactly.toString())));
    }

    @Test
    void filterWidensWithTheWindow() throws Exception {
        mvc.perform(asFinanceiro(get("/api/v1/contracts").param("expiringInDays", "60").param("size", "100")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", contains(
                        endsToday.toString(), in10.toString(), in30Exactly.toString(), in45.toString())));
        mvc.perform(asFinanceiro(get("/api/v1/contracts").param("expiringInDays", "90").param("size", "100")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", contains(
                        endsToday.toString(), in10.toString(), in30Exactly.toString(), in45.toString(), in80.toString())));
    }

    @Test
    void filterNeverReturnsSalesDraftsEndedNoEndDateExpiredOrOtherTenants() throws Exception {
        mvc.perform(asFinanceiro(get("/api/v1/contracts").param("expiringInDays", "365").param("size", "100")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(6)))
                .andExpect(jsonPath("$.content[*].id", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItems(
                        sale.toString(), draft.toString(), ended.toString(), noEndDate.toString(),
                        yesterday.toString(), foreignTenant.toString()))));
    }

    @Test
    void contractEndingAfterTheWindowAppearsOnlyInTheWiderOne() throws Exception {
        mvc.perform(asFinanceiro(get("/api/v1/contracts").param("expiringInDays", "90").param("size", "100")))
                .andExpect(jsonPath("$.content[*].id", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(in91.toString()))));
        mvc.perform(asFinanceiro(get("/api/v1/contracts").param("expiringInDays", "91").param("size", "100")))
                .andExpect(jsonPath("$.content[*].id", org.hamcrest.Matchers.hasItem(in91.toString())));
    }

    @Test
    void corretorFilterIsStillScopedToOwnContracts() throws Exception {
        mvc.perform(as("ex_corretor_a", "CORRETOR", get("/api/v1/contracts").param("expiringInDays", "90").param("size", "100")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", contains(
                        endsToday.toString(), in10.toString(), in30Exactly.toString(), in80.toString())));
    }

    @Test
    void filterRejectsConflictingStatusOrTypeAndOutOfRangeDays() throws Exception {
        mvc.perform(asFinanceiro(get("/api/v1/contracts").param("expiringInDays", "30").param("status", "ENCERRADO")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_EXPIRY_FILTER"));
        mvc.perform(asFinanceiro(get("/api/v1/contracts").param("expiringInDays", "30").param("type", "COMPRA_VENDA")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_EXPIRY_FILTER"));
        mvc.perform(asFinanceiro(get("/api/v1/contracts").param("expiringInDays", "0")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_EXPIRY_WINDOW"));
    }

    @Test
    void summaryCountsAreCumulativeAndMatchTheFilterTotals() throws Exception {
        mvc.perform(asFinanceiro(get("/api/v1/contracts/expiring-summary")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.within30Days").value(3))
                .andExpect(jsonPath("$.within60Days").value(4))
                .andExpect(jsonPath("$.within90Days").value(5));
        mvc.perform(as("ex_admin", "ADMIN", get("/api/v1/contracts/expiring-summary")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.within90Days").value(5));
    }

    @Test
    void summaryIsForbiddenToCorretor() throws Exception {
        mvc.perform(as("ex_corretor_a", "CORRETOR", get("/api/v1/contracts/expiring-summary")))
                .andExpect(status().isForbidden());
    }
}
