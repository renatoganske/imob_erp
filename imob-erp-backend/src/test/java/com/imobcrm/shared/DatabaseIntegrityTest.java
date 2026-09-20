package com.imobcrm.shared;

import com.imobcrm.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Migration V5 (IMOB-52): unicidades do dominio, indices em FKs e o guard contra duplicatas pre-existentes. */
class DatabaseIntegrityTest extends IntegrationTestBase {

    private static final String RUN = UUID.randomUUID().toString().substring(0, 8);
    private static UUID tenantA;
    private static UUID tenantB;
    private static UUID agentA;
    private static UUID contractA;

    @BeforeAll
    static void seed(@Autowired JdbcTemplate jdbc) {
        tenantA = UUID.randomUUID();
        tenantB = UUID.randomUUID();
        jdbc.update("INSERT INTO tenants (id, name, slug, plan) VALUES (?, 'Int A', ?, 'STARTER')", tenantA, "int-a-" + RUN);
        jdbc.update("INSERT INTO tenants (id, name, slug, plan) VALUES (?, 'Int B', ?, 'STARTER')", tenantB, "int-b-" + RUN);
        agentA = user(jdbc, tenantA, "dup@example.com", "a1");
        UUID property = UUID.randomUUID();
        jdbc.update("INSERT INTO properties (id, tenant_id, type, title, address, city, price, status, purpose) "
                + "VALUES (?, ?, 'CASA', 'Casa', 'Rua 1', 'Cidade', 100, 'DISPONIVEL', 'VENDA')", property, tenantA);
        contractA = UUID.randomUUID();
        jdbc.update("INSERT INTO contracts (id, tenant_id, property_id, agent_id, type, status, value, start_date, "
                        + "buyer_name, buyer_document, owner_name, owner_document) "
                        + "VALUES (?, ?, ?, ?, 'VENDA', 'ATIVO', 100, current_date, 'B', '1', 'O', '2')",
                contractA, tenantA, property, agentA);
        commission(jdbc, tenantA, contractA, agentA);
        // volume minimo para o planejador considerar o indice de parcelas por contrato
        for (int i = 0; i < 12; i++) {
            jdbc.update("INSERT INTO financial_entries (id, tenant_id, contract_id, type, category, description, value, due_date, status) "
                    + "VALUES (?, ?, ?, 'RECEITA', 'OUTRO', 'p', 10, current_date, 'PENDENTE')", UUID.randomUUID(), tenantA, contractA);
        }
    }

    private static UUID user(JdbcTemplate jdbc, UUID tenant, String email, String clerkSuffix) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, tenant_id, clerk_user_id, name, email, role) VALUES (?, ?, ?, 'U', ?, 'CORRETOR')",
                id, tenant, "clerk-int-" + RUN + "-" + clerkSuffix, email);
        return id;
    }

    private static void commission(JdbcTemplate jdbc, UUID tenant, UUID contract, UUID agent) {
        jdbc.update("INSERT INTO commissions (id, tenant_id, contract_id, agent_id, rate, base_value, value, status) "
                + "VALUES (?, ?, ?, ?, 5, 100, 5, 'PENDENTE')", UUID.randomUUID(), tenant, contract, agent);
    }

    @Test
    void sameEmailInTheSameTenantIsRejectedIgnoringCase() {
        assertThatThrownBy(() -> user(jdbc, tenantA, "DUP@Example.com", "a2"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void sameEmailInAnotherTenantIsAllowed() {
        assertThat(user(jdbc, tenantB, "dup@example.com", "b1")).isNotNull();
    }

    @Test
    void secondCommissionForTheSameContractAndAgentIsRejected() {
        assertThatThrownBy(() -> commission(jdbc, tenantA, contractA, agentA))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void foreignKeyAndCompositeIndexesExist() {
        List<String> indexes = jdbc.queryForList("SELECT indexname FROM pg_indexes WHERE schemaname = current_schema()", String.class);

        assertThat(indexes).contains(
                "uq_users_tenant_email", "uq_commissions_contract_agent",
                "idx_financial_entries_contract_id", "idx_contracts_property_id", "idx_contracts_agent_id",
                "idx_contracts_lead_id", "idx_visits_lead_id", "idx_visits_property_id", "idx_visits_agent_id",
                "idx_lead_property_property_id", "idx_email_outbox_tenant_id", "idx_financial_entries_tenant_status_due");
    }

    /** Sem o indice de FK a listagem de parcelas de um contrato faz seq scan; com ele, usa o indice. */
    @Test
    void installmentsByContractUsesTheForeignKeyIndex() {
        List<String> plan = jdbc.execute((ConnectionCallback<List<String>>) connection -> {
            try (var statement = connection.createStatement()) {
                statement.execute("SET enable_seqscan = off"); // tabela minuscula: forca o planejador a mostrar o caminho por indice
                List<String> lines = new ArrayList<>();
                try (ResultSet rs = statement.executeQuery(
                        "EXPLAIN SELECT * FROM financial_entries WHERE contract_id = '" + contractA + "'")) {
                    while (rs.next()) {
                        lines.add(rs.getString(1));
                    }
                }
                statement.execute("RESET enable_seqscan");
                return lines;
            }
        });
        System.out.println("EXPLAIN parcelas por contrato:\n" + String.join("\n", plan));

        assertThat(String.join("\n", plan)).contains("idx_financial_entries_contract_id");
    }

    /** O guard falha ANTES de criar qualquer indice, com mensagem que diz como achar as duplicatas. */
    @Test
    void migrationAbortsWithActionableMessageWhenDuplicatesAlreadyExist() throws Exception {
        String sql = new String(new ClassPathResource("db/migration/V5__add_fk_indexes_and_unique_constraints.sql")
                .getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String legacyUsers = "users_legacy_test";
        jdbc.execute("CREATE TABLE " + legacyUsers + " (tenant_id UUID, email VARCHAR(255))");
        try {
            jdbc.update("INSERT INTO " + legacyUsers + " VALUES (?, 'x@example.com'), (?, 'X@example.com')", tenantA, tenantA);
            // Executa somente o bloco DO do guard, apontado para a tabela legada.
            String guard = sql.substring(sql.indexOf("DO $$"), sql.indexOf("END $$;") + "END $$;".length())
                    .replace("FROM users", "FROM " + legacyUsers);

            assertThatThrownBy(() -> jdbc.execute(guard))
                    .hasMessageContaining("IMOB-52")
                    .hasMessageContaining("mesmo e-mail");
        } finally {
            jdbc.execute("DROP TABLE " + legacyUsers);
        }
    }
}
