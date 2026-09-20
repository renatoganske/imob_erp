package com.imobcrm.property;

import com.imobcrm.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Upload de fotos de imoveis (IMOB-21) com o stack real; apenas o cliente S3/R2 e substituido. */
class PropertyPhotoUploadTest extends IntegrationTestBase {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13, 'I', 'H', 'D', 'R'};

    private static UUID tenantId;

    @MockBean
    private S3Client s3Client;

    @BeforeAll
    static void seed(@Autowired JdbcTemplate jdbc) {
        if (tenantId != null) {
            return;
        }
        tenantId = UUID.randomUUID();
        jdbc.update("INSERT INTO tenants (id, name, slug, plan) VALUES (?, 'Imobiliaria Fotos', 'imob-photos', 'BASIC')", tenantId);
        jdbc.update("INSERT INTO users (id, tenant_id, clerk_user_id, name, email, role) "
                + "VALUES (?, ?, 'photos_corretor', 'Corretor', 'c@fotos.com', 'CORRETOR')", UUID.randomUUID(), tenantId);
    }

    private UUID newProperty() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO properties (id, tenant_id, type, title, address, city, price, status, purpose) "
                + "VALUES (?, ?, 'APARTAMENTO', 'Imovel', 'Rua 1', 'Curitiba', 500000, 'DISPONIVEL', 'VENDA')", id, tenantId);
        return id;
    }

    private MockHttpServletRequestBuilder upload(UUID property, String filename, String declaredType, byte[] content) throws Exception {
        var file = new MockMultipartFile("file", filename, declaredType, content);
        return withToken(token("photos_corretor", tenantId, "CORRETOR"), multipart("/api/v1/properties/" + property + "/photos").file(file));
    }

    @Test
    void realImageIsStoredWithTenantScopedKeyAndDetectedExtension() throws Exception {
        reset(s3Client);
        UUID property = newProperty();

        // arquivo PNG real enviado com nome/tipo declarado de JPEG: a chave usa o tipo real (.png)
        mvc.perform(upload(property, "foto.jpg", "image/jpeg", PNG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photos[0]", matchesPattern(
                        ".*/" + tenantId + "/properties/" + property + "/[0-9a-f-]{36}\\.png")));

        verify(s3Client).putObject(
                argThat((PutObjectRequest r) -> r.key().startsWith(tenantId + "/properties/" + property + "/")
                        && r.key().endsWith(".png") && "image/png".equals(r.contentType())),
                any(RequestBody.class));
    }

    @Test
    void fakeImageWithImageExtensionReturns415AndIsNotStored() throws Exception {
        reset(s3Client);
        UUID property = newProperty();

        mvc.perform(upload(property, "foto.jpg", "image/jpeg", "nao sou uma imagem".getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
        mvc.perform(upload(property, "contrato.pdf", "application/pdf", "%PDF-1.7 x".getBytes(StandardCharsets.US_ASCII)))
                .andExpect(status().isUnsupportedMediaType());

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM property_photos WHERE property_id = ?", Integer.class, property));
    }

    @Test
    void fileAboveTenMegabytesReturns413() throws Exception {
        reset(s3Client);
        UUID property = newProperty();
        byte[] big = new byte[10 * 1024 * 1024 + 1];
        System.arraycopy(PNG, 0, big, 0, PNG.length);

        mvc.perform(upload(property, "grande.png", "image/png", big))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("PAYLOAD_TOO_LARGE"));

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void twentyFirstPhotoReturns422() throws Exception {
        reset(s3Client);
        UUID property = newProperty();
        for (int i = 0; i < 20; i++) {
            jdbc.update("INSERT INTO property_photos (property_id, photos) VALUES (?, ?)", property, "http://x/" + i + ".png");
        }

        mvc.perform(upload(property, "extra.png", "image/png", PNG))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("MAX_PHOTOS_EXCEEDED"));

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        assertEquals(20, jdbc.queryForObject("SELECT count(*) FROM property_photos WHERE property_id = ?", Integer.class, property));
    }

    @Test
    void r2RefusingTheUploadReturns502WithTheS3ErrorCodeAndStoresNothing() throws Exception {
        reset(s3Client);
        UUID property = newProperty();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenThrow(s3Error("AccessDenied", 403));

        mvc.perform(upload(property, "foto.png", "image/png", PNG))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("STORAGE_UNAVAILABLE"))
                .andExpect(jsonPath("$.error", containsString("AccessDenied")));

        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM property_photos WHERE property_id = ?", Integer.class, property));
    }

    @Test
    void networkOrCredentialFailureAlsoReturns502NotAGeneric500() throws Exception {
        reset(s3Client);
        UUID property = newProperty();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(SdkClientException.create("Unable to load credentials"));

        mvc.perform(upload(property, "foto.png", "image/png", PNG))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("STORAGE_UNAVAILABLE"))
                .andExpect(jsonPath("$.error", containsString("SdkClientException")));
    }

    @Test
    void r2FailingOnDeleteReturns502AndKeepsThePhotoInTheList() throws Exception {
        reset(s3Client);
        UUID property = newProperty();
        String name = UUID.randomUUID() + ".png";
        jdbc.update("INSERT INTO property_photos (property_id, photos) VALUES (?, ?)",
                property, "http://localhost/fake-r2/" + tenantId + "/properties/" + property + "/" + name);
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenThrow(s3Error("NoSuchBucket", 404));

        mvc.perform(withToken(token("photos_corretor", tenantId, "CORRETOR"),
                        delete("/api/v1/properties/" + property + "/photos/" + name)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("STORAGE_UNAVAILABLE"));

        // a transacao foi revertida: a foto continua listada
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM property_photos WHERE property_id = ?", Integer.class, property));
    }

    /** IMOB-44: o front envia varias fotos em paralelo; nenhuma pode se perder por read-modify-write concorrente. */
    @Test
    void concurrentUploadsToTheSamePropertyKeepEveryPhoto() throws Exception {
        reset(s3Client);
        // R2 lento: alarga a janela em que duas requisicoes leem a mesma lista de fotos
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenAnswer(invocation -> {
            Thread.sleep(150);
            return null;
        });
        UUID property = newProperty();
        // Ja existem fotos: e ai que o "apaga tudo e regrava a lista" de uma transacao destroi a foto da outra
        int existing = 2;
        for (int i = 0; i < existing; i++) {
            jdbc.update("INSERT INTO property_photos (property_id, photos) VALUES (?, ?)", property, "http://x/existente-" + i + ".png");
        }
        int uploads = 6;
        var pool = java.util.concurrent.Executors.newFixedThreadPool(uploads);
        var start = new java.util.concurrent.CountDownLatch(1);
        try {
            var futures = new java.util.ArrayList<java.util.concurrent.Future<Integer>>();
            for (int i = 0; i < uploads; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return mvc.perform(upload(property, "foto.png", "image/png", PNG)).andReturn().getResponse().getStatus();
                }));
            }
            start.countDown();
            for (var future : futures) {
                assertEquals(200, future.get(30, java.util.concurrent.TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }

        assertEquals(existing + uploads, jdbc.queryForObject("SELECT count(*) FROM property_photos WHERE property_id = ?", Integer.class, property));
        assertEquals(existing + uploads, jdbc.queryForObject("SELECT count(DISTINCT photos) FROM property_photos WHERE property_id = ?", Integer.class, property));
    }

    private static S3Exception s3Error(String code, int httpStatus) {
        return (S3Exception) S3Exception.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorCode(code).errorMessage(code).build())
                .statusCode(httpStatus)
                .build();
    }

    @Test
    void deleteRemovesFromR2AndFromTheList() throws Exception {
        reset(s3Client);
        UUID property = newProperty();
        String name = UUID.randomUUID() + ".png";
        jdbc.update("INSERT INTO property_photos (property_id, photos) VALUES (?, ?)",
                property, "http://localhost/fake-r2/" + tenantId + "/properties/" + property + "/" + name);

        mvc.perform(withToken(token("photos_corretor", tenantId, "CORRETOR"),
                        delete("/api/v1/properties/" + property + "/photos/" + name)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photos").isEmpty());

        verify(s3Client).deleteObject(argThat((DeleteObjectRequest r) ->
                r.key().equals(tenantId + "/properties/" + property + "/" + name)));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM property_photos WHERE property_id = ?", Integer.class, property)).isZero();
    }
}
