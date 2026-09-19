package com.imobcrm.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class R2StorageService {

    private final S3Client s3Client;
    private final StorageProperties r2Properties;

    /**
     * Faz upload de um arquivo para o bucket R2 sob o prefixo informado e
     * retorna a URL publica. Chaves seguem o padrao definido em specs.md#7:
     * {tenantId}/properties/{propertyId}/{uuid}.{ext} ou equivalente para contratos.
     */
    public String upload(String keyPrefix, MultipartFile file) {
        // Legado: extensao e Content-Type vem do cliente. Contratos usam este caminho ate a 4.2 (IMOB-22)
        // passar a validar o PDF com o UploadValidator e a chamar a sobrecarga com FileKind.
        String extension = extractExtension(file.getOriginalFilename());
        String key = keyPrefix + "/" + UUID.randomUUID() + extension;

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(r2Properties.bucketName())
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException e) {
            log.error("Falha ao enviar arquivo para o R2: key={}", key, e);
            throw new UncheckedIOException("Falha ao ler o arquivo para upload", e);
        }

        log.info("Arquivo enviado ao R2: key={} size={}b", key, file.getSize());
        return r2Properties.publicUrl() + "/" + key;
    }

    /**
     * Upload de arquivo ja validado pelo {@link UploadValidator}: extensao e Content-Type vem do tipo
     * real detectado, nao do que o cliente informou. Chave: {keyPrefix}/{uuid}.{ext}.
     */
    public String upload(String keyPrefix, MultipartFile file, FileKind kind) {
        String key = keyPrefix + "/" + UUID.randomUUID() + "." + kind.extension();
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(r2Properties.bucketName())
                            .key(key)
                            .contentType(kind.contentType())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException e) {
            log.error("Falha ao enviar arquivo para o R2: key={}", key, e);
            throw new UncheckedIOException("Falha ao ler o arquivo para upload", e);
        }
        log.info("Arquivo enviado ao R2: key={} type={} size={}b", key, kind, file.getSize());
        return r2Properties.publicUrl() + "/" + key;
    }

    public void delete(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(r2Properties.bucketName())
                .key(key)
                .build());
        log.info("Arquivo removido do R2: key={}", key);
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.'));
    }
}
