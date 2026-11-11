package co.edu.uco.kudos.infraestructura.salida.persistencia.adaptador;

import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioNotificacionesPendientes;
import co.edu.uco.kudos.dominio.modelo.Notificacion;
import co.edu.uco.kudos.infraestructura.salida.persistencia.entidad.NotificacionOutboxEntidad;
import co.edu.uco.kudos.infraestructura.salida.persistencia.mapeador.NotificacionMapeador;
import co.edu.uco.kudos.infraestructura.salida.persistencia.repositorio.NotificacionOutboxRepositorioJpa;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class RepositorioNotificacionesPendientesAdaptador implements RepositorioNotificacionesPendientes {

    private final NotificacionOutboxRepositorioJpa repositorio;

    public RepositorioNotificacionesPendientesAdaptador(NotificacionOutboxRepositorioJpa repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    @Transactional
    public List<Notificacion> reservarPendientes(Instant ahora, int limite, Duration duracionReserva) {
        List<NotificacionOutboxEntidad> bloqueadas = repositorio.bloquearPendientes(ahora, limite);
        List<Notificacion> reservadas = bloqueadas.stream().map(NotificacionMapeador::aDominio).toList();
        Instant limiteReserva = ahora.plus(duracionReserva);
        bloqueadas.forEach(entidad -> entidad.reservarHasta(limiteReserva));
        return reservadas;
    }

    @Override
    @Transactional
    public void actualizar(Notificacion notificacion) {
        NotificacionOutboxEntidad entidad = repositorio.findById(notificacion.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "No existe la notificación " + notificacion.getId() + " en el outbox"));
        entidad.actualizarResultado(notificacion.getEstado(), notificacion.getIntentos(),
                notificacion.getProximoIntento(), notificacion.getUltimoError());
    }
}
