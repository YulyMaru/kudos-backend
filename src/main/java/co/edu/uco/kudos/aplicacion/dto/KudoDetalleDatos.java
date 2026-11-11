package co.edu.uco.kudos.aplicacion.dto;


import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record KudoDetalleDatos(UUID id, PersonaVista autor, PersonaVista destinatario, CategoriaVista categoria,
                               String contenido, Instant fechaPublicacion, List<AdjuntoDatos> adjuntos,
                               long cantidadReplicas, double promedioCalificacion, long cantidadCalificaciones) {

    public KudoDetalleDatos {
        adjuntos = List.copyOf(adjuntos);
    }
}
