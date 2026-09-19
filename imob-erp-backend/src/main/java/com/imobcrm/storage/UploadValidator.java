package com.imobcrm.storage;

import com.imobcrm.shared.exception.PayloadTooLargeException;
import com.imobcrm.shared.exception.UnsupportedMediaTypeException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Set;

/**
 * Validador unico de uploads (fotos de imoveis e documentos de contrato). O tipo vem dos
 * magic bytes do conteudo, nunca do Content-Type nem da extensao informados pelo cliente.
 */
@Component
public class UploadValidator {

    public static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

    public static final Set<FileKind> IMAGES = Set.of(FileKind.JPEG, FileKind.PNG, FileKind.WEBP);

    private static final int HEADER_BYTES = 12;

    /**
     * @throws PayloadTooLargeException      arquivo acima de 10MB (413)
     * @throws UnsupportedMediaTypeException vazio, ilegivel ou de tipo fora de {@code allowed} (415)
     */
    public FileKind validate(MultipartFile file, Set<FileKind> allowed) {
        if (file == null || file.isEmpty()) {
            throw new UnsupportedMediaTypeException("Arquivo vazio ou nao enviado");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new PayloadTooLargeException("Arquivo acima do limite de 10MB");
        }
        FileKind kind = detect(readHeader(file));
        if (kind == null || !allowed.contains(kind)) {
            throw new UnsupportedMediaTypeException("Tipo de arquivo nao suportado. Permitidos: " + describe(allowed));
        }
        return kind;
    }

    private static byte[] readHeader(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return in.readNBytes(HEADER_BYTES);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler o arquivo enviado", e);
        }
    }

    private static FileKind detect(byte[] h) {
        if (startsWith(h, 0xFF, 0xD8, 0xFF)) {
            return FileKind.JPEG;
        }
        if (startsWith(h, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return FileKind.PNG;
        }
        // WebP: "RIFF" <tamanho de 4 bytes> "WEBP"
        if (h.length >= 12 && startsWith(h, 'R', 'I', 'F', 'F') && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') {
            return FileKind.WEBP;
        }
        if (startsWith(h, '%', 'P', 'D', 'F', '-')) {
            return FileKind.PDF;
        }
        return null;
    }

    private static boolean startsWith(byte[] data, int... signature) {
        if (data.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((data[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private static String describe(Set<FileKind> allowed) {
        return String.join(", ", allowed.stream().map(FileKind::name).sorted().toList());
    }
}
