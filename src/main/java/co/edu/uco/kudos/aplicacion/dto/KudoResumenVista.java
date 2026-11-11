package co.edu.uco.kudos.aplicacion.dto;


import java.time.Instant;
import java.util.UUID;

public record KudoResumenVista(UUID id, PersonaVista autor, PersonaVista destinatario, CategoriaVista categoria,
                               String extracto, boolean contenidoTruncado, Instant fechaPublicacion,
                               int cantidadAdjuntos, long cantidadReplicas, double promedioCalificacion,
                               long cantidadCalificaciones) {
}
