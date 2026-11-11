package co.edu.uco.kudos.infraestructura.entrada.rest.dto;

import java.time.Instant;
import java.util.UUID;

public record KudoPublicadoRespuesta(UUID id, Instant fechaPublicacion) {
}
