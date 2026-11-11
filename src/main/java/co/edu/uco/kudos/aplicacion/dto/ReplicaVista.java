package co.edu.uco.kudos.aplicacion.dto;


import java.time.Instant;
import java.util.UUID;

public record ReplicaVista(UUID id, UUID kudoId, UUID replicaPadreId, PersonaVista autor, String contenido,
                           Instant fechaPublicacion, long cantidadRespuestas, double promedioCalificacion,
                           long cantidadCalificaciones) {
}
