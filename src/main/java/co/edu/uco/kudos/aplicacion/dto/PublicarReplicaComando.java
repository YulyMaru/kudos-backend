package co.edu.uco.kudos.aplicacion.dto;


import java.util.UUID;

public record PublicarReplicaComando(UUID autorId, UUID kudoId, UUID replicaPadreId, String contenido) {
}
