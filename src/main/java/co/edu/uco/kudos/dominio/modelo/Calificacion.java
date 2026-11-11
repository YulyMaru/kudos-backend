package co.edu.uco.kudos.dominio.modelo;

import co.edu.uco.kudos.dominio.excepcion.CampoObligatorioExcepcion;
import co.edu.uco.kudos.dominio.excepcion.ReglaNegocioExcepcion;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Calificacion(UUID usuarioId, TipoContenido tipoContenido, UUID contenidoId, int valor,
                           Instant fecha) {

    public static final int VALOR_MINIMO = 1;
    public static final int VALOR_MAXIMO = 5;

    public Calificacion {
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(fecha, "fecha");
        if (tipoContenido == null) {
            throw new CampoObligatorioExcepcion("tipoContenido");
        }
        if (contenidoId == null) {
            throw new CampoObligatorioExcepcion("contenidoId");
        }
        if (valor < VALOR_MINIMO || valor > VALOR_MAXIMO) {
            throw new ReglaNegocioExcepcion("CALIFICACION_FUERA_DE_RANGO", "valor",
                    "La calificación debe estar entre " + VALOR_MINIMO + " y " + VALOR_MAXIMO);
        }
    }
}
