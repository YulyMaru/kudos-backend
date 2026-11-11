package co.edu.uco.kudos.aplicacion.dto;

import co.edu.uco.kudos.dominio.modelo.Rol;

import java.time.Instant;
import java.util.UUID;

public record SesionResultado(String tokenAcceso, Instant expiraAcceso, String tokenRefresco,
                              Instant expiraRefresco, UUID usuarioId, String nombreCompleto, Rol rol) {

    @Override
    public String toString() {
        return "SesionResultado[usuarioId=" + usuarioId + ", rol=" + rol + ", expiraAcceso=" + expiraAcceso + "]";
    }
}
