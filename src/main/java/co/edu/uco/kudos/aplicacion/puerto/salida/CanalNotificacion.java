package co.edu.uco.kudos.aplicacion.puerto.salida;

import co.edu.uco.kudos.aplicacion.dto.ResultadoEnvio;
import co.edu.uco.kudos.dominio.modelo.Canal;
import co.edu.uco.kudos.dominio.modelo.Notificacion;

public interface CanalNotificacion {

    Canal canal();

    ResultadoEnvio enviar(Notificacion notificacion);
}
