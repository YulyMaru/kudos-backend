package co.edu.uco.kudos.infraestructura.entrada.programado;

import co.edu.uco.kudos.aplicacion.dto.ResultadoLimpieza;
import co.edu.uco.kudos.aplicacion.puerto.entrada.LimpiarDatosPuerto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "kudos.limpieza.habilitada", havingValue = "true", matchIfMissing = true)
public class TareaLimpiezaProgramada {

    private static final Logger LOG = LoggerFactory.getLogger(TareaLimpiezaProgramada.class);

    private final LimpiarDatosPuerto limpiarDatos;

    public TareaLimpiezaProgramada(LimpiarDatosPuerto limpiarDatos) {
        this.limpiarDatos = limpiarDatos;
    }

    @Scheduled(cron = "${kudos.limpieza.cron:0 30 3 * * *}", zone = "UTC")
    public void limpiar() {
        try {
            ResultadoLimpieza resultado = limpiarDatos.limpiar();
            LOG.info("Limpieza programada: tokens={}, notificaciones={}, codigos={}", resultado.tokensEliminados(),
                    resultado.notificacionesEliminadas(), resultado.codigosEliminados());
        } catch (RuntimeException error) {
            LOG.error("Falló la limpieza programada de datos", error);
        }
    }
}
