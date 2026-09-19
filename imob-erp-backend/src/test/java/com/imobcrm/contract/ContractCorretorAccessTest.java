package com.imobcrm.contract;

import com.imobcrm.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Leitura de contratos pelo corretor (IMOB-35): apenas os contratos dele, sem CPF/CNPJ e sem a URL do PDF;
 * escrita segue restrita a Admin/Financeiro; sem vazamento entre corretores nem entre tenants.
 */
class ContractCorretorAccessTest extends IntegrationTestBase {

    private static UUID tenantId, corretorAId, corretorBId, propertyId;
    private static UUID foreignTenantId, foreignContractId;

    @BeforeAll
    static void seed(@Autowired JdbcTemplate jdbc) {
        if (tenantId != null) {
            return;
        }
        tenantId = UUID.randomUUID();
        corretorAId = UUID.randomUUID();
        corretorBId = UUID.randomUUID();
        propertyId = UUID.randomUUID();
        jdbc.update("INSERT INTO tenants (id, name, slug, plan) VALUES (?, 'Imobiliaria Contratos', 'imob-contracts-corretor', 'BASIC')", tenantId);
        insertUser(jdbc, tenantId, corretorAId, "cc_corretor_a", "CORRETOR");
        insertUser(jdbc, tenantId, corretorBId, "cc_corretor_b", "CORRETOR");
        insertUser(jdbc, tenantId, UUID.randomUUID(), "cc_admin", "ADMIN");
        insertUser(jdbc, tenantId, UUID.randomUUID(), "cc_financeiro", "FINANCEIRO");
        propertyId = insertProperty(jdbc, tenantId);

        // outro tenant, com um contrato cujo corretor tambem e "corretor"
        foreignTenantId = UUID.randomUUID();
        jdbc.update("INSERT INTO tenants (id, name, slug, plan) VALUES (?, 'Outra', 'imob-contracts-foreign', 'BASIC')", foreignTenantId);
        UUID foreignAgent = UUID.randomUUID();
        insertUser(jdbc, foreignTenantId, foreignAgent, "cc_foreign_corretor", "CORRETOR");
        foreignContractId = insertContract(jdbc, foreignTenantId, insertProperty(jdbc, foreignTenantId), foreignAgent);
    }

    private static void insertUser(JdbcTemplate jdbc, UUID tenant, UUID id, String clerkId, String role) {
        jdbc.update("INSERT INTO users (id, tenant_id, clerk_user_id, name, email, role, commission_rate) "
                + "VALUES (?, ?, ?, ?, ?, ?, 5.00)", id, tenant, clerkId, clerkId, clerkId + "@x.com", role);
    }

    private static UUID insertProperty(JdbcTemplate jdbc, UUID tenant) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO properties (id, tenant_id, type, title, address, city, price, status, purpose) "
                + "VALUES (?, ?, 'APARTAMENTO', 'Imovel', 'Rua 1', 'Curitiba', 500000, 'DISPONIVEL', 'VENDA')", id, tenant);
        return id;
    }

    private static UUID insertContract(JdbcTemplate jdbc, UUID tenant, UUID property, UUID agent) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO contracts (id, tenant_id, property_id, agent_id, type, status, value, start_date, "
                + "buyer_name, buyer_document, owner_name, owner_document, document_url, notes) "
                + "VALUES (?, ?, ?, ?, 'COMPRA_VENDA', 'RASCUNHO', 500000, current_date, 'Comprador Fulano', '111.111.111-11', "
                + "'Dono Beltrano', '222.222.222-22', 'http://localhost/fake-r2/contrato.pdf', 'observacao interna')",
                id, tenant, property, agent);
        return id;
    }

    private UUID newContract(UUID agent) {
        return insertContract(jdbc, tenantId, propertyId, agent);
    }

    private MockHttpServletRequestBuilder as(String clerkId, String role, MockHttpServletRequestBuilder r) throws Exception {
        return withToken(token(clerkId, tenantId, role), r);
    }

    private MockHttpServletRequestBuilder asCorretorA(MockHttpServletRequestBuilder r) throws Exception {
        return as("cc_corretor_a", "CORRETOR", r);
    }

    // ---- leitura do corretor ----

    @Test
    void corretorListsOnlyOwnContractsWithoutSensitiveData() throws Exception {
        UUID own = newContract(corretorAId);
        UUID foreign = newContract(corretorBId);

        mvc.perform(asCorretorA(get("/api/v1/contracts").param("size", "100")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem(own.toString())))
                .andExpect(jsonPath("$.content[*].id", not(hasItem(foreign.toString()))))
                .andExpect(jsonPath("$.content[?(@.id=='" + own + "')].buyerName", hasItem("Comprador Fulano")))
                .andExpect(jsonPath("$.content[?(@.id=='" + own + "')].notes", hasItem("observacao interna")))
                .andExpect(jsonPath("$.content[*].buyerDocument", not(hasItem("111.111.111-11"))))
                .andExpect(jsonPath("$.content[*].ownerDocument", not(hasItem("222.222.222-22"))))
                .andExpect(jsonPath("$.content[*].documentUrl", not(hasItem("http://localhost/fake-r2/contrato.pdf"))));
    }

    @Test
    void corretorDetailOfOwnContractOmitsCpfCnpjAndDocumentUrl() throws Exception {
        UUID own = newContract(corretorAId);

        mvc.perform(asCorretorA(get("/api/v1/contracts/" + own)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(own.toString()))
                .andExpect(jsonPath("$.buyerName").value("Comprador Fulano"))
                .andExpect(jsonPath("$.ownerName").value("Dono Beltrano"))
                .andExpect(jsonPath("$.buyerDocument").value(nullValue()))
                .andExpect(jsonPath("$.ownerDocument").value(nullValue()))
                .andExpect(jsonPath("$.documentUrl").value(nullValue()));
    }

    @Test
    void corretorGets404ForAnotherCorretorsContractAndForAnotherTenants() throws Exception {
        UUID foreign = newContract(corretorBId);

        mvc.perform(asCorretorA(get("/api/v1/contracts/" + foreign))).andExpect(status().isNotFound());
        mvc.perform(asCorretorA(get("/api/v1/contracts/" + foreignContractId))).andExpect(status().isNotFound());
    }

    @Test
    void adminAndFinanceiroStillSeeEverythingIncludingSensitiveData() throws Exception {
        UUID a = newContract(corretorAId);
        UUID b = newContract(corretorBId);

        for (String[] who : new String[][]{{"cc_admin", "ADMIN"}, {"cc_financeiro", "FINANCEIRO"}}) {
            mvc.perform(as(who[0], who[1], get("/api/v1/contracts").param("size", "200")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].id", hasItem(a.toString())))
                    .andExpect(jsonPath("$.content[*].id", hasItem(b.toString())))
                    .andExpect(jsonPath("$.content[*].id", not(hasItem(foreignContractId.toString()))));
            mvc.perform(as(who[0], who[1], get("/api/v1/contracts/" + b)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.buyerDocument").value("111.111.111-11"))
                    .andExpect(jsonPath("$.ownerDocument").value("222.222.222-22"))
                    .andExpect(jsonPath("$.documentUrl").value("http://localhost/fake-r2/contrato.pdf"));
        }
    }

    // ---- escrita continua restrita ----

    @Test
    void corretorCannotWriteContractsEvenItsOwn() throws Exception {
        UUID own = newContract(corretorAId);
        String body = "{\"propertyId\":\"" + propertyId + "\",\"agentId\":\"" + corretorAId + "\",\"type\":\"COMPRA_VENDA\","
                + "\"value\":100,\"startDate\":\"2026-01-01\",\"buyerName\":\"X\",\"buyerDocument\":\"1\","
                + "\"ownerName\":\"Y\",\"ownerDocument\":\"2\"}";

        mvc.perform(asCorretorA(post("/api/v1/contracts").contentType(MediaType.APPLICATION_JSON).content(body)))
                .andExpect(status().isForbidden());
        mvc.perform(asCorretorA(put("/api/v1/contracts/" + own).contentType(MediaType.APPLICATION_JSON).content(body)))
                .andExpect(status().isForbidden());
        mvc.perform(asCorretorA(patch("/api/v1/contracts/" + own + "/status")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ATIVO\"}")))
                .andExpect(status().isForbidden());
        var pdf = new MockMultipartFile("file", "c.pdf", "application/pdf", "%PDF-1.7 x".getBytes());
        mvc.perform(asCorretorA(multipart("/api/v1/contracts/" + own + "/document").file(pdf)))
                .andExpect(status().isForbidden());

        assertEquals("RASCUNHO", jdbc.queryForObject("SELECT status FROM contracts WHERE id = ?", String.class, own));
        assertEquals("http://localhost/fake-r2/contrato.pdf",
                jdbc.queryForObject("SELECT document_url FROM contracts WHERE id = ?", String.class, own));
    }
}
