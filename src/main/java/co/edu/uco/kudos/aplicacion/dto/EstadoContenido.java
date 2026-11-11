package co.edu.uco.kudos.aplicacion.dto;


import co.edu.uco.kudos.dominio.modelo.TipoContenido;

import java.util.UUID;

public record EstadoContenido(TipoContenido tipo, UUID id, UUID kudoId, UUID autorId, boolean vetado) {
}
