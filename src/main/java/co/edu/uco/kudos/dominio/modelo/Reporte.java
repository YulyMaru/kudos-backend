package co.edu.uco.kudos.dominio.modelo;

import co.edu.uco.kudos.dominio.excepcion.CampoObligatorioExcepcion;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Reporte(UUID id, TipoContenido tipoContenido, UUID contenidoId, UUID reportanteId, String motivo,
                      Instant creadoEn) {

    public Reporte {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tipoContenido, "tipoContenido");
        Objects.requireNonNull(contenidoId, "contenidoId");
        Objects.requireNonNull(reportanteId, "reportanteId");
        Objects.requireNonNull(creadoEn, "creadoEn");
        if (motivo == null || motivo.isBlank()) {
            throw new CampoObligatorioExcepcion("motivo");
        }
    }

    public static Reporte crear(TipoContenido tipo, UUID contenidoId, UUID reportanteId, String motivo, Instant ahora) {
        return new Reporte(UUID.randomUUID(), tipo, contenidoId, reportanteId, motivo == null ? null : motivo.trim(),
                ahora);
    }
}
