package com.imobcrm.property;

import com.imobcrm.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** IMOB-50: a ordem das fotos e persistida e pode ser alterada por PUT /properties/{id}/photos/order. */
class PropertyPhotoOrderTest extends IntegrationTestBase {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13, 'I', 'H', 'D', 'R'};

    private static UUID tenantId;
    private static UUID otherTenantId;

    @MockBean
    private S3Client s3Client;

    @BeforeAll
    static void seed(@Autowired JdbcTemplate jdbc) {
        if (tenantId != null) {
            return;
        }
        tenantId = UUID.randomUUID();
        otherTenantId = UUID.randomUUID();
        jdbc.update("INSERT INTO tenants (id, name, slug, plan) VALUES (?, 'Imobiliaria Ordem', 'imob-photo-order', 'BASIC')", tenantId);
        jdbc.update("INSERT INTO tenants (id, name, slug, plan) VALUES (?, 'Outra Ordem', 'outra-photo-order', 'BASIC')", otherTenantId);
        insertUser(jdbc, tenantId, "order_corretor", "CORRETOR", "c@ordem.com");
        insertUser(jdbc, tenantId, "order_financeiro", "FINANCEIRO", "f@ordem.com");
        insertUser(jdbc, otherTenantId, "order_outro", "CORRETOR", "o@outra.com");
    }

    private static void insertUser(JdbcTemplate jdbc, UUID tenant, String clerkId, String role, String email) {
        jdbc.update("INSERT INTO users (id, tenant_id, clerk_user_id, name, email, role) VALUES (?, ?, ?, 'Usuario', ?, ?)",
                UUID.randomUUID(), tenant, clerkId, email, role);
    }

    private UUID newProperty() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO properties (id, tenant_id, type, title, address, city, price, status, purpose) "
                + "VALUES (?, ?, 'APARTAMENTO', 'Imovel', 'Rua 1', 'Curitiba', 500000, 'DISPONIVEL', 'VENDA')", id, tenantId);
        return id;
    }

    /** Insere as fotos ja na ordem dada e devolve as chaves (nome do arquivo), na mesma ordem. */
    private List<String> withPhotos(UUID property, int count) {
        List<String> keys = IntStream.range(0, count).mapToObj(i -> UUID.randomUUID() + ".png").toList();
        IntStream.range(0, count).forEach(i -> jdbc.update(
                "INSERT INTO property_photos (property_id, photo_order, photos) VALUES (?, ?, ?)",
                property, i, urlOf(property, keys.get(i))));
        return keys;
    }

    private String urlOf(UUID property, String key) {
        return "http://localhost/fake-r2/" + tenantId + "/properties/" + property + "/" + key;
    }

    private List<String> storedKeysInOrder(UUID property) {
        return jdbc.queryForList("SELECT photos FROM property_photos WHERE property_id = ? ORDER BY photo_order", String.class, property)
                .stream().map(url -> url.substring(url.lastIndexOf('/') + 1)).toList();
    }

    private MockHttpServletRequestBuilder reorder(String clerkId, UUID tenant, UUID property, List<String> keys) throws Exception {
        String body = "{\"keys\":[" + String.join(",", keys.stream().map(k -> "\"" + k + "\"").toList()) + "]}";
        return withToken(token(clerkId, tenant, clerkId.equals("order_financeiro") ? "FINANCEIRO" : "CORRETOR"),
                put("/api/v1/properties/" + property + "/photos/order").contentType("application/json").content(body));
    }

    private MockHttpServletRequestBuilder reorderAsCorretor(UUID property, List<String> keys) throws Exception {
        return reorder("order_corretor", tenantId, property, keys);
    }

    private List<String> reversed(List<String> keys) {
        List<String> copy = new ArrayList<>(keys);
        java.util.Collections.reverse(copy);
        return copy;
    }

    @Test
    void reorderPersistsAndIsReturnedByTheResponseAndByANewGet() throws Exception {
        UUID property = newProperty();
        List<String> keys = withPhotos(property, 4);
        List<String> newOrder = List.of(keys.get(2), keys.get(0), keys.get(3), keys.get(1));

        var response = mvc.perform(reorderAsCorretor(property, newOrder)).andExpect(status().isOk()).andReturn();
        List<String> returned = com.jayway.jsonpath.JsonPath.read(response.getResponse().getContentAsString(), "$.photos");

        assertThat(returned).containsExactlyElementsOf(newOrder.stream().map(k -> urlOf(property, k)).toList());
        assertThat(storedKeysInOrder(property)).containsExactlyElementsOf(newOrder);
        mvc.perform(withToken(token("order_corretor", tenantId, "CORRETOR"), get("/api/v1/properties/" + property)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photos[0]").value(urlOf(property, newOrder.get(0))))
                .andExpect(jsonPath("$.photos[3]").value(urlOf(property, newOrder.get(3))));
    }

    @Test
    void newPhotoGoesToTheEndAfterAReorder() throws Exception {
        reset(s3Client);
        UUID property = newProperty();
        List<String> keys = withPhotos(property, 3);
        List<String> newOrder = reversed(keys);
        mvc.perform(reorderAsCorretor(property, newOrder)).andExpect(status().isOk());

        var file = new MockMultipartFile("file", "nova.png", "image/png", PNG);
        mvc.perform(withToken(token("order_corretor", tenantId, "CORRETOR"),
                multipart("/api/v1/properties/" + property + "/photos").file(file))).andExpect(status().isOk());

        List<String> stored = storedKeysInOrder(property);
        assertThat(stored).hasSize(4);
        assertThat(stored.subList(0, 3)).containsExactlyElementsOf(newOrder);
    }

    @Test
    void removingAPhotoLeavesNoHolesInTheOrder() throws Exception {
        reset(s3Client);
        UUID property = newProperty();
        List<String> keys = withPhotos(property, 4);
        mvc.perform(reorderAsCorretor(property, reversed(keys))).andExpect(status().isOk());
        String removed = keys.get(2);

        mvc.perform(withToken(token("order_corretor", tenantId, "CORRETOR"),
                delete("/api/v1/properties/" + property + "/photos/" + removed))).andExpect(status().isOk());

        assertThat(jdbc.queryForList("SELECT photo_order FROM property_photos WHERE property_id = ? ORDER BY photo_order",
                Integer.class, property)).containsExactly(0, 1, 2);
        assertThat(storedKeysInOrder(property)).containsExactlyElementsOf(reversed(keys).stream().filter(k -> !k.equals(removed)).toList());
    }

    @Test
    void incompleteDuplicatedOrUnknownKeyListsReturn422AndKeepTheOrder() throws Exception {
        UUID property = newProperty();
        List<String> keys = withPhotos(property, 3);
        List<List<String>> invalid = List.of(
                keys.subList(0, 2),                                            // falta uma
                List.of(keys.get(0), keys.get(1), keys.get(1)),                // repetida
                List.of(keys.get(0), keys.get(1), keys.get(2), keys.get(0)),   // sobra
                List.of(keys.get(0), keys.get(1), "desconhecida.png"),         // chave que nao e do imovel
                List.of());                                                    // vazia
        for (List<String> attempt : invalid) {
            mvc.perform(reorderAsCorretor(property, attempt))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("PHOTO_ORDER_MISMATCH"));
        }
        assertThat(storedKeysInOrder(property)).containsExactlyElementsOf(keys);
    }

    @Test
    void missingKeysFieldReturns400() throws Exception {
        UUID property = newProperty();
        withPhotos(property, 2);

        mvc.perform(withToken(token("order_corretor", tenantId, "CORRETOR"),
                        put("/api/v1/properties/" + property + "/photos/order").contentType("application/json").content("{}")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void propertyOfAnotherTenantReturns404AndIsNotChanged() throws Exception {
        UUID property = newProperty();
        List<String> keys = withPhotos(property, 2);

        mvc.perform(reorder("order_outro", otherTenantId, property, reversed(keys))).andExpect(status().isNotFound());

        assertThat(storedKeysInOrder(property)).containsExactlyElementsOf(keys);
    }

    @Test
    void financeiroCannotReorderAndGets403() throws Exception {
        UUID property = newProperty();
        List<String> keys = withPhotos(property, 2);

        mvc.perform(reorder("order_financeiro", tenantId, property, reversed(keys))).andExpect(status().isForbidden());

        assertThat(storedKeysInOrder(property)).containsExactlyElementsOf(keys);
    }

    /** Reordenar concorrendo com uploads: o lock da linha do imovel evita perder ou duplicar fotos. */
    @Test
    void reorderRacingWithUploadsKeepsEveryPhoto() throws Exception {
        reset(s3Client);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenAnswer(invocation -> {
            Thread.sleep(100);
            return null;
        });
        UUID property = newProperty();
        int existing = 4;
        List<String> keys = withPhotos(property, existing);
        int uploads = 4;
        var pool = Executors.newFixedThreadPool(uploads + 1);
        var start = new CountDownLatch(1);
        try {
            var futures = new ArrayList<java.util.concurrent.Future<Integer>>();
            futures.add(pool.submit(() -> {
                start.await();
                return mvc.perform(reorderAsCorretor(property, reversed(keys))).andReturn().getResponse().getStatus();
            }));
            for (int i = 0; i < uploads; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    var file = new MockMultipartFile("file", "foto.png", "image/png", PNG);
                    return mvc.perform(withToken(token("order_corretor", tenantId, "CORRETOR"),
                            multipart("/api/v1/properties/" + property + "/photos").file(file))).andReturn().getResponse().getStatus();
                }));
            }
            start.countDown();
            for (var future : futures) {
                int statusCode = future.get(30, TimeUnit.SECONDS);
                // a reordenacao pode rodar antes dos uploads terminarem: nesse caso a lista nao bate e devolve 422
                assertThat(statusCode).isIn(200, 422);
            }
        } finally {
            pool.shutdownNow();
        }

        List<String> stored = storedKeysInOrder(property);
        assertEquals(existing + uploads, stored.size());
        assertEquals(existing + uploads, stored.stream().distinct().count());
        assertThat(jdbc.queryForList("SELECT photo_order FROM property_photos WHERE property_id = ? ORDER BY photo_order",
                Integer.class, property)).containsExactlyElementsOf(IntStream.range(0, existing + uploads).boxed().toList());
    }

    /** A migration V4 aplicada a uma tabela com dados pre-existentes (sem photo_order) preserva a ordem por imovel. */
    @Test
    void migrationBackfillNumbersExistingPhotosPerPropertyStartingAtZero() throws Exception {
        String sql = new String(new ClassPathResource("db/migration/V4__add_property_photo_order.sql").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
        String legacy = "property_photos_legacy_test";
        jdbc.execute("CREATE TABLE " + legacy + " (property_id UUID NOT NULL, photos VARCHAR(1024))");
        try {
            UUID a = UUID.randomUUID();
            UUID b = UUID.randomUUID();
            List.of("a1", "a2", "a3").forEach(p -> jdbc.update("INSERT INTO " + legacy + " VALUES (?, ?)", a, p));
            List.of("b1", "b2").forEach(p -> jdbc.update("INSERT INTO " + legacy + " VALUES (?, ?)", b, p));

            jdbc.execute(sql.replace("property_photos", legacy));

            assertThat(jdbc.queryForList("SELECT photos FROM " + legacy + " WHERE property_id = ? ORDER BY photo_order", String.class, a))
                    .containsExactly("a1", "a2", "a3");
            assertThat(jdbc.queryForList("SELECT photo_order FROM " + legacy + " WHERE property_id = ? ORDER BY photo_order", Integer.class, b))
                    .containsExactly(0, 1);
        } finally {
            jdbc.execute("DROP TABLE " + legacy);
        }
    }
}
