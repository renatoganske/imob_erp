package com.imobcrm.user;

import com.imobcrm.onboarding.ClerkUserDirectory;
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
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Convite real por e-mail e vinculo no aceite (IMOB-40). */
class UserInviteAcceptanceTest extends IntegrationTestBase {

    @MockBean
    private ClerkUserDirectory clerk;

    private String run;
    private UUID tenantId;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        reset(clerk);
        run = UUID.randomUUID().toString().substring(0, 8);
        tenantId = seedTenant("t-" + run);
        String adminClerk = "clerk_admin_" + run;
        jdbc.update("INSERT INTO users (id, tenant_id, clerk_user_id, name, email, role, active) VALUES (?, ?, ?, 'Admin', ?, 'ADMIN', true)",
                UUID.randomUUID(), tenantId, adminClerk, "admin-" + run + "@example.com");
        adminToken = token(adminClerk, tenantId, "ADMIN");
    }

    private UUID seedTenant(String slug) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO tenants (id, name, slug, plan) VALUES (?, ?, ?, 'STARTER')", id, "Imob " + slug, slug);
        return id;
    }

    private String email(String who) {
        return who + "-" + run + "@example.com";
    }

    private ResultActions invite(String token, String email, String role) throws Exception {
        return mvc.perform(withToken(token, post("/api/v1/users/invite").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Convidado\",\"email\":\"" + email + "\",\"role\":\"" + role + "\"}")));
    }

    private Map<String, Object> row(String email) {
        return jdbc.queryForMap("SELECT clerk_user_id, active, role FROM users WHERE email = ?", email);
    }

    @Test
    void invitingANewEmailSendsTheClerkInvitationAndKeepsAPendingRecord() throws Exception {
        String email = email("novo");
        when(clerk.findByVerifiedEmail(email)).thenReturn(Optional.empty());

        invite(adminToken, email, "CORRETOR").andExpect(status().isCreated());

        verify(clerk).createInvitation(eq(email), eq(Map.of("tenantId", tenantId.toString(), "role", "CORRETOR")),
                eq("http://localhost:3000/sign-up"));
        Map<String, Object> row = row(email);
        assertEquals(false, row.get("active"));
        assertEquals(true, row.get("clerk_user_id").toString().startsWith("pending:"));
    }

    @Test
    void aClerkFailureRollsTheLocalRecordBack() throws Exception {
        String email = email("falha");
        when(clerk.findByVerifiedEmail(email)).thenReturn(Optional.empty());
        doThrow(new ExternalServiceException("fora do ar", "CLERK_UNAVAILABLE"))
                .when(clerk).createInvitation(anyString(), anyMap(), anyString());

        invite(adminToken, email, "CORRETOR").andExpect(status().isBadGateway());

        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM users WHERE email = ?", Integer.class, email));
    }

    @Test
    void anEmailAlreadyInTheSystemIsRejected() throws Exception {
        invite(adminToken, "admin-" + run + "@example.com", "CORRETOR").andExpect(status().isConflict());
        verify(clerk, never()).createInvitation(anyString(), anyMap(), anyString());
    }

    @Test
    void anExistingClerkAccountIsLinkedRightAway() throws Exception {
        String email = email("conta");
        when(clerk.findByVerifiedEmail(email)).thenReturn(Optional.of(new ClerkUser("user_conta_" + run, "Conta")));

        invite(adminToken, email, "CORRETOR").andExpect(status().isCreated());

        Map<String, Object> row = row(email);
        assertEquals("user_conta_" + run, row.get("clerk_user_id"));
        assertEquals(true, row.get("active"));
        verify(clerk).mergePublicMetadata("user_conta_" + run, Map.of("tenantId", tenantId.toString(), "role", "CORRETOR"));
        verify(clerk, never()).createInvitation(anyString(), anyMap(), anyString());
    }

    @Test
    void anAccountBoundToAnotherTenantCannotBeInvited() throws Exception {
        String email = email("alheia");
        when(clerk.findByVerifiedEmail(email)).thenReturn(
                Optional.of(new ClerkUser("user_alheia_" + run, "Alheia", UUID.randomUUID().toString())));

        invite(adminToken, email, "CORRETOR").andExpect(status().isConflict());

        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM users WHERE email = ?", Integer.class, email));
        verify(clerk, never()).mergePublicMetadata(anyString(), anyMap());
    }

    @Test
    void onlyAdminsCanInvite() throws Exception {
        String corretorClerk = "clerk_corretor_" + run;
        jdbc.update("INSERT INTO users (id, tenant_id, clerk_user_id, name, email, role, active) VALUES (?, ?, ?, 'Corretor', ?, 'CORRETOR', true)",
                UUID.randomUUID(), tenantId, corretorClerk, email("corretor"));

        invite(token(corretorClerk, tenantId, "CORRETOR"), email("x"), "CORRETOR").andExpect(status().isForbidden());
    }

    @Test
    void theInviteeIsLinkedOnTheFirstAccessAfterAccepting() throws Exception {
        String email = email("aceita");
        when(clerk.findByVerifiedEmail(email)).thenReturn(Optional.empty());
        invite(adminToken, email, "CORRETOR").andExpect(status().isCreated());
        String newClerkId = "user_aceita_" + run;
        when(clerk.findVerifiedEmails(newClerkId)).thenReturn(Set.of(email));
        String inviteeToken = token(newClerkId, tenantId, "CORRETOR");

        mvc.perform(withToken(inviteeToken, get("/api/v1/leads"))).andExpect(status().isOk());

        Map<String, Object> row = row(email);
        assertEquals(newClerkId, row.get("clerk_user_id"));
        assertEquals(true, row.get("active"));
    }

    @Test
    void anUnverifiedOrDifferentEmailDoesNotClaimTheInvite() throws Exception {
        String email = email("intruso");
        when(clerk.findByVerifiedEmail(email)).thenReturn(Optional.empty());
        invite(adminToken, email, "CORRETOR").andExpect(status().isCreated());
        String attackerId = "user_intruso_" + run;
        when(clerk.findVerifiedEmails(attackerId)).thenReturn(Set.of("outro-" + run + "@example.com"));

        mvc.perform(withToken(token(attackerId, tenantId, "CORRETOR"), get("/api/v1/leads")))
                .andExpect(status().isForbidden());

        assertEquals(true, row(email).get("clerk_user_id").toString().startsWith("pending:"));
    }

    @Test
    void aTokenFromAnotherTenantCannotClaimTheInvite() throws Exception {
        String email = email("cruzado");
        when(clerk.findByVerifiedEmail(email)).thenReturn(Optional.empty());
        invite(adminToken, email, "CORRETOR").andExpect(status().isCreated());
        String otherId = "user_cruzado_" + run;
        when(clerk.findVerifiedEmails(any())).thenReturn(Set.of(email));

        mvc.perform(withToken(token(otherId, seedTenant("outro-" + run), "CORRETOR"), get("/api/v1/leads")))
                .andExpect(status().isForbidden());

        assertEquals(true, row(email).get("clerk_user_id").toString().startsWith("pending:"));
    }
}
