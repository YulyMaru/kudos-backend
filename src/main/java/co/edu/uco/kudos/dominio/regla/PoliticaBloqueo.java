package co.edu.uco.kudos.dominio.regla;

import java.time.Duration;
import java.util.Objects;

public record PoliticaBloqueo(int maximoIntentosFallidos, Duration duracionBloqueo) {

    public PoliticaBloqueo {
        Objects.requireNonNull(duracionBloqueo, "duracionBloqueo");
        if (maximoIntentosFallidos < 1) {
            throw new IllegalArgumentException("maximoIntentosFallidos debe ser al menos 1");
        }
        if (duracionBloqueo.isNegative() || duracionBloqueo.isZero()) {
            throw new IllegalArgumentException("duracionBloqueo debe ser positiva");
        }
    }

    public boolean debeBloquear(int intentosFallidosConsecutivos) {
        return intentosFallidosConsecutivos >= maximoIntentosFallidos;
    }
}
