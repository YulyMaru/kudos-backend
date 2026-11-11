package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.dto.ResultadoEnvio;
import co.edu.uco.kudos.aplicacion.puerto.entrada.ProcesarNotificacionesPuerto;
import co.edu.uco.kudos.aplicacion.puerto.salida.CanalNotificacion;
import co.edu.uco.kudos.aplicacion.puerto.salida.GestorTransacciones;
import co.edu.uco.kudos.aplicacion.puerto.salida.RegistroAuditoria;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioNotificacionesPendientes;
import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;
import co.edu.uco.kudos.dominio.modelo.Canal;
import co.edu.uco.kudos.dominio.modelo.EntradaAuditoria;
import co.edu.uco.kudos.dominio.modelo.Notificacion;
import co.edu.uco.kudos.dominio.modelo.ResultadoAuditoria;
import co.edu.uco.kudos.dominio.regla.PoliticaReintentos;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.DoubleSupplier;

public final class ProcesarNotificacionesCasoUso implements ProcesarNotificacionesPuerto {

    private static final System.Logger LOG = System.getLogger(ProcesarNotificacionesCasoUso.class.getName());
    private static final String RECURSO_KUDO = "KUDO";
    private static final int LONGITUD_MAXIMA_ERROR = 500;

    private final RepositorioNotificacionesPendientes pendientes;
    private final Map<Canal, CanalNotificacion> canales;
    private final RegistroAuditoria auditoria;
    private final GestorTransacciones transacciones;
    private final Clock reloj;
    private final PoliticaReintentos politica;
    private final int tamanoLote;
    private final Duration duracionReserva;
    private final DoubleSupplier aleatorio;

    public ProcesarNotificacionesCasoUso(RepositorioNotificacionesPendientes pendientes,
                                         List<CanalNotificacion> canales, RegistroAuditoria auditoria,
                                         GestorTransacciones transacciones, Clock reloj,
                                         PoliticaReintentos politica, int tamanoLote, Duration duracionReserva,
                                         DoubleSupplier aleatorio) {
        this.pendientes = Objects.requireNonNull(pendientes);
        this.canales = indexarPorCanal(Objects.requireNonNull(canales));
        this.auditoria = Objects.requireNonNull(auditoria);
        this.transacciones = Objects.requireNonNull(transacciones);
        this.reloj = Objects.requireNonNull(reloj);
        this.politica = Objects.requireNonNull(politica);
        this.duracionReserva = Objects.requireNonNull(duracionReserva);
        this.aleatorio = Objects.requireNonNull(aleatorio);
        if (tamanoLote < 1) {
            throw new IllegalArgumentException("tamanoLote debe ser al menos 1");
        }
        this.tamanoLote = tamanoLote;
    }

    @Override
    public int procesarPendientes() {
        List<Notificacion> lote = pendientes.reservarPendientes(Instant.now(reloj), tamanoLote, duracionReserva);
        int procesadas = 0;
        for (Notificacion notificacion : lote) {
            if (procesar(notificacion)) {
                procesadas++;
            }
        }
        return procesadas;
    }

    private boolean procesar(Notificacion notificacion) {
        if (!notificacion.puedeProcesarse()) {
            return false;
        }
        Resultado resultado = enviar(notificacion);
        try {
            transacciones.enTransaccion(() -> {
                pendientes.actualizar(resultado.notificacion());
                auditoria.registrar(resultado.entrada());
            });
            return true;
        } catch (RuntimeException error) {
            LOG.log(System.Logger.Level.ERROR,
                    "No fue posible registrar el resultado de la notificación " + notificacion.getId(), error);
            return false;
        }
    }

    private Resultado enviar(Notificacion notificacion) {
        try {
            CanalNotificacion canal = canales.get(notificacion.getCanal());
            if (canal == null) {
                throw new IllegalStateException("No hay adaptador configurado para el canal " + notificacion.getCanal());
            }
            ResultadoEnvio envio = canal.enviar(notificacion);
            Instant ahora = Instant.now(reloj);
            if (envio.entregado()) {
                Notificacion enviada = notificacion.marcarEnviada();
                return new Resultado(enviada, auditar(enviada, ahora, AccionAuditoria.NOTIFICACION_ENVIADA,
                        ResultadoAuditoria.EXITOSO, "intento=" + enviada.getIntentos()));
            }
            Notificacion noAplica = notificacion.marcarNoAplica(envio.motivo());
            return new Resultado(noAplica, auditar(noAplica, ahora, AccionAuditoria.NOTIFICACION_NO_APLICA,
                    ResultadoAuditoria.NO_APLICA, envio.motivo()));
        } catch (RuntimeException error) {
            Instant ahora = Instant.now(reloj);
            String descripcion = describir(error);
            Notificacion fallida = notificacion.registrarFallo(descripcion, ahora, politica, aleatorio.getAsDouble());
            String detalle = "intento=" + fallida.getIntentos() + ", estado=" + fallida.getEstado()
                    + ", error=" + descripcion;
            return new Resultado(fallida, auditar(fallida, ahora, AccionAuditoria.NOTIFICACION_FALLIDA,
                    ResultadoAuditoria.FALLIDO, detalle));
        }
    }

    private static EntradaAuditoria auditar(Notificacion notificacion, Instant ahora, AccionAuditoria accion,
                                            ResultadoAuditoria resultado, String detalle) {
        return EntradaAuditoria.de(ahora, notificacion.getIdCorrelacion(), null, accion,
                notificacion.getDatos().getOrDefault("recursoTipo", RECURSO_KUDO), notificacion.getIdCorrelacion(),
                notificacion.getCanal(), resultado, detalle);
    }

    private static String describir(RuntimeException error) {
        String mensaje = error.getClass().getSimpleName() + ": " + Objects.toString(error.getMessage(), "sin detalle");
        return mensaje.length() > LONGITUD_MAXIMA_ERROR ? mensaje.substring(0, LONGITUD_MAXIMA_ERROR) : mensaje;
    }

    private static Map<Canal, CanalNotificacion> indexarPorCanal(List<CanalNotificacion> canales) {
        Map<Canal, CanalNotificacion> indice = new EnumMap<>(Canal.class);
        for (CanalNotificacion canal : canales) {
            if (indice.putIfAbsent(canal.canal(), canal) != null) {
                throw new IllegalArgumentException("Hay más de un adaptador para el canal " + canal.canal());
            }
        }
        return Map.copyOf(indice);
    }

    private record Resultado(Notificacion notificacion, EntradaAuditoria entrada) {
    }
}
