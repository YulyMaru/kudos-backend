package co.edu.uco.kudos.aplicacion.puerto.salida;

import co.edu.uco.kudos.dominio.modelo.Notificacion;

import java.util.List;

public interface RepositorioOutbox {

    void guardarTodas(List<Notificacion> notificaciones);
}
