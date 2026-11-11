package co.edu.uco.kudos.aplicacion.dto;


import co.edu.uco.kudos.dominio.modelo.Rol;

import java.time.Instant;
import java.util.UUID;

public record SolicitudPendienteVista(UUID id, UUID usuarioId, String nombreCompleto, String correo,
                                      Rol rolSolicitado, String facultad, String programa, boolean correoVerificado,
                                      boolean celularVerificado, Instant creadaEn) {
}
