package co.edu.uco.kudos.aplicacion.puerto.salida;

import co.edu.uco.kudos.dominio.modelo.Notificacion;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public interface RepositorioNotificacionesPendientes {

    List<Notificacion> reservarPendientes(Instant ahora, int limite, Duration duracionReserva);

    void actualizar(Notificacion notificacion);
}
