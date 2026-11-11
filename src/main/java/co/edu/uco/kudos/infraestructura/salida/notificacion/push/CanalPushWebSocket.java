package co.edu.uco.kudos.infraestructura.salida.notificacion.push;

import co.edu.uco.kudos.aplicacion.dto.ResultadoEnvio;
import co.edu.uco.kudos.aplicacion.puerto.salida.CanalNotificacion;
import co.edu.uco.kudos.dominio.modelo.Canal;
import co.edu.uco.kudos.dominio.modelo.Notificacion;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class CanalPushWebSocket implements CanalNotificacion {

    public static final String DESTINO_USUARIO = "/queue/notificaciones";

    private final SimpMessagingTemplate mensajeria;
    private final SimpUserRegistry usuariosConectados;

    public CanalPushWebSocket(SimpMessagingTemplate mensajeria, SimpUserRegistry usuariosConectados) {
        this.mensajeria = Objects.requireNonNull(mensajeria);
        this.usuariosConectados = Objects.requireNonNull(usuariosConectados);
    }

    @Override
    public Canal canal() {
        return Canal.PUSH;
    }

    @Override
    public ResultadoEnvio enviar(Notificacion notificacion) {
        String usuario = notificacion.getDestinatario();
        if (usuario == null || usuariosConectados.getUser(usuario) == null) {
            return ResultadoEnvio.noAplica("El destinatario no está conectado");
        }
        Map<String, String> datos = notificacion.getDatos();
        Map<String, String> mensaje = new LinkedHashMap<>();
        mensaje.put("tipo", "KUDO_RECIBIDO");
        mensaje.put("idNotificacion", notificacion.getId().toString());
        mensaje.put("idKudo", datos.getOrDefault("idKudo", ""));
        mensaje.put("nombreAutor", datos.getOrDefault("nombreAutor", ""));
        mensaje.put("nombreCategoria", datos.getOrDefault("nombreCategoria", ""));
        mensajeria.convertAndSendToUser(usuario, DESTINO_USUARIO, mensaje);
        return ResultadoEnvio.exitoso();
    }
}
