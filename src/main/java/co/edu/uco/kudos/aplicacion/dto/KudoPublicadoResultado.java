package co.edu.uco.kudos.aplicacion.dto;

import java.time.Instant;
import java.util.UUID;

public record KudoPublicadoResultado(UUID idKudo, Instant fechaPublicacion) {
}
