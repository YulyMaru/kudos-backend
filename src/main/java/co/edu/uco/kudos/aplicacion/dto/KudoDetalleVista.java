package co.edu.uco.kudos.aplicacion.dto;


import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record KudoDetalleVista(UUID id, PersonaVista autor, PersonaVista destinatario, CategoriaVista categoria,
                               String contenido, Instant fechaPublicacion, List<AdjuntoVista> adjuntos,
                               long cantidadReplicas, double promedioCalificacion, long cantidadCalificaciones) {

    public KudoDetalleVista {
        adjuntos = List.copyOf(adjuntos);
    }

    public record AdjuntoVista(UUID id, String nombreOriginal, String tipoContenido, long tamanoBytes) {
    }
}
