package co.edu.uco.kudos.aplicacion.dto;


import co.edu.uco.kudos.dominio.modelo.TipoContenido;

import java.util.UUID;

public record CalificarComando(UUID usuarioId, TipoContenido tipoContenido, UUID contenidoId, Integer valor) {
}
