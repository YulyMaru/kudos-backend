package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.dominio.modelo.Canal;
import co.edu.uco.kudos.dominio.modelo.Notificacion;
import co.edu.uco.kudos.dominio.modelo.PropositoCodigo;
import co.edu.uco.kudos.dominio.modelo.Usuario;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

final class NotificacionesCuenta {

    static final String PLANTILLA = "plantilla";
    static final String RECURSO_TIPO = "recursoTipo";
    static final String RECURSO_USUARIO = "USUARIO";

    private NotificacionesCuenta() {
    }

    static Notificacion codigo(Usuario usuario, PropositoCodigo proposito, String codigo, Instant ahora) {
        boolean porCorreo = proposito == PropositoCodigo.VERIFICAR_CORREO;
        Map<String, String> datos = Map.of(
                PLANTILLA, "CODIGO_VERIFICACION",
                RECURSO_TIPO, RECURSO_USUARIO,
                "codigo", codigo,
                "nombreDestinatario", usuario.getNombreCompleto());
        return Notificacion.pendiente(UUID.randomUUID(), usuario.getId(), porCorreo ? Canal.EMAIL : Canal.WHATSAPP,
                porCorreo ? usuario.getCorreo() : usuario.getCelular(), datos, ahora);
    }

    static Notificacion restablecimiento(Usuario usuario, String token, Instant ahora) {
        return correo(usuario, ahora, Map.of(
                PLANTILLA, "RESTABLECER_CONTRASENA",
                RECURSO_TIPO, RECURSO_USUARIO,
                "token", token,
                "nombreDestinatario", usuario.getNombreCompleto()));
    }

    static Notificacion solicitudRechazada(Usuario usuario, String motivo, Instant ahora) {
        return correo(usuario, ahora, Map.of(
                PLANTILLA, "SOLICITUD_RECHAZADA",
                RECURSO_TIPO, RECURSO_USUARIO,
                "motivo", motivo,
                "nombreDestinatario", usuario.getNombreCompleto()));
    }

    static Notificacion cuentaActivada(Usuario usuario, Instant ahora) {
        return correo(usuario, ahora, Map.of(
                PLANTILLA, "CUENTA_ACTIVADA",
                RECURSO_TIPO, RECURSO_USUARIO,
                "nombreDestinatario", usuario.getNombreCompleto()));
    }

    private static Notificacion correo(Usuario usuario, Instant ahora, Map<String, String> datos) {
        return Notificacion.pendiente(UUID.randomUUID(), usuario.getId(), Canal.EMAIL, usuario.getCorreo(), datos,
                ahora);
    }
}
