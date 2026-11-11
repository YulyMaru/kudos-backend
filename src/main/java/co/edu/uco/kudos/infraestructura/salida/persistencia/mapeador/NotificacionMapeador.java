package co.edu.uco.kudos.infraestructura.salida.persistencia.mapeador;

import co.edu.uco.kudos.dominio.modelo.Notificacion;
import co.edu.uco.kudos.infraestructura.salida.persistencia.entidad.NotificacionOutboxEntidad;

public final class NotificacionMapeador {

    private NotificacionMapeador() {
    }

    public static NotificacionOutboxEntidad aEntidad(Notificacion notificacion) {
        return new NotificacionOutboxEntidad(notificacion.getId(), notificacion.getIdEvento(),
                notificacion.getIdCorrelacion(), notificacion.getCanal(), notificacion.getDestinatario(),
                notificacion.getDatos(), notificacion.getEstado(), notificacion.getIntentos(),
                notificacion.getProximoIntento(), notificacion.getUltimoError(), notificacion.getCreadoEn());
    }

    public static Notificacion aDominio(NotificacionOutboxEntidad entidad) {
        return Notificacion.reconstruir(entidad.getId(), entidad.getIdEvento(), entidad.getIdCorrelacion(),
                entidad.getCanal(), entidad.getDestinatario(), entidad.getPayload(), entidad.getEstado(),
                entidad.getIntentos(), entidad.getProximoIntento(), entidad.getUltimoError(), entidad.getCreadoEn());
    }
}
