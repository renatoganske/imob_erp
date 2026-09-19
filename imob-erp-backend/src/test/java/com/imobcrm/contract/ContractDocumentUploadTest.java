package com.imobcrm.contract;

import com.imobcrm.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Upload do PDF do contrato (IMOB-22) com o stack real; apenas o cliente S3/R2 e substituido. */
class ContractDocumentUploadTest extends IntegrationTestBase {

    private static final byte[] PDF = "%PDF-1.7\n1 0 obj\n<< >>\nendobj\n".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13};
    private static final String PUBLIC_URL = "http://localhost/fake-r2";

    private static UUID tenantId, agentId, propertyId;

    @MockBean
    private S3Client s3Client;

    @BeforeAll
    static void seed(@Autowired JdbcTemplate jdbc) {
        if (tenantId != null) {
            return;
        }
        tenantId = UUID.randomUUID();
        agentId = UUID.randomUUID();
        propertyId = UUID.randomUUID();
        jdbc.update("INSERT INTO tenants (id, name, slug, plan) VALUES (?, 'Imobiliaria Docs', 'imob-docs', 'BASIC')", tenantId);
        jdbc.update("INSERT INTO users (id, tenant_id, clerk_user_id, name, email, role, commission_rate) "
                + "VALUES (?, ?, 'docs_financeiro', 'Financeiro', 'f@docs.com', 'FINANCEIRO', 5.00)", agentId, tenantId);
        jdbc.update("INSERT INTO properties (id, tenant_id, type, title, address, city, price, status, purpose) "
                + "VALUES (?, ?, 'APARTAMENTO', 'Imovel', 'Rua 1', 'Curitiba', 500000, 'DISPONIVEL', 'VENDA')", propertyId, tenantId);
    }

    private UUID newContract(String documentUrl) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO contracts (id, tenant_id, property_id, agent_id, type, status, value, start_date, "
                + "buyer_name, buyer_document, owner_name, owner_document, document_url) "
                + "VALUES (?, ?, ?, ?, 'COMPRA_VENDA', 'RASCUNHO', 500000, current_date, 'Comprador', '111', 'Dono', '222', ?)",
                id, tenantId, propertyId, agentId, documentUrl);
        return id;
    }

    private MockHttpServletRequestBuilder upload(UUID contract, String filename, String declaredType, byte[] content) throws Exception {
        var file = new MockMultipartFile("file", filename, declaredType, content);
        return withToken(token("docs_financeiro", tenantId, "FINANCEIRO"),
                multipart("/api/v1/contracts/" + contract + "/document").file(file));
    }

    private String storedUrl(UUID contract) {
        return jdbc.queryForObject("SELECT document_url FROM contracts WHERE id = ?", String.class, contract);
    }

    @Test
    void pdfIsStoredWithTenantScopedKeyAndPdfExtension() throws Exception {
        reset(s3Client);
        UUID contract = newContract(null);

        // PDF real enviado com nome/tipo declarados de imagem: o tipo real (.pdf) prevalece
        mvc.perform(upload(contract, "contrato.png", "image/png", PDF))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentUrl", matchesPattern(
                        PUBLIC_URL + "/" + tenantId + "/contracts/" + contract + "/[0-9a-f-]{36}\\.pdf")));

        verify(s3Client).putObject(
                argThat((PutObjectRequest r) -> r.key().startsWith(tenantId + "/contracts/" + contract + "/")
                        && r.key().endsWith(".pdf") && "application/pdf".equals(r.contentType())),
                any(RequestBody.class));
        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void nonPdfReturns415KeepsTheCurrentDocumentAndStoresNothing() throws Exception {
        reset(s3Client);
        String current = PUBLIC_URL + "/" + tenantId + "/contracts/x/atual.pdf";
        UUID contract = newContract(current);

        mvc.perform(upload(contract, "contrato.pdf", "application/pdf", PNG))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
        mvc.perform(upload(contract, "contrato.pdf", "application/pdf", "texto puro".getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isUnsupportedMediaType());

        assertEquals(current, storedUrl(contract));
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void replacingTheDocumentDeletesTheOldObjectFromR2() throws Exception {
        reset(s3Client);
        UUID contract = newContract(null);
        mvc.perform(upload(contract, "v1.pdf", "application/pdf", PDF)).andExpect(status().isOk());
        String firstUrl = storedUrl(contract);
        String firstKey = firstUrl.substring((PUBLIC_URL + "/").length());

        mvc.perform(upload(contract, "v2.pdf", "application/pdf", PDF)).andExpect(status().isOk());

        String secondUrl = storedUrl(contract);
        assertEquals(false, secondUrl.equals(firstUrl));
        verify(s3Client, times(1)).deleteObject(argThat((DeleteObjectRequest r) -> r.key().equals(firstKey)));
        // o objeto novo nunca e apagado
        verify(s3Client, never()).deleteObject(argThat((DeleteObjectRequest r) ->
                secondUrl.endsWith("/" + r.key())));
    }

    @Test
    void previousDocumentOutsideTheContractPrefixIsNeverDeleted() throws Exception {
        reset(s3Client);
        // URL legada/de outra origem ou de outro tenant: a chave nao pertence a este contrato
        UUID contract = newContract(PUBLIC_URL + "/" + UUID.randomUUID() + "/contracts/" + UUID.randomUUID() + "/alheio.pdf");
        mvc.perform(upload(contract, "novo.pdf", "application/pdf", PDF)).andExpect(status().isOk());
        UUID other = newContract("https://outro-host.example/arquivo.pdf");
        mvc.perform(upload(other, "novo.pdf", "application/pdf", PDF)).andExpect(status().isOk());

        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void contractOfAnotherTenantReturns404AndStoresNothing() throws Exception {
        reset(s3Client);
        UUID foreignTenant = UUID.randomUUID();
        jdbc.update("INSERT INTO tenants (id, name, slug, plan) VALUES (?, 'Outra', ?, 'BASIC')", foreignTenant, "imob-docs-" + foreignTenant);
        UUID foreignAgent = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, tenant_id, clerk_user_id, name, email, role) VALUES (?, ?, ?, 'X', 'x@x.com', 'ADMIN')",
                foreignAgent, foreignTenant, "docs_foreign_" + foreignTenant);
        UUID foreignProperty = UUID.randomUUID();
        jdbc.update("INSERT INTO properties (id, tenant_id, type, title, address, city, price, status, purpose) "
                + "VALUES (?, ?, 'APARTAMENTO', 'I', 'R', 'C', 1, 'DISPONIVEL', 'VENDA')", foreignProperty, foreignTenant);
        UUID foreignContract = UUID.randomUUID();
        jdbc.update("INSERT INTO contracts (id, tenant_id, property_id, agent_id, type, status, value, start_date, "
                + "buyer_name, buyer_document, owner_name, owner_document) "
                + "VALUES (?, ?, ?, ?, 'COMPRA_VENDA', 'RASCUNHO', 1, current_date, 'C', '1', 'D', '2')",
                foreignContract, foreignTenant, foreignProperty, foreignAgent);

        mvc.perform(upload(foreignContract, "contrato.pdf", "application/pdf", PDF)).andExpect(status().isNotFound());

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }
}
