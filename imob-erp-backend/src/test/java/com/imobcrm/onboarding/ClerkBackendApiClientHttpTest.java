package com.imobcrm.onboarding;

import com.imobcrm.config.ClerkProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Transporte HTTP de verdade (servidor local), com o construtor de producao do cliente. O
 * MockRestServiceServer nao passa pela rede e nao detecta clientes HTTP sem suporte a PATCH
 * (HttpURLConnection): foi assim que o 502 do IMOB-39 escapou.
 */
class ClerkBackendApiClientHttpTest {

    private HttpServer server;
    private ClerkBackendApiClient client;
    private final List<String> requests = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI() + " " + body
                    + " auth=" + exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] response = exchange.getRequestMethod().equals("GET")
                    ? ("[{\"id\":\"user_1\",\"first_name\":\"Ana\",\"email_addresses\":[{\"email_address\":\"a@x.com\","
                    + "\"verification\":{\"status\":\"verified\"}}]}]").getBytes(StandardCharsets.UTF_8)
                    : "{}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        client = new ClerkBackendApiClient(new ClerkProperties("sk_test_x", null, null, List.of()),
                "http://localhost:" + server.getAddress().getPort());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void findsAUserOverRealHttp() {
        assertEquals("user_1", client.findByVerifiedEmail("a@x.com").orElseThrow().id());
        assertTrue(requests.get(0).startsWith("GET /v1/users?email_address=a%40x.com"), requests.get(0));
        assertTrue(requests.get(0).endsWith("auth=Bearer sk_test_x"), requests.get(0));
    }

    @Test
    void patchesPublicMetadataOverRealHttp() {
        client.mergePublicMetadata("user_1", Map.of("role", "ADMIN"));

        assertEquals(1, requests.size());
        assertTrue(requests.get(0).startsWith("PATCH /v1/users/user_1/metadata "), requests.get(0));
        assertTrue(requests.get(0).contains("\"public_metadata\":{\"role\":\"ADMIN\"}"), requests.get(0));
    }
}
