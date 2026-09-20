package com.imobcrm.onboarding;

import com.fasterxml.jackson.databind.JsonNode;
import com.imobcrm.config.ClerkProperties;
import com.imobcrm.shared.exception.ExternalServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class ClerkBackendApiClient implements ClerkUserDirectory {

    private final RestClient client;

    @Autowired
    public ClerkBackendApiClient(ClerkProperties properties,
                                 @Value("${clerk.api-url:https://api.clerk.com}") String apiUrl) {
        // Fixo no cliente HTTP do JDK: o padrao do Spring Boot 3.3 (HttpURLConnection) nao suporta PATCH (IMOB-39).
        this(RestClient.builder().requestFactory(new JdkClientHttpRequestFactory()), properties, apiUrl);
    }

    /** Para testes com MockRestServiceServer: o builder ja chega com a request factory do mock. */
    ClerkBackendApiClient(RestClient.Builder builder, ClerkProperties properties, String apiUrl) {
        this.client = builder
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.secretKey())
                .build();
    }

    @Override
    public Optional<ClerkUser> findByVerifiedEmail(String email) {
        JsonNode users;
        try {
            users = client.get()
                    .uri(uri -> uri.path("/v1/users").queryParam("email_address", "{email}").build(email))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            throw unavailable("buscar usuario", e);
        }
        if (users == null || !users.isArray()) {
            return Optional.empty();
        }
        for (JsonNode user : users) {
            if (hasVerifiedEmail(user, email)) {
                return Optional.of(new ClerkUser(user.path("id").asText(), displayName(user, email)));
            }
        }
        return Optional.empty();
    }

    @Override
    public void mergePublicMetadata(String clerkUserId, Map<String, Object> metadata) {
        try {
            client.patch()
                    .uri("/v1/users/{id}/metadata", clerkUserId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("public_metadata", metadata))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw unavailable("gravar metadata", e);
        }
    }

    private static boolean hasVerifiedEmail(JsonNode user, String email) {
        for (JsonNode address : user.path("email_addresses")) {
            if (email.equalsIgnoreCase(address.path("email_address").asText())
                    && "verified".equals(address.path("verification").path("status").asText())) {
                return true;
            }
        }
        return false;
    }

    private static String displayName(JsonNode user, String email) {
        String name = (user.path("first_name").asText("") + " " + user.path("last_name").asText("")).trim();
        return name.isEmpty() ? email.substring(0, email.indexOf('@')) : name;
    }

    private static ExternalServiceException unavailable(String action, RestClientException e) {
        log.error("Falha na API do Clerk ao {}: {}", action, e.getMessage());
        return new ExternalServiceException("Nao foi possivel falar com o Clerk (" + action + "). Tente novamente.",
                "CLERK_UNAVAILABLE");
    }
}
