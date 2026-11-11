package co.edu.uco.kudos.dominio.modelo;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record EntradaAuditoria(UUID id, Instant fechaHora, UUID idCorrelacion, UUID actorId, Rol rolActor,
                               AccionAuditoria accion, String recursoTipo, UUID recursoId, Canal canal,
                               ResultadoAuditoria resultado, String detalle) {

    public EntradaAuditoria {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(fechaHora, "fechaHora");
        Objects.requireNonNull(accion, "accion");
        Objects.requireNonNull(resultado, "resultado");
    }

    public static EntradaAuditoria de(Instant fechaHora, UUID idCorrelacion, Usuario actor, AccionAuditoria accion,
                                      String recursoTipo, UUID recursoId, Canal canal,
                                      ResultadoAuditoria resultado, String detalle) {
        UUID actorId = actor == null ? null : actor.getId();
        Rol rolActor = actor == null ? null : actor.getRol();
        return new EntradaAuditoria(UUID.randomUUID(), fechaHora, idCorrelacion, actorId, rolActor, accion,
                recursoTipo, recursoId, canal, resultado, detalle);
    }
}
