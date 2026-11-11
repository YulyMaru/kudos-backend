package co.edu.uco.kudos.aplicacion.puerto.salida;


import co.edu.uco.kudos.aplicacion.dto.ResumenCalificacion;
import co.edu.uco.kudos.dominio.modelo.Calificacion;
import co.edu.uco.kudos.dominio.modelo.TipoContenido;

import java.util.UUID;

public interface RepositorioCalificaciones {

    void guardarOReemplazar(Calificacion calificacion);

    ResumenCalificacion resumen(TipoContenido tipo, UUID contenidoId, UUID usuarioId);
}
