package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.dto.ResultadoLimpieza;
import co.edu.uco.kudos.aplicacion.puerto.entrada.LimpiarDatosPuerto;
import co.edu.uco.kudos.aplicacion.puerto.salida.MantenimientoDatos;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class LimpiarDatosCasoUso implements LimpiarDatosPuerto {

    private final MantenimientoDatos mantenimiento;
    private final Clock reloj;
    private final Duration retencionNotificaciones;
    private final Duration retencionCredenciales;

    public LimpiarDatosCasoUso(MantenimientoDatos mantenimiento, Clock reloj, Duration retencionNotificaciones,
                               Duration retencionCredenciales) {
        this.mantenimiento = Objects.requireNonNull(mantenimiento);
        this.reloj = Objects.requireNonNull(reloj);
        this.retencionNotificaciones = Objects.requireNonNull(retencionNotificaciones);
        this.retencionCredenciales = Objects.requireNonNull(retencionCredenciales);
    }

    @Override
    public ResultadoLimpieza limpiar() {
        Instant ahora = Instant.now(reloj);
        Instant limiteCredenciales = ahora.minus(retencionCredenciales);
        int tokens = mantenimiento.eliminarTokensRefrescoVencidos(limiteCredenciales);
        int notificaciones = mantenimiento.eliminarNotificacionesFinalizadas(ahora.minus(retencionNotificaciones));
        int codigos = mantenimiento.eliminarCodigosVencidos(limiteCredenciales);
        return new ResultadoLimpieza(tokens, notificaciones, codigos);
    }
}
