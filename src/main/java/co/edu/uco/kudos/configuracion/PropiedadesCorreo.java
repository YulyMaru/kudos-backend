package co.edu.uco.kudos.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kudos.notificaciones.correo")
public record PropiedadesCorreo(String remitente, String nombreRemitente, String urlFrontend) {

    public PropiedadesCorreo {
        remitente = valorODefecto(remitente, "no-responder@uco-kudos.app");
        nombreRemitente = valorODefecto(nombreRemitente, "UCO Kudos");
        urlFrontend = valorODefecto(urlFrontend, "http://localhost:4200");
    }

    private static String valorODefecto(String valor, String defecto) {
        return valor == null || valor.isBlank() ? defecto : valor;
    }
}
