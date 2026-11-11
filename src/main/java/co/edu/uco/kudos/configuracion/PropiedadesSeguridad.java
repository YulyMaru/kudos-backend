package co.edu.uco.kudos.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "kudos.seguridad")
public record PropiedadesSeguridad(String jwtSecreto, List<String> origenesPermitidos, String emisorJwt,
                                   Duration duracionTokenAcceso, Duration duracionTokenRefresco,
                                   Integer maximoIntentosFallidos, Duration duracionBloqueo, Integer costoBcrypt) {

    private static final int LONGITUD_MINIMA_SECRETO_BYTES = 32;

    public PropiedadesSeguridad {
        if (jwtSecreto == null || jwtSecreto.getBytes(StandardCharsets.UTF_8).length < LONGITUD_MINIMA_SECRETO_BYTES) {
            throw new IllegalStateException(
                    "La variable JWT_SECRETO es obligatoria y debe tener al menos 32 bytes");
        }
        origenesPermitidos = origenesPermitidos == null ? List.of() : List.copyOf(origenesPermitidos);
        emisorJwt = emisorJwt == null || emisorJwt.isBlank() ? "uco-kudos" : emisorJwt;
        duracionTokenAcceso = duracionTokenAcceso == null ? Duration.ofMinutes(15) : duracionTokenAcceso;
        duracionTokenRefresco = duracionTokenRefresco == null ? Duration.ofDays(7) : duracionTokenRefresco;
        maximoIntentosFallidos = maximoIntentosFallidos == null ? 5 : maximoIntentosFallidos;
        duracionBloqueo = duracionBloqueo == null ? Duration.ofMinutes(15) : duracionBloqueo;
        costoBcrypt = costoBcrypt == null ? 12 : costoBcrypt;
    }

    @Override
    public String toString() {
        return "PropiedadesSeguridad[jwtSecreto=****, emisorJwt=" + emisorJwt + "]";
    }
}
