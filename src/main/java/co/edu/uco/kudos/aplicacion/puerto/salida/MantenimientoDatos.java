package co.edu.uco.kudos.aplicacion.puerto.salida;


import java.time.Instant;

public interface MantenimientoDatos {

    int eliminarTokensRefrescoVencidos(Instant antesDe);

    int eliminarNotificacionesFinalizadas(Instant antesDe);

    int eliminarCodigosVencidos(Instant antesDe);
}
