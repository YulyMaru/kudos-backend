package co.edu.uco.kudos.dominio.modelo;

import co.edu.uco.kudos.dominio.excepcion.CampoObligatorioExcepcion;
import co.edu.uco.kudos.dominio.excepcion.ReglaNegocioExcepcion;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Replica(UUID id, UUID kudoId, UUID replicaPadreId, UUID autorId, String contenido,
                      Instant fechaPublicacion) {

    public Replica {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kudoId, "kudoId");
        Objects.requireNonNull(autorId, "autorId");
        Objects.requireNonNull(contenido, "contenido");
        Objects.requireNonNull(fechaPublicacion, "fechaPublicacion");
    }

    public static Replica publicar(UUID kudoId, UUID replicaPadreId, Usuario autor, String contenido, Instant ahora) {
        if (autor == null) {
            throw new CampoObligatorioExcepcion("autor");
        }
        if (!autor.estaActivo()) {
            throw new ReglaNegocioExcepcion("AUTOR_INACTIVO", "autor",
                    "El autor debe tener una cuenta activa para replicar");
        }
        if (contenido == null || contenido.isBlank()) {
            throw new CampoObligatorioExcepcion("contenido");
        }
        return new Replica(UUID.randomUUID(), kudoId, replicaPadreId, autor.getId(), contenido, ahora);
    }
}
