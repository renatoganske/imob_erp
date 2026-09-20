package com.imobcrm.financial;

import com.imobcrm.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Dashboard financeiro por mes (IMOB-23): parametro month, inadimplencia acumulada, RBAC e isolamento. */
class FinancialDashboardTest extends IntegrationTestBase {

    private static UUID tenantId;
    private static final String RUN = UUID.randomUUID().toString().substring(0, 8);

    @BeforeAll
    static void seed(@Autowired JdbcTemplate jdbc) {
        tenantId = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        jdbc.update("INSERT INTO tenants (id, name, slug, plan) VALUES (?, 'Dash A', ?, 'STARTER')", tenantId, "dash-a-" + RUN);
        jdbc.update("INSERT INTO tenants (id, name, slug, plan) VALUES (?, 'Dash B', ?, 'STARTER')", other, "dash-b-" + RUN);
        for (String[] u : new String[][]{{"admin", "ADMIN"}, {"fin", "FINANCEIRO"}, {"corretor", "CORRETOR"}}) {
            jdbc.update("INSERT INTO users (id, tenant_id, clerk_user_id, name, email, role, active) VALUES (?, ?, ?, ?, ?, ?, true)",
                    UUID.randomUUID(), tenantId, "clerk_" + u[0] + "_" + RUN, u[0], u[0] + "-" + RUN + "@x.com", u[1]);
        }
        // Mes 2026-05
        entry(jdbc, tenantId, "RECEITA", "PAGO", 1000, "2026-05-10");
        entry(jdbc, tenantId, "DESPESA", "PAGO", 300, "2026-05-12");
        entry(jdbc, tenantId, "RECEITA", "PENDENTE", 500, "2026-05-25");
        entry(jdbc, tenantId, "DESPESA", "PENDENTE", 200, "2026-05-28");
        entry(jdbc, tenantId, "RECEITA", "ATRASADO", 100, "2026-05-05");
        // Atraso de mes anterior: continua devido e deve aparecer na inadimplencia de maio
        entry(jdbc, tenantId, "RECEITA", "ATRASADO", 400, "2026-04-15");
        // Outros meses
        entry(jdbc, tenantId, "RECEITA", "PAGO", 7000, "2026-06-10");
        entry(jdbc, tenantId, "RECEITA", "ATRASADO", 50, "2026-07-01");
        // Outro tenant: nunca pode entrar nas somas
        entry(jdbc, other, "RECEITA", "ATRASADO", 9999, "2026-05-01");
        entry(jdbc, other, "RECEITA", "PAGO", 8888, "2026-05-02");
    }

    private static void entry(JdbcTemplate jdbc, UUID tenant, String type, String status, int value, String dueDate) {
        jdbc.update("INSERT INTO financial_entries (id, tenant_id, type, category, description, value, due_date, status) "
                + "VALUES (?, ?, ?, 'OUTRO', 'teste', ?, ?::date, ?)", UUID.randomUUID(), tenant, type, value, dueDate, status);
    }

    private ResultActions dashboard(String role, String query) throws Exception {
        String clerkId = "clerk_" + (role.equals("ADMIN") ? "admin" : role.equals("FINANCEIRO") ? "fin" : "corretor") + "_" + RUN;
        return mvc.perform(withToken(token(clerkId, tenantId, role), get("/api/v1/financial/dashboard" + query)));
    }

    @Test
    void returnsTheRequestedMonth() throws Exception {
        dashboard("ADMIN", "?month=2026-05")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2026-05"))
                .andExpect(jsonPath("$.saldoDoMes").value(700.0))
                .andExpect(jsonPath("$.totalAReceber").value(500.0))
                .andExpect(jsonPath("$.totalAPagar").value(200.0));
    }

    @Test
    void delinquencyIncludesOverdueFromPreviousMonthsButNotFutureOnes() throws Exception {
        dashboard("ADMIN", "?month=2026-05").andExpect(jsonPath("$.inadimplencia").value(500.0)); // 100 (maio) + 400 (abril)
        dashboard("ADMIN", "?month=2026-06").andExpect(jsonPath("$.inadimplencia").value(500.0)); // o de 01/07 ainda nao vencia
        dashboard("ADMIN", "?month=2026-07").andExpect(jsonPath("$.inadimplencia").value(550.0));
        dashboard("ADMIN", "?month=2026-03").andExpect(jsonPath("$.inadimplencia").value(0.0));
    }

    @Test
    void otherMonthsHaveTheirOwnBalance() throws Exception {
        dashboard("ADMIN", "?month=2026-06")
                .andExpect(jsonPath("$.saldoDoMes").value(7000.0))
                .andExpect(jsonPath("$.totalAReceber").value(0.0))
                .andExpect(jsonPath("$.totalAPagar").value(0.0));
    }

    @Test
    void defaultsToTheCurrentMonthInBrasiliaTimeWhenMonthIsMissing() throws Exception {
        dashboard("ADMIN", "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value(YearMonth.now(ZoneId.of("America/Sao_Paulo")).toString()));
    }

    @Test
    void financeiroCanReadButCorretorCannot() throws Exception {
        dashboard("FINANCEIRO", "?month=2026-05").andExpect(status().isOk());
        dashboard("CORRETOR", "?month=2026-05").andExpect(status().isForbidden());
    }

    @Test
    void invalidMonthIsABadRequestNotAServerError() throws Exception {
        dashboard("ADMIN", "?month=2026-13").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        dashboard("ADMIN", "?month=maio").andExpect(status().isBadRequest());
    }
}
