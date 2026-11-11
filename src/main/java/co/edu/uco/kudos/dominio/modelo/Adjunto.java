package co.edu.uco.kudos.dominio.modelo;

import java.util.Objects;

public record Adjunto(String nombreOriginal, String tipoContenido, long tamanoBytes,
                      String hashSha256, String claveObjeto) {

    public Adjunto {
        Objects.requireNonNull(nombreOriginal, "nombreOriginal");
        Objects.requireNonNull(tipoContenido, "tipoContenido");
        Objects.requireNonNull(hashSha256, "hashSha256");
        Objects.requireNonNull(claveObjeto, "claveObjeto");
        if (tamanoBytes <= 0) {
            throw new IllegalArgumentException("tamanoBytes debe ser positivo");
        }
    }
}
