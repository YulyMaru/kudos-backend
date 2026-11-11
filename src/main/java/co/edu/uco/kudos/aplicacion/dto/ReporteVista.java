package co.edu.uco.kudos.aplicacion.dto;


import co.edu.uco.kudos.dominio.modelo.TipoContenido;

import java.time.Instant;
import java.util.UUID;

public record ReporteVista(UUID id, TipoContenido tipoContenido, UUID contenidoId, PersonaVista reportante,
                           String motivo, Instant creadoEn, String extractoContenido) {
}
