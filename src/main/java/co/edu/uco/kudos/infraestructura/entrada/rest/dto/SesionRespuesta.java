package co.edu.uco.kudos.infraestructura.entrada.rest.dto;

import co.edu.uco.kudos.aplicacion.dto.SesionResultado;

import java.time.Instant;
import java.util.UUID;

public record SesionRespuesta(String tokenAcceso, String tipoToken, Instant expiraEn, String tokenRefresco,
                              Instant expiraRefrescoEn, UUID usuarioId, String nombreCompleto, String rol) {

    public static SesionRespuesta desde(SesionResultado resultado) {
        return new SesionRespuesta(resultado.tokenAcceso(), "Bearer", resultado.expiraAcceso(),
                resultado.tokenRefresco(), resultado.expiraRefresco(), resultado.usuarioId(),
                resultado.nombreCompleto(), resultado.rol().name());
    }

    @Override
    public String toString() {
        return "SesionRespuesta[usuarioId=" + usuarioId + ", rol=" + rol + ", expiraEn=" + expiraEn + "]";
    }
}
