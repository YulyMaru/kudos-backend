package co.edu.uco.kudos.infraestructura.entrada.rest.dto;


import co.edu.uco.kudos.dominio.modelo.TipoContenido;

import java.util.UUID;

public record CalificacionSolicitud(TipoContenido tipoContenido, UUID contenidoId, Integer valor) {
}
