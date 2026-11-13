package co.edu.uco.kudos.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "kudos.notificaciones.smstraccar")
public record PropiedadesSmsTraccar(String url, String token, Duration tiempoConexion, Duration tiempoLectura) {

    public PropiedadesSmsTraccar {
        url = url == null || url.isBlank() ? "https://www.traccar.org/sms/" : url;
        token = token == null ? "" : token;
        tiempoConexion = tiempoConexion == null ? Duration.ofSeconds(5) : tiempoConexion;
        tiempoLectura = tiempoLectura == null ? Duration.ofSeconds(10) : tiempoLectura;
    }

    @Override
    public String toString() {
        return "PropiedadesSmsTraccar[url=" + url + ", token=****, tiempoConexion=" + tiempoConexion
                + ", tiempoLectura=" + tiempoLectura + "]";
    }
}
