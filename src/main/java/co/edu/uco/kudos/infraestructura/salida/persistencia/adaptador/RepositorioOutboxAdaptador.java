package co.edu.uco.kudos.infraestructura.salida.persistencia.adaptador;

import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioOutbox;
import co.edu.uco.kudos.dominio.modelo.Notificacion;
import co.edu.uco.kudos.infraestructura.salida.persistencia.mapeador.NotificacionMapeador;
import co.edu.uco.kudos.infraestructura.salida.persistencia.repositorio.NotificacionOutboxRepositorioJpa;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RepositorioOutboxAdaptador implements RepositorioOutbox {

    private final NotificacionOutboxRepositorioJpa repositorio;

    public RepositorioOutboxAdaptador(NotificacionOutboxRepositorioJpa repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void guardarTodas(List<Notificacion> notificaciones) {
        repositorio.saveAll(notificaciones.stream().map(NotificacionMapeador::aEntidad).toList());
    }
}
