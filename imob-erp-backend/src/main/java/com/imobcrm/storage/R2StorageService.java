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
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class R2StorageService {

    private final S3Client s3Client;
    private final StorageProperties r2Properties;

    /**
     * Faz upload de um arquivo ja validado pelo {@link UploadValidator} e retorna a URL publica.
     * Extensao e Content-Type vem do tipo real detectado, nao do que o cliente informou.
     * Chave (specs.md#7): {keyPrefix}/{uuid}.{ext}, ex. {tenantId}/contracts/{contractId}/{uuid}.pdf
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

    /** Chave do objeto a partir de uma URL publica gerada por este servico; vazio para URLs de outra origem. */
    public Optional<String> keyFromUrl(String url) {
        String prefix = r2Properties.publicUrl() + "/";
        return url != null && url.startsWith(prefix) ? Optional.of(url.substring(prefix.length())) : Optional.empty();
    }

    public void delete(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(r2Properties.bucketName())
                .key(key)
                .build());
        log.info("Arquivo removido do R2: key={}", key);
    }
}
