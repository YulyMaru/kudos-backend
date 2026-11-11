package co.edu.uco.kudos.dominio.modelo;

import co.edu.uco.kudos.dominio.excepcion.CampoObligatorioExcepcion;
import co.edu.uco.kudos.dominio.excepcion.ReglaNegocioExcepcion;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SolicitudRegistro(UUID id, UUID usuarioId, Rol rolSolicitado, UUID facultadId, UUID programaId,
                                EstadoAprobacion estado, UUID decididaPor, String motivoRechazo,
                                Instant politicaAceptadaEn, Instant creadaEn, Instant decididaEn) {

    public SolicitudRegistro {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(rolSolicitado, "rolSolicitado");
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(politicaAceptadaEn, "politicaAceptadaEn");
        Objects.requireNonNull(creadaEn, "creadaEn");
    }

    public static SolicitudRegistro crear(Usuario usuario, UUID facultadId, UUID programaId, Instant ahora) {
        return new SolicitudRegistro(UUID.randomUUID(), usuario.getId(), usuario.getRol(), facultadId, programaId,
                EstadoAprobacion.PENDIENTE, null, null, ahora, ahora, null);
    }

    public boolean estaAprobada() {
        return estado == EstadoAprobacion.APROBADA;
    }

    public SolicitudRegistro aprobar(UUID aprobadorId, Instant ahora) {
        validarPendiente();
        return new SolicitudRegistro(id, usuarioId, rolSolicitado, facultadId, programaId, EstadoAprobacion.APROBADA,
                aprobadorId, null, politicaAceptadaEn, creadaEn, ahora);
    }

    public SolicitudRegistro rechazar(UUID aprobadorId, String motivo, Instant ahora) {
        validarPendiente();
        if (motivo == null || motivo.isBlank()) {
            throw new CampoObligatorioExcepcion("motivo");
        }
        return new SolicitudRegistro(id, usuarioId, rolSolicitado, facultadId, programaId, EstadoAprobacion.RECHAZADA,
                aprobadorId, motivo.trim(), politicaAceptadaEn, creadaEn, ahora);
    }

    private void validarPendiente() {
        if (estado != EstadoAprobacion.PENDIENTE) {
            throw new ReglaNegocioExcepcion("SOLICITUD_YA_DECIDIDA", "solicitud",
                    "La solicitud ya fue " + estado.name().toLowerCase());
        }
    }
}
