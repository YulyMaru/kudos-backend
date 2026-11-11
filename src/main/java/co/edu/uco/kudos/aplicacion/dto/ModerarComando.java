package co.edu.uco.kudos.aplicacion.dto;


import co.edu.uco.kudos.dominio.modelo.TipoContenido;

import java.util.UUID;

public record ModerarComando(UUID actorId, TipoContenido tipoContenido, UUID contenidoId, String motivo) {
}
