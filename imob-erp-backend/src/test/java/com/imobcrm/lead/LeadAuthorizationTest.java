package com.imobcrm.lead;

import com.imobcrm.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Autorizacao de leads por corretor e regra de FECHADO (IMOB-20), com o stack real:
 * corretor A x corretor B no mesmo tenant, Admin e Financeiro.
 */
class LeadAuthorizationTest extends IntegrationTestBase {

    private static UUID tenantId, adminId, corretorAId, corretorBId, propertyId;

    @BeforeAll
    static void seed(@Autowired JdbcTemplate jdbc) {
        if (tenantId != null) {
            return;
        }
        tenantId = UUID.randomUUID();
        adminId = UUID.randomUUID();
        corretorAId = UUID.randomUUID();
        corretorBId = UUID.randomUUID();
        propertyId = UUID.randomUUID();
        jdbc.update("INSERT INTO tenants (id, name, slug, plan) VALUES (?, 'Imobiliaria Leads', 'imob-leads-authz', 'BASIC')", tenantId);
        insertUser(jdbc, adminId, "leads_admin", "ADMIN");
        insertUser(jdbc, corretorAId, "leads_corretor_a", "CORRETOR");
        insertUser(jdbc, corretorBId, "leads_corretor_b", "CORRETOR");
        insertUser(jdbc, UUID.randomUUID(), "leads_financeiro", "FINANCEIRO");
        jdbc.update("INSERT INTO properties (id, tenant_id, type, title, address, city, price, status, purpose) "
                + "VALUES (?, ?, 'APARTAMENTO', 'Imovel', 'Rua 1', 'Curitiba', 500000, 'DISPONIVEL', 'VENDA')", propertyId, tenantId);
    }

    private static void insertUser(JdbcTemplate jdbc, UUID id, String clerkId, String role) {
        jdbc.update("INSERT INTO users (id, tenant_id, clerk_user_id, name, email, role, commission_rate) "
                + "VALUES (?, ?, ?, ?, ?, ?, 5.00)", id, tenantId, clerkId, clerkId, clerkId + "@x.com", role);
    }

    private UUID newLead(UUID assignedTo, String stage) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO leads (id, tenant_id, assigned_to, name, phone, source, stage) "
                + "VALUES (?, ?, ?, 'Lead', '41999999999', 'SITE', ?)", id, tenantId, assignedTo, stage);
        return id;
    }

    private MockHttpServletRequestBuilder asCorretorA(MockHttpServletRequestBuilder r) throws Exception {
        return withToken(token("leads_corretor_a", tenantId, "CORRETOR"), r);
    }

    private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder r) throws Exception {
        return withToken(token("leads_admin", tenantId, "ADMIN"), r);
    }

    private MockHttpServletRequestBuilder asFinanceiro(MockHttpServletRequestBuilder r) throws Exception {
        return withToken(token("leads_financeiro", tenantId, "FINANCEIRO"), r);
    }

    // ---- leitura ----

    @Test
    void corretorListaApenasOsProprios_mesmoComFiltroAssignedToDeOutro() throws Exception {
        UUID own = newLead(corretorAId, "NOVO");
        UUID foreign = newLead(corretorBId, "NOVO");

        mvc.perform(asCorretorA(get("/api/v1/leads").param("size", "100")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem(own.toString())))
                .andExpect(jsonPath("$.content[*].id", not(hasItem(foreign.toString()))));
        mvc.perform(asCorretorA(get("/api/v1/leads").param("size", "100").param("assignedTo", corretorBId.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", not(hasItem(foreign.toString()))));
    }

    @Test
    void adminEFinanceiroVeemTodosOsLeads() throws Exception {
        UUID a = newLead(corretorAId, "NOVO");
        UUID b = newLead(corretorBId, "NOVO");
        for (var request : new MockHttpServletRequestBuilder[]{
                asAdmin(get("/api/v1/leads").param("size", "200")),
                asFinanceiro(get("/api/v1/leads").param("size", "200"))}) {
            mvc.perform(request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].id", hasItem(a.toString())))
                    .andExpect(jsonPath("$.content[*].id", hasItem(b.toString())));
        }
        mvc.perform(asFinanceiro(get("/api/v1/leads/" + b))).andExpect(status().isOk());
    }

    @Test
    void corretorNaoVeDetalheDeLeadDeOutroCorretor() throws Exception {
        UUID foreign = newLead(corretorBId, "NOVO");
        mvc.perform(asCorretorA(get("/api/v1/leads/" + foreign))).andExpect(status().isNotFound());
        mvc.perform(asAdmin(get("/api/v1/leads/" + foreign))).andExpect(status().isOk());
    }

    // ---- escrita ----

    @Test
    void corretorNaoAlteraLeadDeOutroCorretor() throws Exception {
        UUID foreign = newLead(corretorBId, "NOVO");
        String update = "{\"name\":\"Hack\",\"phone\":\"41888888888\",\"source\":\"SITE\"}";
        mvc.perform(asCorretorA(put("/api/v1/leads/" + foreign).contentType(MediaType.APPLICATION_JSON).content(update)))
                .andExpect(status().isNotFound());
        mvc.perform(asCorretorA(patch("/api/v1/leads/" + foreign + "/stage")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"stage\":\"EM_ATENDIMENTO\"}")))
                .andExpect(status().isNotFound());
        mvc.perform(asCorretorA(post("/api/v1/leads/" + foreign + "/properties")
                        .contentType(MediaType.APPLICATION_JSON).content("\"" + propertyId + "\"")))
                .andExpect(status().isNotFound());
        mvc.perform(asCorretorA(delete("/api/v1/leads/" + foreign + "/properties/" + propertyId)))
                .andExpect(status().isNotFound());

        assertEquals("NOVO", jdbc.queryForObject("SELECT stage FROM leads WHERE id = ?", String.class, foreign));
        assertEquals("Lead", jdbc.queryForObject("SELECT name FROM leads WHERE id = ?", String.class, foreign));
    }

    @Test
    void apenasAdminReatribuiLead() throws Exception {
        UUID own = newLead(corretorAId, "NOVO");
        String body = "{\"agentId\":\"" + corretorBId + "\"}";
        mvc.perform(asCorretorA(patch("/api/v1/leads/" + own + "/assign").contentType(MediaType.APPLICATION_JSON).content(body)))
                .andExpect(status().isForbidden());
        mvc.perform(asAdmin(patch("/api/v1/leads/" + own + "/assign").contentType(MediaType.APPLICATION_JSON).content(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedTo").value(corretorBId.toString()));
    }

    @Test
    void corretorNaoAtribuiLeadAOutroCorretorNemAoCriarNemAoEditar() throws Exception {
        String createForOther = "{\"name\":\"Novo\",\"phone\":\"41999999999\",\"source\":\"SITE\",\"assignedTo\":\"" + corretorBId + "\"}";
        mvc.perform(asCorretorA(post("/api/v1/leads").contentType(MediaType.APPLICATION_JSON).content(createForOther)))
                .andExpect(status().isForbidden());

        String createOwn = "{\"name\":\"Novo\",\"phone\":\"41999999999\",\"source\":\"SITE\"}";
        mvc.perform(asCorretorA(post("/api/v1/leads").contentType(MediaType.APPLICATION_JSON).content(createOwn)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignedTo").value(corretorAId.toString()));

        UUID own = newLead(corretorAId, "NOVO");
        mvc.perform(asCorretorA(put("/api/v1/leads/" + own).contentType(MediaType.APPLICATION_JSON).content(createForOther)))
                .andExpect(status().isForbidden());
        assertEquals(corretorAId, jdbc.queryForObject("SELECT assigned_to FROM leads WHERE id = ?", UUID.class, own));
    }

    @Test
    void financeiroNaoAlteraLeads() throws Exception {
        UUID lead = newLead(corretorAId, "NOVO");
        mvc.perform(asFinanceiro(patch("/api/v1/leads/" + lead + "/stage")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"stage\":\"EM_ATENDIMENTO\"}")))
                .andExpect(status().isForbidden());
    }

    // ---- regra de FECHADO ----

    @Test
    void fecharLeadSemImovelRetorna422ENaoAlteraOStage() throws Exception {
        UUID lead = newLead(corretorAId, "PROPOSTA");
        mvc.perform(asCorretorA(patch("/api/v1/leads/" + lead + "/stage")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"stage\":\"FECHADO\"}")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("LEAD_FECHADO_SEM_IMOVEL"));

        assertEquals("PROPOSTA", jdbc.queryForObject("SELECT stage FROM leads WHERE id = ?", String.class, lead));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM contracts WHERE lead_id = ?", Integer.class, lead));
    }

    @Test
    void fecharLeadComImovelFuncionaEGeraRascunhoDeContrato() throws Exception {
        UUID lead = newLead(corretorAId, "PROPOSTA");
        mvc.perform(asCorretorA(post("/api/v1/leads/" + lead + "/properties")
                        .contentType(MediaType.APPLICATION_JSON).content("\"" + propertyId + "\"")))
                .andExpect(status().isOk());

        mvc.perform(asCorretorA(patch("/api/v1/leads/" + lead + "/stage")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"stage\":\"FECHADO\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value("FECHADO"));

        assertEquals("RASCUNHO", jdbc.queryForObject(
                "SELECT status FROM contracts WHERE lead_id = ?", String.class, lead));
    }
}
