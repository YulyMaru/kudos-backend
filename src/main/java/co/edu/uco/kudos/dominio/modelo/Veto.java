package co.edu.uco.kudos.dominio.modelo;

import co.edu.uco.kudos.dominio.excepcion.CampoObligatorioExcepcion;
import co.edu.uco.kudos.dominio.regla.ReglaModeracion;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Veto(UUID id, TipoContenido tipoContenido, UUID contenidoId, UUID moderadorId, String motivo,
                   Instant creadoEn) {

    public Veto {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tipoContenido, "tipoContenido");
        Objects.requireNonNull(contenidoId, "contenidoId");
        Objects.requireNonNull(moderadorId, "moderadorId");
        Objects.requireNonNull(creadoEn, "creadoEn");
        if (motivo == null || motivo.isBlank()) {
            throw new CampoObligatorioExcepcion("motivo");
        }
    }

    public static Veto aplicar(Usuario moderador, TipoContenido tipo, UUID contenidoId, String motivo, Instant ahora) {
        ReglaModeracion.validarPuedeVetar(moderador);
        return new Veto(UUID.randomUUID(), tipo, contenidoId, moderador.getId(), motivo == null ? null : motivo.trim(),
                ahora);
    }
}
