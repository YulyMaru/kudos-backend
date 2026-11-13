package co.edu.uco.kudos.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kudos.notificaciones.smstraccar")
public record PropiedadesSmsTraccar(String url, String token) {

    public PropiedadesSmsTraccar {
        url = url == null || url.isBlank() ? "https://www.traccar.org/sms/" : url;
        token = token == null ? "" : token;
    }

    @Override
    public String toString() {
        return "PropiedadesSmsTraccar[url=" + url + ", token=****]";
    }
}
