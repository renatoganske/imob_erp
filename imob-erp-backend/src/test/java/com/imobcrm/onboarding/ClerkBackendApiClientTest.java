package com.imobcrm.onboarding;

import com.imobcrm.config.ClerkProperties;
import com.imobcrm.shared.exception.ExternalServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ClerkBackendApiClientTest {

    private MockRestServiceServer server;
    private ClerkBackendApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ClerkBackendApiClient(builder, new ClerkProperties("sk_test_x", null, null, List.of()), "https://clerk.test");
    }

    @Test
    void findsTheUserByVerifiedEmailAndEncodesThePlusSign() {
        server.expect(requestTo("https://clerk.test/v1/users?email_address=a%2Bb%40x.com"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer sk_test_x"))
                .andRespond(withSuccess("""
                        [{"id":"user_1","first_name":"Ana","last_name":"Lima",
                          "email_addresses":[{"email_address":"a+b@x.com","verification":{"status":"verified"}}]}]
                        """, MediaType.APPLICATION_JSON));

        var user = client.findByVerifiedEmail("a+b@x.com").orElseThrow();

        assertEquals("user_1", user.id());
        assertEquals("Ana Lima", user.name());
        server.verify();
    }

    @Test
    void ignoresUsersWhoseEmailIsNotVerified() {
        server.expect(requestTo("https://clerk.test/v1/users?email_address=a%40x.com"))
                .andRespond(withSuccess("""
                        [{"id":"user_1","email_addresses":[{"email_address":"a@x.com","verification":{"status":"unverified"}}]}]
                        """, MediaType.APPLICATION_JSON));

        assertTrue(client.findByVerifiedEmail("a@x.com").isEmpty());
    }

    @Test
    void mergesPublicMetadataWithSnakeCaseBody() {
        server.expect(requestTo("https://clerk.test/v1/users/user_1/metadata"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(content().json("{\"public_metadata\":{\"tenantId\":\"t-1\",\"role\":\"ADMIN\"}}"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        client.mergePublicMetadata("user_1", Map.of("tenantId", "t-1", "role", "ADMIN"));

        server.verify();
    }

    @Test
    void mapsClerkFailuresToAnExternalServiceError() {
        server.expect(requestTo("https://clerk.test/v1/users/user_1/metadata")).andRespond(withServerError());

        var e = assertThrows(ExternalServiceException.class, () -> client.mergePublicMetadata("user_1", Map.of()));
        assertEquals("CLERK_UNAVAILABLE", e.getCode());
    }
}
