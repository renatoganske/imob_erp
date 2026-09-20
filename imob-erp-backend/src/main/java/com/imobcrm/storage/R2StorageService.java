package com.imobcrm.storage;

import com.imobcrm.shared.exception.ExternalServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkException;
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
        } catch (SdkException e) {
            throw storageFailure("upload", key, e);
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
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(r2Properties.bucketName())
                    .key(key)
                    .build());
        } catch (SdkException e) {
            throw storageFailure("delete", key, e);
        }
        log.info("Arquivo removido do R2: key={}", key);
    }

    /**
     * Falha do R2 (credencial/permissao, bucket, rede): 502 com o codigo do erro em vez de um 500 generico,
     * e a causa completa no log. Nunca inclui credenciais na mensagem nem no log.
     */
    private ExternalServiceException storageFailure(String operation, String key, SdkException e) {
        String cause = describe(e);
        log.error("Falha no armazenamento R2: op={} bucket={} key={} causa={}", operation, r2Properties.bucketName(), key, cause, e);
        return new ExternalServiceException("Falha ao acessar o armazenamento de arquivos: " + cause, "STORAGE_UNAVAILABLE");
    }

    /** Codigo do erro do S3/R2 (ex.: AccessDenied, NoSuchBucket) ou o tipo da excecao para falhas de rede/credencial. */
    static String describe(SdkException e) {
        if (e instanceof AwsServiceException aws && aws.awsErrorDetails() != null && aws.awsErrorDetails().errorCode() != null) {
            return aws.awsErrorDetails().errorCode() + " (HTTP " + aws.statusCode() + ")";
        }
        return e.getClass().getSimpleName();
    }
}
