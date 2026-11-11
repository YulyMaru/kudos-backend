package co.edu.uco.kudos.aplicacion.dto;


import co.edu.uco.kudos.dominio.modelo.TipoContenido;

import java.util.UUID;

public record ResumenCalificacion(TipoContenido tipoContenido, UUID contenidoId, double promedio, long cantidad,
                                  Integer miCalificacion) {
}
