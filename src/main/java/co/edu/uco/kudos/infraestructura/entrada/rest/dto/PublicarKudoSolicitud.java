package co.edu.uco.kudos.infraestructura.entrada.rest.dto;

import java.util.UUID;

public record PublicarKudoSolicitud(UUID destinatarioId, UUID categoriaId, String contenido) {
}
