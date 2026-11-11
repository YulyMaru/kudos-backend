package co.edu.uco.kudos.infraestructura.entrada.programado;

import co.edu.uco.kudos.aplicacion.puerto.entrada.ProcesarNotificacionesPuerto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "kudos.outbox.habilitado", havingValue = "true", matchIfMissing = true)
public class ProcesadorOutboxProgramado {

    private static final Logger LOG = LoggerFactory.getLogger(ProcesadorOutboxProgramado.class);

    private final ProcesarNotificacionesPuerto procesarNotificaciones;

    public ProcesadorOutboxProgramado(ProcesarNotificacionesPuerto procesarNotificaciones) {
        this.procesarNotificaciones = procesarNotificaciones;
    }

    @Scheduled(fixedDelayString = "${kudos.outbox.intervalo:PT5S}",
            initialDelayString = "${kudos.outbox.retraso-inicial:PT10S}")
    public void procesar() {
        try {
            int procesadas = procesarNotificaciones.procesarPendientes();
            if (procesadas > 0) {
                LOG.info("Notificaciones procesadas del outbox: {}", procesadas);
            }
        } catch (RuntimeException error) {
            LOG.error("Falló la ejecución del procesador del outbox", error);
        }
    }
}
