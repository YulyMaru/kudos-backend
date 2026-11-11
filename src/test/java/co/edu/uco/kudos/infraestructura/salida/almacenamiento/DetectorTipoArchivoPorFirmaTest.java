package co.edu.uco.kudos.infraestructura.salida.almacenamiento;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DetectorTipoArchivoPorFirmaTest {

    private final DetectorTipoArchivoPorFirma detector = new DetectorTipoArchivoPorFirma();

    @Test
    void detectaPng() {
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};

        assertThat(detector.detectar(png)).contains("image/png");
    }

    @Test
    void detectaJpeg() {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00};

        assertThat(detector.detectar(jpeg)).contains("image/jpeg");
    }

    @Test
    void detectaPdf() {
        byte[] pdf = "%PDF-1.7 contenido".getBytes(StandardCharsets.US_ASCII);

        assertThat(detector.detectar(pdf)).contains("application/pdf");
    }

    @Test
    void noReconoceUnEjecutableAunqueSeLlamePng() {
        byte[] ejecutable = {'M', 'Z', (byte) 0x90, 0x00};

        assertThat(detector.detectar(ejecutable)).isEmpty();
    }

    @Test
    void noReconoceContenidoVacioNiMasCortoQueLaFirma() {
        assertThat(detector.detectar(new byte[0])).isEmpty();
        assertThat(detector.detectar(new byte[]{(byte) 0x89, 0x50})).isEmpty();
        assertThat(detector.detectar(null)).isEmpty();
    }
}
