package co.edu.uco.kudos.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "kudos.notificaciones.whatsapp")
public record PropiedadesWhatsapp(String urlApi, String token, String telefonoId, String plantilla,
                                  String plantillaCodigo, String idioma, Duration tiempoConexion,
                                  Duration tiempoLectura) {

    public PropiedadesWhatsapp {
        urlApi = valorODefecto(urlApi, "https://graph.facebook.com/v23.0");
        token = token == null ? "" : token;
        telefonoId = telefonoId == null ? "" : telefonoId;
        plantilla = valorODefecto(plantilla, "kudo_recibido");
        plantillaCodigo = valorODefecto(plantillaCodigo, "codigo_verificacion");
        idioma = valorODefecto(idioma, "es");
        tiempoConexion = tiempoConexion == null ? Duration.ofSeconds(5) : tiempoConexion;
        tiempoLectura = tiempoLectura == null ? Duration.ofSeconds(10) : tiempoLectura;
    }

    private static String valorODefecto(String valor, String defecto) {
        return valor == null || valor.isBlank() ? defecto : valor;
    }
}
