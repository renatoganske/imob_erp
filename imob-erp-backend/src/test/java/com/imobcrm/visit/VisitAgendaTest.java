package com.imobcrm.visit;

import com.imobcrm.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Agenda de visitas (IMOB-24), com o stack real: filtros por data e status, 422 para imovel vendido
 * e isolamento por corretor (corretor A x corretor B no mesmo tenant, mais Admin).
 */
class VisitAgendaTest extends IntegrationTestBase {

    private static UUID tenantId, adminId, corretorAId, corretorBId, leadId, availableId, soldId;

    @BeforeAll
    static void seed(@Autowired JdbcTemplate jdbc) {
        if (tenantId != null) {
            return;
        }
        tenantId = UUID.randomUUID();
        adminId = UUID.randomUUID();
        corretorAId = UUID.randomUUID();
        corretorBId = UUID.randomUUID();
        leadId = UUID.randomUUID();
        availableId = UUID.randomUUID();
        soldId = UUID.randomUUID();
        jdbc.update("INSERT INTO tenants (id, name, slug, plan) VALUES (?, 'Imobiliaria Visitas', 'imob-visits-agenda', 'BASIC')", tenantId);
        insertUser(jdbc, adminId, "visits_admin", "ADMIN");
        insertUser(jdbc, corretorAId, "visits_corretor_a", "CORRETOR");
        insertUser(jdbc, corretorBId, "visits_corretor_b", "CORRETOR");
        jdbc.update("INSERT INTO leads (id, tenant_id, assigned_to, name, phone, source, stage) "
                + "VALUES (?, ?, ?, 'Lead', '41999999999', 'SITE', 'NOVO')", leadId, tenantId, corretorAId);
        insertProperty(jdbc, availableId, "DISPONIVEL");
        insertProperty(jdbc, soldId, "VENDIDO");
    }

    private static void insertUser(JdbcTemplate jdbc, UUID id, String clerkId, String role) {
        jdbc.update("INSERT INTO users (id, tenant_id, clerk_user_id, name, email, role, commission_rate) "
                + "VALUES (?, ?, ?, ?, ?, ?, 5.00)", id, tenantId, clerkId, clerkId, clerkId + "@x.com", role);
    }

    private static void insertProperty(JdbcTemplate jdbc, UUID id, String status) {
        jdbc.update("INSERT INTO properties (id, tenant_id, type, title, address, city, price, status, purpose) "
                + "VALUES (?, ?, 'APARTAMENTO', 'Imovel', 'Rua 1', 'Curitiba', 500000, ?, 'VENDA')", id, tenantId, status);
    }

    private UUID newVisit(UUID agentId, String scheduledAt, String status) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO visits (id, tenant_id, lead_id, property_id, agent_id, scheduled_at, status) "
                + "VALUES (?, ?, ?, ?, ?, ?::timestamptz, ?)", id, tenantId, leadId, availableId, agentId, scheduledAt, status);
        return id;
    }

    private MockHttpServletRequestBuilder asCorretorA(MockHttpServletRequestBuilder r) throws Exception {
        return withToken(token("visits_corretor_a", tenantId, "CORRETOR"), r);
    }

    private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder r) throws Exception {
        return withToken(token("visits_admin", tenantId, "ADMIN"), r);
    }

    private String visitBody(UUID propertyId, UUID agentId) {
        return "{\"leadId\":\"" + leadId + "\",\"propertyId\":\"" + propertyId + "\",\"agentId\":\"" + agentId
                + "\",\"scheduledAt\":\"" + OffsetDateTime.now().plusDays(3) + "\"}";
    }

    // ---- filtros ----

    @Test
    void filtraPorStatus() throws Exception {
        UUID scheduled = newVisit(corretorBId, "2031-03-10T13:00:00-03:00", "AGENDADA");
        UUID done = newVisit(corretorBId, "2031-03-10T14:00:00-03:00", "REALIZADA");

        mvc.perform(asAdmin(get("/api/v1/visits").param("size", "200").param("status", "REALIZADA")
                        .param("from", "2031-03-10").param("to", "2031-03-10")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem(done.toString())))
                .andExpect(jsonPath("$.content[*].id", not(hasItem(scheduled.toString()))));
    }

    @Test
    void filtraPorPeriodoInclusivoNoFusoDeBrasilia() throws Exception {
        // 23:30 em Brasilia de 15/04 ja e 16/04 em UTC: precisa cair no dia 15.
        UUID lateNight = newVisit(corretorBId, "2031-04-15T23:30:00-03:00", "AGENDADA");
        UUID before = newVisit(corretorBId, "2031-04-14T23:59:00-03:00", "AGENDADA");
        UUID after = newVisit(corretorBId, "2031-04-16T00:00:00-03:00", "AGENDADA");

        mvc.perform(asAdmin(get("/api/v1/visits").param("size", "200").param("agentId", corretorBId.toString())
                        .param("from", "2031-04-15").param("to", "2031-04-15")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem(lateNight.toString())))
                .andExpect(jsonPath("$.content[*].id", not(hasItem(before.toString()))))
                .andExpect(jsonPath("$.content[*].id", not(hasItem(after.toString()))));
    }

    @Test
    void listaOrdenadaPorDataCrescente() throws Exception {
        UUID second = newVisit(corretorBId, "2031-05-02T10:00:00-03:00", "AGENDADA");
        UUID first = newVisit(corretorBId, "2031-05-01T10:00:00-03:00", "AGENDADA");

        mvc.perform(asAdmin(get("/api/v1/visits").param("size", "200").param("from", "2031-05-01").param("to", "2031-05-02")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", contains(first.toString(), second.toString())));
    }

    // ---- imovel vendido ----

    @Test
    void visitaParaImovelVendidoRetorna422() throws Exception {
        mvc.perform(asAdmin(post("/api/v1/visits").contentType(MediaType.APPLICATION_JSON)
                        .content(visitBody(soldId, corretorAId))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PROPERTY_SOLD"));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM visits WHERE property_id = ?", Integer.class, soldId));
    }

    @Test
    void visitaParaImovelDisponivelEAgendada() throws Exception {
        mvc.perform(asAdmin(post("/api/v1/visits").contentType(MediaType.APPLICATION_JSON)
                        .content(visitBody(availableId, corretorAId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AGENDADA"));
    }

    // ---- isolamento por corretor ----

    @Test
    void corretorListaApenasAsProprias_mesmoComFiltroAgentIdDeOutro() throws Exception {
        UUID own = newVisit(corretorAId, "2031-06-01T10:00:00-03:00", "AGENDADA");
        UUID foreign = newVisit(corretorBId, "2031-06-01T11:00:00-03:00", "AGENDADA");

        mvc.perform(asCorretorA(get("/api/v1/visits").param("size", "200")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem(own.toString())))
                .andExpect(jsonPath("$.content[*].id", not(hasItem(foreign.toString()))));
        mvc.perform(asCorretorA(get("/api/v1/visits").param("size", "200").param("agentId", corretorBId.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", not(hasItem(foreign.toString()))));
    }

    @Test
    void adminVeTodasAsVisitas() throws Exception {
        UUID a = newVisit(corretorAId, "2031-07-01T10:00:00-03:00", "AGENDADA");
        UUID b = newVisit(corretorBId, "2031-07-01T11:00:00-03:00", "AGENDADA");

        mvc.perform(asAdmin(get("/api/v1/visits").param("size", "200").param("from", "2031-07-01").param("to", "2031-07-01")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem(a.toString())))
                .andExpect(jsonPath("$.content[*].id", hasItem(b.toString())));
    }

    @Test
    void corretorNaoAcessaNemAlteraVisitaDeOutroCorretor() throws Exception {
        UUID foreign = newVisit(corretorBId, "2031-08-01T10:00:00-03:00", "AGENDADA");

        mvc.perform(asCorretorA(get("/api/v1/visits/" + foreign))).andExpect(status().isNotFound());
        mvc.perform(asCorretorA(patch("/api/v1/visits/" + foreign + "/status")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELADA\"}")))
                .andExpect(status().isNotFound());
        mvc.perform(asCorretorA(patch("/api/v1/visits/" + foreign + "/result")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"result\":\"Hack\"}")))
                .andExpect(status().isNotFound());

        assertEquals("AGENDADA", jdbc.queryForObject("SELECT status FROM visits WHERE id = ?", String.class, foreign));
        mvc.perform(asAdmin(get("/api/v1/visits/" + foreign))).andExpect(status().isOk());
    }

    @Test
    void corretorNaoAgendaParaOutroCorretor() throws Exception {
        mvc.perform(asCorretorA(post("/api/v1/visits").contentType(MediaType.APPLICATION_JSON)
                        .content(visitBody(availableId, corretorBId))))
                .andExpect(status().isForbidden());
        mvc.perform(asCorretorA(post("/api/v1/visits").contentType(MediaType.APPLICATION_JSON)
                        .content(visitBody(availableId, corretorAId))))
                .andExpect(status().isCreated());
    }

    @Test
    void corretorAlteraStatusEResultadoDaPropriaVisita() throws Exception {
        UUID own = newVisit(corretorAId, "2031-09-01T10:00:00-03:00", "AGENDADA");

        mvc.perform(asCorretorA(patch("/api/v1/visits/" + own + "/status")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELADA\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"));
        mvc.perform(asCorretorA(patch("/api/v1/visits/" + own + "/result")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"result\":\"Cliente gostou\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("Cliente gostou"));
    }
}
