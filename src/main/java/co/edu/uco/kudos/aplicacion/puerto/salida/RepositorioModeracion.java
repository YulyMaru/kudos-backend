package co.edu.uco.kudos.aplicacion.puerto.salida;


import co.edu.uco.kudos.dominio.modelo.Reporte;
import co.edu.uco.kudos.dominio.modelo.TipoContenido;
import co.edu.uco.kudos.dominio.modelo.Veto;

import java.time.Instant;
import java.util.UUID;

public interface RepositorioModeracion {

    boolean existeReporte(TipoContenido tipo, UUID contenidoId, UUID reportanteId);

    void guardarReporte(Reporte reporte);

    void guardarVeto(Veto veto);

    void marcarReportesAtendidos(TipoContenido tipo, UUID contenidoId, Instant fecha);
}
