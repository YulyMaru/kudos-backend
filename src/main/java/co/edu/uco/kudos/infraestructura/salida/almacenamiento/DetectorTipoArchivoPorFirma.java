package co.edu.uco.kudos.infraestructura.salida.almacenamiento;

import co.edu.uco.kudos.aplicacion.puerto.salida.DetectorTipoArchivo;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class DetectorTipoArchivoPorFirma implements DetectorTipoArchivo {

    private static final Map<String, byte[]> FIRMAS = Map.of(
            "image/png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A},
            "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "application/pdf", new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D});

    @Override
    public Optional<String> detectar(byte[] contenido) {
        if (contenido == null) {
            return Optional.empty();
        }
        return FIRMAS.entrySet().stream()
                .filter(firma -> empiezaCon(contenido, firma.getValue()))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    private static boolean empiezaCon(byte[] contenido, byte[] firma) {
        if (contenido.length < firma.length) {
            return false;
        }
        for (int i = 0; i < firma.length; i++) {
            if (contenido[i] != firma[i]) {
                return false;
            }
        }
        return true;
    }
}
