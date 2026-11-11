package co.edu.uco.kudos.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "kudos.outbox")
public record PropiedadesOutbox(Integer tamanoLote, Integer maximoIntentos, Duration retrasoBase,
                                Duration retrasoMaximo, Duration duracionReserva) {

    public PropiedadesOutbox {
        tamanoLote = tamanoLote == null ? 20 : tamanoLote;
        maximoIntentos = maximoIntentos == null ? 5 : maximoIntentos;
        retrasoBase = retrasoBase == null ? Duration.ofSeconds(10) : retrasoBase;
        retrasoMaximo = retrasoMaximo == null ? Duration.ofMinutes(10) : retrasoMaximo;
        duracionReserva = duracionReserva == null ? Duration.ofMinutes(2) : duracionReserva;
    }
}
