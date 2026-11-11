package co.edu.uco.kudos.aplicacion.dto;


import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;

import java.time.Instant;
import java.util.UUID;

public record FiltroAuditoria(Instant desde, Instant hasta, UUID actorId, AccionAuditoria accion, String recursoTipo,
                              UUID recursoId, UUID idCorrelacion) {
}
