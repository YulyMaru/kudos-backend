package co.edu.uco.kudos.infraestructura.salida.observabilidad;

import co.edu.uco.kudos.dominio.modelo.EstadoNotificacion;
import co.edu.uco.kudos.infraestructura.salida.persistencia.repositorio.NotificacionOutboxRepositorioJpa;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MetricasOutbox {

    public static final String METRICA_OUTBOX = "kudos.outbox.notificaciones";

    public MetricasOutbox(NotificacionOutboxRepositorioJpa repositorio, MeterRegistry registro) {
        for (EstadoNotificacion estado : EstadoNotificacion.values()) {
            Gauge.builder(METRICA_OUTBOX, () -> repositorio.countByEstado(estado))
                    .tag("estado", estado.name())
                    .description("Notificaciones del outbox por estado")
                    .register(registro);
        }
    }
}
