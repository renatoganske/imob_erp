package com.imobcrm.shared.seed;

import com.imobcrm.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** O seed respeita o schema, e o que ele grava e lido pela API real (enums, FKs e escopo de tenant). */
class DevDataSeederTest extends IntegrationTestBase {

    private static final String SLUG = "seed-test-tenant";
    private static final String ADMIN_CLERK_ID = "seed_test_admin";

    private static UUID tenantId;

    @BeforeAll
    static void seed(@Autowired JdbcTemplate jdbc) {
        if (tenantId != null) {
            return;
        }
        assertTrue(new DevDataSeeder(jdbc, SLUG, ADMIN_CLERK_ID).seed());
        tenantId = jdbc.queryForObject("SELECT id FROM tenants WHERE slug = ?", UUID.class, SLUG);
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE tenant_id = ?", Integer.class, tenantId);
    }

    @Test
    void seedingTwiceDoesNotDuplicateData() {
        int leads = count("leads");
        assertFalse(new DevDataSeeder(jdbc, SLUG, ADMIN_CLERK_ID).seed());
        assertEquals(leads, count("leads"));
        assertEquals(8, leads);
    }

    @Test
    void adminReadsEverySeededEntityThroughTheApi() throws Exception {
        String token = token(ADMIN_CLERK_ID, tenantId, "ADMIN");

        mvc.perform(withToken(token, get("/api/v1/leads"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(8));
        mvc.perform(withToken(token, get("/api/v1/properties"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(6));
        mvc.perform(withToken(token, get("/api/v1/contracts"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(withToken(token, get("/api/v1/visits"))).andExpect(status().isOk());
        mvc.perform(withToken(token, get("/api/v1/commissions"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(withToken(token, get("/api/v1/financial/entries"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(7));
        mvc.perform(withToken(token, get("/api/v1/users"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
    }
}
