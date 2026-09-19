package com.imobcrm.tenant;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Isolamento multi-tenant ponta a ponta (IMOB-19): banco PostgreSQL real (Testcontainers) com o
 * schema do Flyway, contexto Spring completo e o ClerkJwtAuthenticationFilter real validando JWTs
 * assinados localmente e publicados via JWKS em um servidor HTTP embutido.
 * Qualquer vazamento entre tenants aqui deve quebrar o build.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TenantIsolationTest {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    private static final RSAKey SIGNING_KEY;
    private static final HttpServer JWKS_SERVER;

    static {
        POSTGRES.start();
        try {
            SIGNING_KEY = new RSAKeyGenerator(2048).keyID("test-key").generate();
            byte[] jwks = new JWKSet(SIGNING_KEY.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
            JWKS_SERVER = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            JWKS_SERVER.createContext("/jwks", exchange -> {
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, jwks.length);
                exchange.getResponseBody().write(jwks);
                exchange.close();
            });
            JWKS_SERVER.start();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("clerk.jwks-url", () -> "http://localhost:" + JWKS_SERVER.getAddress().getPort() + "/jwks");
    }

    /** Todos os ids de um tenant populado. */
    private record Seed(UUID tenantId, UUID adminId, String adminClerkId, UUID agentId, UUID propertyId,
                        UUID leadId, UUID visitId, UUID contractId, UUID entryId, UUID commissionId) {
    }

    private static Seed tenantA;
    private static Seed tenantB;

    @Value("${clerk.issuer}")
    private String issuer;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeAll
    static void seed(@Autowired JdbcTemplate jdbc) {
        if (tenantA == null) {
            tenantA = seedTenant(jdbc, "a");
            tenantB = seedTenant(jdbc, "b");
        }
    }

    private static Seed seedTenant(JdbcTemplate jdbc, String tag) {
        UUID tenant = UUID.randomUUID(), admin = UUID.randomUUID(), agent = UUID.randomUUID();
        UUID property = UUID.randomUUID(), lead = UUID.randomUUID(), visit = UUID.randomUUID();
        UUID contract = UUID.randomUUID(), entry = UUID.randomUUID(), commission = UUID.randomUUID();
        String adminClerk = "clerk_admin_" + tag;

        jdbc.update("INSERT INTO tenants (id, name, slug, plan) VALUES (?, ?, ?, 'BASIC')",
                tenant, "Imobiliaria " + tag, "imob-" + tag);
        jdbc.update("INSERT INTO users (id, tenant_id, clerk_user_id, name, email, role, commission_rate) "
                + "VALUES (?, ?, ?, ?, ?, 'ADMIN', 5.00)", admin, tenant, adminClerk, "Admin " + tag, "admin@" + tag + ".com");
        jdbc.update("INSERT INTO users (id, tenant_id, clerk_user_id, name, email, role, commission_rate) "
                + "VALUES (?, ?, ?, ?, ?, 'CORRETOR', 5.00)", agent, tenant, "clerk_agent_" + tag, "Corretor " + tag, "agent@" + tag + ".com");
        jdbc.update("INSERT INTO properties (id, tenant_id, type, title, address, city, price, status, purpose) "
                + "VALUES (?, ?, 'APARTAMENTO', ?, 'Rua 1', 'Curitiba', 500000, 'DISPONIVEL', 'VENDA')",
                property, tenant, "Imovel " + tag);
        jdbc.update("INSERT INTO leads (id, tenant_id, assigned_to, name, phone, source, stage) "
                + "VALUES (?, ?, ?, ?, '41999999999', 'SITE', 'NOVO')", lead, tenant, admin, "Lead " + tag);
        jdbc.update("INSERT INTO lead_property (lead_id, property_id) VALUES (?, ?)", lead, property);
        jdbc.update("INSERT INTO visits (id, tenant_id, lead_id, property_id, agent_id, scheduled_at, status) "
                + "VALUES (?, ?, ?, ?, ?, now() + interval '1 day', 'AGENDADA')", visit, tenant, lead, property, admin);
        jdbc.update("INSERT INTO contracts (id, tenant_id, lead_id, property_id, agent_id, type, status, value, start_date, "
                + "buyer_name, buyer_document, owner_name, owner_document) "
                + "VALUES (?, ?, ?, ?, ?, 'COMPRA_VENDA', 'RASCUNHO', 500000, current_date, 'Comprador', '111', 'Dono', '222')",
                contract, tenant, lead, property, admin);
        jdbc.update("INSERT INTO financial_entries (id, tenant_id, contract_id, type, category, description, value, due_date, status) "
                + "VALUES (?, ?, ?, 'RECEITA', 'OUTRO', ?, 1000, current_date, 'PENDENTE')",
                entry, tenant, contract, "Lancamento " + tag);
        jdbc.update("INSERT INTO commissions (id, tenant_id, contract_id, agent_id, rate, base_value, value, status) "
                + "VALUES (?, ?, ?, ?, 5.00, 500000, 25000, 'PENDENTE')", commission, tenant, contract, admin);
        return new Seed(tenant, admin, adminClerk, agent, property, lead, visit, contract, entry, commission);
    }

    private String tokenFor(Seed tenant) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(tenant.adminClerkId())
                .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                .claim("publicMetadata", Map.of("tenantId", tenant.tenantId().toString(), "role", "ADMIN"))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(SIGNING_KEY.getKeyID()).build(), claims);
        jwt.sign(new RSASSASigner(SIGNING_KEY));
        return jwt.serialize();
    }

    private MockHttpServletRequestBuilder as(Seed tenant, MockHttpServletRequestBuilder request) throws Exception {
        return request.header("Authorization", "Bearer " + tokenFor(tenant));
    }

    // ---- listagens: cada tenant so enxerga os proprios registros ----

    @Test
    void listagensRetornamApenasDadosDoProprioTenant() throws Exception {
        assertListsOnlyOwn("/api/v1/properties", tenantA.propertyId(), tenantB.propertyId());
        assertListsOnlyOwn("/api/v1/leads", tenantA.leadId(), tenantB.leadId());
        assertListsOnlyOwn("/api/v1/visits", tenantA.visitId(), tenantB.visitId());
        assertListsOnlyOwn("/api/v1/contracts", tenantA.contractId(), tenantB.contractId());
        assertListsOnlyOwn("/api/v1/financial/entries", tenantA.entryId(), tenantB.entryId());
        assertListsOnlyOwn("/api/v1/commissions", tenantA.commissionId(), tenantB.commissionId());
    }

    private void assertListsOnlyOwn(String path, UUID own, UUID foreign) throws Exception {
        mvc.perform(as(tenantA, get(path).param("size", "100")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem(own.toString())))
                .andExpect(jsonPath("$.content[*].id", not(hasItem(foreign.toString()))));
        mvc.perform(as(tenantB, get(path).param("size", "100")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem(foreign.toString())))
                .andExpect(jsonPath("$.content[*].id", not(hasItem(own.toString()))));
    }

    @Test
    void relatorioDeComissoesEListagemDeUsuariosNaoVazam() throws Exception {
        mvc.perform(as(tenantA, get("/api/v1/commissions/report")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].agentId", not(hasItem(tenantB.adminId().toString()))));
        mvc.perform(as(tenantA, get("/api/v1/users")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(tenantA.adminId().toString())))
                .andExpect(jsonPath("$[*].id", not(hasItem(tenantB.adminId().toString()))))
                .andExpect(jsonPath("$[*].id", not(hasItem(tenantB.agentId().toString()))));
    }

    @Test
    void dashboardFinanceiroNaoSomaLancamentosDeOutroTenant() throws Exception {
        // cada tenant tem exatamente um lancamento de 1000; se somasse o outro daria 2000
        for (Seed tenant : new Seed[]{tenantA, tenantB}) {
            mvc.perform(as(tenant, get("/api/v1/financial/dashboard")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalAReceber").value(1000.0));
        }
    }

    // ---- detalhes: recurso de outro tenant nunca existe (404) ----

    @ParameterizedTest
    @ValueSource(strings = {"properties", "leads", "visits", "contracts", "financial/entries", "commissions"})
    void detalheDeOutroTenantRetorna404(String resource) throws Exception {
        UUID foreignId = idOf(tenantB, resource);
        UUID ownId = idOf(tenantA, resource);
        mvc.perform(as(tenantA, get("/api/v1/" + resource + "/" + foreignId))).andExpect(status().isNotFound());
        mvc.perform(as(tenantA, get("/api/v1/" + resource + "/" + ownId))).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ownId.toString()));
        mvc.perform(as(tenantB, get("/api/v1/" + resource + "/" + ownId))).andExpect(status().isNotFound());
    }

    private static UUID idOf(Seed seed, String resource) {
        return switch (resource) {
            case "properties" -> seed.propertyId();
            case "leads" -> seed.leadId();
            case "visits" -> seed.visitId();
            case "contracts" -> seed.contractId();
            case "financial/entries" -> seed.entryId();
            case "commissions" -> seed.commissionId();
            default -> throw new IllegalArgumentException(resource);
        };
    }

    // ---- escritas cruzadas tambem nao alcancam o outro tenant ----

    @Test
    void escritasEmRecursosDeOutroTenantRetornam404ENaoAlteramDados() throws Exception {
        UUID property = tenantB.propertyId();
        mvc.perform(as(tenantA, patch("/api/v1/properties/" + property + "/status")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"VENDIDO\"}")))
                .andExpect(status().isNotFound());
        mvc.perform(as(tenantA, delete("/api/v1/properties/" + property))).andExpect(status().isNotFound());
        mvc.perform(as(tenantA, patch("/api/v1/leads/" + tenantB.leadId() + "/stage")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"stage\":\"EM_ATENDIMENTO\"}")))
                .andExpect(status().isNotFound());
        mvc.perform(as(tenantA, patch("/api/v1/commissions/" + tenantB.commissionId() + "/pay")))
                .andExpect(status().isNotFound());
        mvc.perform(as(tenantA, patch("/api/v1/financial/entries/" + tenantB.entryId() + "/pay")))
                .andExpect(status().isNotFound());
        mvc.perform(as(tenantA, patch("/api/v1/financial/entries/" + tenantB.entryId() + "/cancel")))
                .andExpect(status().isNotFound());

        Map<String, Object> row = jdbc.queryForMap("SELECT status FROM properties WHERE id = ?", property);
        org.junit.jupiter.api.Assertions.assertEquals("DISPONIVEL", row.get("status"));
        org.junit.jupiter.api.Assertions.assertEquals("NOVO",
                jdbc.queryForObject("SELECT stage FROM leads WHERE id = ?", String.class, tenantB.leadId()));
        org.junit.jupiter.api.Assertions.assertEquals("PENDENTE",
                jdbc.queryForObject("SELECT status FROM commissions WHERE id = ?", String.class, tenantB.commissionId()));
        org.junit.jupiter.api.Assertions.assertEquals("PENDENTE",
                jdbc.queryForObject("SELECT status FROM financial_entries WHERE id = ?", String.class, tenantB.entryId()));
    }

    @Test
    void naoPermiteReferenciarImovelDeOutroTenantAoCriarVisita() throws Exception {
        String body = "{\"leadId\":\"%s\",\"propertyId\":\"%s\",\"agentId\":\"%s\",\"scheduledAt\":\"%s\"}"
                .formatted(tenantA.leadId(), tenantB.propertyId(), tenantA.adminId(), Instant.now().plusSeconds(86400));
        mvc.perform(as(tenantA, post("/api/v1/visits").contentType(MediaType.APPLICATION_JSON).content(body)))
                .andExpect(status().is4xxClientError());
        org.junit.jupiter.api.Assertions.assertEquals(0,
                jdbc.queryForObject("SELECT count(*) FROM visits WHERE tenant_id = ? AND property_id = ?",
                        Integer.class, tenantA.tenantId(), tenantB.propertyId()));
    }

    @Test
    void requisicaoSemTokenOuComTokenInvalidoNaoAutentica() throws Exception {
        mvc.perform(get("/api/v1/properties")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/properties").header("Authorization", "Bearer lixo")).andExpect(status().isForbidden());
    }
}
