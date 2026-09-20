package com.imobcrm.onboarding;

import com.imobcrm.onboarding.ClerkUserDirectory.ClerkUser;
import com.imobcrm.shared.exception.ExternalServiceException;
import com.imobcrm.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Onboarding assistido (IMOB-38): o operador cria a imobiliaria + admin e o admin consegue entrar. */
class OperatorOnboardingTest extends IntegrationTestBase {

    private static final String KEY = "test-operator-key";

    @MockBean
    private ClerkUserDirectory clerk;

    private String run;

    @BeforeEach
    void setUp() {
        reset(clerk);
        run = UUID.randomUUID().toString().substring(0, 8);
    }

    private ResultActions create(String key, String tenantName, String email) throws Exception {
        var request = post("/internal/v1/tenants").contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantName\":\"" + tenantName + "\",\"adminEmail\":\"" + email + "\"}");
        if (key != null) {
            request.header("X-Operator-Key", key);
        }
        return mvc.perform(request);
    }

    private String email(String who) {
        return who + "-" + run + "@example.com";
    }

    private void clerkHas(String email, String clerkId) {
        when(clerk.findByVerifiedEmail(email)).thenReturn(Optional.of(new ClerkUser(clerkId, "Dono " + run)));
    }

    private UUID seedTenant(String slug) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO tenants (id, name, slug, plan) VALUES (?, ?, ?, 'STARTER')", id, "Imob " + slug, slug);
        return id;
    }

    private int tenantCount() {
        return jdbc.queryForObject("SELECT count(*) FROM tenants", Integer.class);
    }

    @Test
    void reusesTheTenantAlreadyInTheClerkMetadataInsteadOfCreatingAnother() throws Exception {
        String email = email("reusa");
        UUID existingTenant = seedTenant("imob-existente-" + run);
        when(clerk.findByVerifiedEmail(email)).thenReturn(
                Optional.of(new ClerkUser("user_reusa_" + run, "Dono", existingTenant.toString())));
        int before = tenantCount();

        create(KEY, "Imob Existente " + run, email)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId").value(existingTenant.toString()));

        assertEquals(before, tenantCount());
        assertEquals(existingTenant, jdbc.queryForObject("SELECT tenant_id FROM users WHERE email = ?", UUID.class, email));
        verify(clerk).mergePublicMetadata("user_reusa_" + run, Map.of("tenantId", existingTenant.toString(), "role", "ADMIN"));
    }

    @Test
    void refusesWhenTheNameDiffersFromTheTenantTheClerkAlreadyPointsTo() throws Exception {
        String email = email("difere");
        UUID existingTenant = seedTenant("imob-outra-" + run);
        when(clerk.findByVerifiedEmail(email)).thenReturn(
                Optional.of(new ClerkUser("user_difere_" + run, "Dono", existingTenant.toString())));
        int before = tenantCount();

        create(KEY, "Nome Diferente " + run, email)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TENANT_MISMATCH"));

        assertEquals(before, tenantCount());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM users WHERE email = ?", Integer.class, email));
        verify(clerk, never()).mergePublicMetadata(anyString(), any());
    }

    @Test
    void failsClearlyWhenTheClerkTenantDoesNotExistInTheDatabase() throws Exception {
        String email = email("orfao");
        when(clerk.findByVerifiedEmail(email)).thenReturn(
                Optional.of(new ClerkUser("user_orfao_" + run, "Dono", UUID.randomUUID().toString())));
        int before = tenantCount();

        create(KEY, "Qualquer " + run, email)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CLERK_TENANT_NOT_FOUND"));

        assertEquals(before, tenantCount());
    }

    @Test
    void failsClearlyWhenTheClerkTenantIdIsNotAUuid() throws Exception {
        String email = email("lixo");
        when(clerk.findByVerifiedEmail(email)).thenReturn(Optional.of(new ClerkUser("user_lixo_" + run, "Dono", "nao-e-uuid")));

        create(KEY, "Qualquer " + run, email)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CLERK_TENANT_INVALID"));
    }

    @Test
    void createsTenantAndAdminSyncsClerkMetadataAndAdminCanLogin() throws Exception {
        String email = email("dono");
        String clerkId = "user_" + run;
        clerkHas(email, clerkId);

        String body = create(KEY, "Imobiliária Ação " + run, email)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.created").value(true))
                .andExpect(jsonPath("$.slug").value("imobiliaria-acao-" + run))
                .andExpect(jsonPath("$.adminClerkUserId").value(clerkId))
                .andReturn().getResponse().getContentAsString();
        UUID tenantId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(body, "$.tenantId"));

        verify(clerk).mergePublicMetadata(clerkId, Map.of("tenantId", tenantId.toString(), "role", "ADMIN"));

        // O token que o Clerk emitira depois do vinculo: prova de que o admin cadastra dados no proprio tenant.
        String token = token(clerkId, tenantId, "ADMIN");
        mvc.perform(withToken(token, get("/api/v1/users")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value(email));
    }

    @Test
    void repeatingTheCallIsIdempotentAndResyncsMetadata() throws Exception {
        String email = email("repete");
        clerkHas(email, "user_repete_" + run);

        create(KEY, "Repete " + run, email).andExpect(status().isCreated());
        create(KEY, "Repete " + run, email).andExpect(status().isOk()).andExpect(jsonPath("$.created").value(false));

        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM tenants WHERE slug = ?", Integer.class, "repete-" + run));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM users WHERE email = ?", Integer.class, email));
        verify(clerk, times(2)).mergePublicMetadata(eq("user_repete_" + run), any());
    }

    @Test
    void retryAfterClerkFailureCompletesTheLink() throws Exception {
        String email = email("falha");
        clerkHas(email, "user_falha_" + run);
        doThrow(new ExternalServiceException("fora do ar", "CLERK_UNAVAILABLE"))
                .doNothing().when(clerk).mergePublicMetadata(anyString(), any());

        create(KEY, "Falha " + run, email).andExpect(status().isBadGateway());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM users WHERE email = ?", Integer.class, email));

        create(KEY, "Falha " + run, email).andExpect(status().isOk());
        verify(clerk, times(2)).mergePublicMetadata(eq("user_falha_" + run), any());
    }

    @Test
    void rejectsMissingOrWrongOperatorKeyBeforeTouchingClerk() throws Exception {
        create(null, "X " + run, email("x")).andExpect(status().isForbidden());
        create("chave-errada", "X " + run, email("x")).andExpect(status().isForbidden());
        verify(clerk, never()).findByVerifiedEmail(anyString());
    }

    @Test
    void failsClearlyWhenThereIsNoVerifiedClerkAccount() throws Exception {
        when(clerk.findByVerifiedEmail(anyString())).thenReturn(Optional.empty());

        create(KEY, "Sem Conta " + run, email("semconta"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CLERK_USER_NOT_FOUND"));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM tenants WHERE slug = ?", Integer.class, "sem-conta-" + run));
    }

    @Test
    void anEmailBelongsToASingleTenant() throws Exception {
        String email = email("unico");
        clerkHas(email, "user_unico_" + run);
        create(KEY, "Primeira " + run, email).andExpect(status().isCreated());

        create(KEY, "Segunda " + run, email)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_IN_USE"));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM tenants WHERE slug = ?", Integer.class, "segunda-" + run));
    }

    @Test
    void pendingInviteRowWithSameEmailBlocksTheLink() throws Exception {
        String email = email("convidado");
        UUID other = UUID.randomUUID();
        jdbc.update("INSERT INTO tenants (id, name, slug, plan) VALUES (?, ?, ?, 'STARTER')", other, "Outra " + run, "outra-" + run);
        jdbc.update("INSERT INTO users (id, tenant_id, clerk_user_id, name, email, role, active) VALUES (?, ?, ?, 'Convidado', ?, 'CORRETOR', false)",
                UUID.randomUUID(), other, "pending:" + UUID.randomUUID(), email);
        clerkHas(email, "user_convidado_" + run);

        create(KEY, "Nova " + run, email).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EMAIL_IN_USE"));
    }

    @Test
    void rejectsATenantNameAlreadyTaken() throws Exception {
        clerkHas(email("a"), "user_a_" + run);
        clerkHas(email("b"), "user_b_" + run);
        create(KEY, "Mesmo Nome " + run, email("a")).andExpect(status().isCreated());

        create(KEY, "mesmo nome " + run, email("b"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TENANT_SLUG_TAKEN"));
    }

    @Test
    void rejectsInvalidBody() throws Exception {
        create(KEY, "", "nao-e-email").andExpect(status().isBadRequest());
    }
}
