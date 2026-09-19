package com.imobcrm.storage;

import com.imobcrm.shared.exception.PayloadTooLargeException;
import com.imobcrm.shared.exception.UnsupportedMediaTypeException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UploadValidatorTest {

    // Cabecalhos reais de cada formato, seguidos de bytes quaisquer.
    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F', 0, 1};
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13};
    static final byte[] WEBP = {'R', 'I', 'F', 'F', 0x24, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' '};
    static final byte[] PDF = "%PDF-1.7\n%conteudo".getBytes(StandardCharsets.US_ASCII);

    private final UploadValidator validator = new UploadValidator();

    private static MockMultipartFile file(String name, String declaredType, byte[] content) {
        return new MockMultipartFile("file", name, declaredType, content);
    }

    @Test
    void detectsJpegPngAndWebpByMagicBytes() {
        assertThat(validator.validate(file("a.jpg", "image/jpeg", JPEG), UploadValidator.IMAGES)).isEqualTo(FileKind.JPEG);
        assertThat(validator.validate(file("a.png", "image/png", PNG), UploadValidator.IMAGES)).isEqualTo(FileKind.PNG);
        assertThat(validator.validate(file("a.webp", "image/webp", WEBP), UploadValidator.IMAGES)).isEqualTo(FileKind.WEBP);
    }

    @Test
    void trustsContentNotTheDeclaredExtensionOrContentType() {
        // PNG de verdade enviado como ".jpg" / "image/jpeg": o tipo real prevalece
        assertThat(validator.validate(file("foto.jpg", "image/jpeg", PNG), UploadValidator.IMAGES)).isEqualTo(FileKind.PNG);
    }

    @Test
    void rejectsTextDisguisedAsImage() {
        var fake = file("foto.jpg", "image/jpeg", "isto nao e uma imagem".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> validator.validate(fake, UploadValidator.IMAGES))
                .isInstanceOf(UnsupportedMediaTypeException.class);
    }

    @Test
    void rejectsExecutableAndHtmlDisguisedAsImage() {
        var exe = file("foto.png", "image/png", new byte[]{'M', 'Z', (byte) 0x90, 0, 3, 0, 0, 0, 4, 0, 0, 0});
        var html = file("foto.png", "image/png", "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> validator.validate(exe, UploadValidator.IMAGES)).isInstanceOf(UnsupportedMediaTypeException.class);
        assertThatThrownBy(() -> validator.validate(html, UploadValidator.IMAGES)).isInstanceOf(UnsupportedMediaTypeException.class);
    }

    @Test
    void rejectsRiffThatIsNotWebp() {
        byte[] wav = {'R', 'I', 'F', 'F', 0x24, 0, 0, 0, 'W', 'A', 'V', 'E', 'f', 'm', 't', ' '};

        assertThatThrownBy(() -> validator.validate(file("a.webp", "image/webp", wav), UploadValidator.IMAGES))
                .isInstanceOf(UnsupportedMediaTypeException.class);
    }

    @Test
    void rejectsRealFormatThatIsNotAllowedForTheUseCase() {
        assertThatThrownBy(() -> validator.validate(file("doc.pdf", "application/pdf", PDF), UploadValidator.IMAGES))
                .isInstanceOf(UnsupportedMediaTypeException.class);
        assertThat(validator.validate(file("doc.pdf", "application/pdf", PDF), Set.of(FileKind.PDF))).isEqualTo(FileKind.PDF);
    }

    @Test
    void rejectsEmptyAndTruncatedFiles() {
        assertThatThrownBy(() -> validator.validate(file("a.jpg", "image/jpeg", new byte[0]), UploadValidator.IMAGES))
                .isInstanceOf(UnsupportedMediaTypeException.class);
        assertThatThrownBy(() -> validator.validate(file("a.png", "image/png", new byte[]{(byte) 0x89, 'P'}), UploadValidator.IMAGES))
                .isInstanceOf(UnsupportedMediaTypeException.class);
        assertThatThrownBy(() -> validator.validate(null, UploadValidator.IMAGES))
                .isInstanceOf(UnsupportedMediaTypeException.class);
    }

    @Test
    void rejectsFileAboveTenMegabytesButAcceptsExactlyTenMegabytes() {
        byte[] exactly = new byte[(int) UploadValidator.MAX_SIZE_BYTES];
        System.arraycopy(JPEG, 0, exactly, 0, JPEG.length);
        byte[] above = new byte[(int) UploadValidator.MAX_SIZE_BYTES + 1];
        System.arraycopy(JPEG, 0, above, 0, JPEG.length);

        assertThat(validator.validate(file("a.jpg", "image/jpeg", exactly), UploadValidator.IMAGES)).isEqualTo(FileKind.JPEG);
        assertThatThrownBy(() -> validator.validate(file("a.jpg", "image/jpeg", above), UploadValidator.IMAGES))
                .isInstanceOf(PayloadTooLargeException.class);
    }
}
