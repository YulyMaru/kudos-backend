package co.edu.uco.kudos.infraestructura.entrada.rest.dto;


import java.util.UUID;

public record ReplicaSolicitud(String contenido, UUID replicaPadreId) {
}
