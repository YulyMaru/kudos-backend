package co.edu.uco.kudos.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "kudos.limpieza")
public record PropiedadesLimpieza(Duration retencionNotificaciones, Duration retencionCredenciales) {

    public PropiedadesLimpieza {
        retencionNotificaciones = retencionNotificaciones == null ? Duration.ofDays(30) : retencionNotificaciones;
        retencionCredenciales = retencionCredenciales == null ? Duration.ofDays(1) : retencionCredenciales;
    }
}
