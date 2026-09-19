package com.imobcrm.storage;

/** Tipos de arquivo aceitos em uploads, identificados pelo conteudo real (magic bytes). */
public enum FileKind {
    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp"),
    PDF("application/pdf", "pdf");

    private final String contentType;
    private final String extension;

    FileKind(String contentType, String extension) {
        this.contentType = contentType;
        this.extension = extension;
    }

    public String contentType() {
        return contentType;
    }

    public String extension() {
        return extension;
    }
}
