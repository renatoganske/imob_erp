package com.imobcrm.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "r2")
public record StorageProperties(
        String accountId,
        String accessKeyId,
        String secretAccessKey,
        String bucketName,
        String publicUrl
) {
}
