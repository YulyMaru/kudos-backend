package co.edu.uco.kudos.aplicacion.dto;


import java.time.Instant;
import java.util.UUID;

public record ReplicaPublicadaResultado(UUID id, UUID kudoId, UUID replicaPadreId, Instant fechaPublicacion) {
}
