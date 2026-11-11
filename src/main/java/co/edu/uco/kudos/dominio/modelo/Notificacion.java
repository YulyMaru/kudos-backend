package co.edu.uco.kudos.dominio.modelo;

import co.edu.uco.kudos.dominio.regla.PoliticaReintentos;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class Notificacion {

    private final UUID id;
    private final UUID idEvento;
    private final UUID idCorrelacion;
    private final Canal canal;
    private final String destinatario;
    private final Map<String, String> datos;
    private final EstadoNotificacion estado;
    private final int intentos;
    private final Instant proximoIntento;
    private final String ultimoError;
    private final Instant creadoEn;

    private Notificacion(UUID id, UUID idEvento, UUID idCorrelacion, Canal canal, String destinatario,
                         Map<String, String> datos, EstadoNotificacion estado, int intentos,
                         Instant proximoIntento, String ultimoError, Instant creadoEn) {
        this.id = Objects.requireNonNull(id, "id");
        this.idEvento = Objects.requireNonNull(idEvento, "idEvento");
        this.idCorrelacion = Objects.requireNonNull(idCorrelacion, "idCorrelacion");
        this.canal = Objects.requireNonNull(canal, "canal");
        this.destinatario = destinatario;
        this.datos = datos == null ? Map.of() : Map.copyOf(datos);
        this.estado = Objects.requireNonNull(estado, "estado");
        this.intentos = intentos;
        this.proximoIntento = proximoIntento;
        this.ultimoError = ultimoError;
        this.creadoEn = Objects.requireNonNull(creadoEn, "creadoEn");
    }

    public static Notificacion pendiente(UUID idEvento, UUID idCorrelacion, Canal canal, String destinatario,
                                         Map<String, String> datos, Instant ahora) {
        Objects.requireNonNull(destinatario, "destinatario");
        return new Notificacion(UUID.randomUUID(), idEvento, idCorrelacion, canal, destinatario, datos,
                EstadoNotificacion.PENDIENTE, 0, ahora, null, ahora);
    }

    public static Notificacion noAplica(UUID idEvento, UUID idCorrelacion, Canal canal,
                                        Map<String, String> datos, String motivo, Instant ahora) {
        return new Notificacion(UUID.randomUUID(), idEvento, idCorrelacion, canal, null, datos,
                EstadoNotificacion.NO_APLICA, 0, null, motivo, ahora);
    }

    public static Notificacion reconstruir(UUID id, UUID idEvento, UUID idCorrelacion, Canal canal,
                                           String destinatario, Map<String, String> datos,
                                           EstadoNotificacion estado, int intentos, Instant proximoIntento,
                                           String ultimoError, Instant creadoEn) {
        return new Notificacion(id, idEvento, idCorrelacion, canal, destinatario, datos, estado, intentos,
                proximoIntento, ultimoError, creadoEn);
    }

    public boolean puedeProcesarse() {
        return estado == EstadoNotificacion.PENDIENTE || estado == EstadoNotificacion.REINTENTANDO;
    }

    public Notificacion marcarEnviada() {
        validarProcesable();
        return conResultado(EstadoNotificacion.ENVIADO, intentos + 1, null, null);
    }

    public Notificacion marcarNoAplica(String motivo) {
        validarProcesable();
        return conResultado(EstadoNotificacion.NO_APLICA, intentos + 1, null, motivo);
    }

    public Notificacion registrarFallo(String error, Instant ahora, PoliticaReintentos politica, double aleatorio) {
        validarProcesable();
        int intentosRealizados = intentos + 1;
        if (politica.agotado(intentosRealizados)) {
            return conResultado(EstadoNotificacion.FALLIDO, intentosRealizados, null, error);
        }
        Instant siguiente = ahora.plus(politica.calcularRetraso(intentosRealizados, aleatorio));
        return conResultado(EstadoNotificacion.REINTENTANDO, intentosRealizados, siguiente, error);
    }

    private void validarProcesable() {
        if (!puedeProcesarse()) {
            throw new IllegalStateException("La notificación " + id + " ya está finalizada en estado " + estado);
        }
    }

    private Notificacion conResultado(EstadoNotificacion nuevoEstado, int nuevosIntentos, Instant siguienteIntento,
                                      String error) {
        return new Notificacion(id, idEvento, idCorrelacion, canal, destinatario, datos, nuevoEstado,
                nuevosIntentos, siguienteIntento, error, creadoEn);
    }

    public UUID getId() {
        return id;
    }

    public UUID getIdEvento() {
        return idEvento;
    }

    public UUID getIdCorrelacion() {
        return idCorrelacion;
    }

    public Canal getCanal() {
        return canal;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public Map<String, String> getDatos() {
        return datos;
    }

    public EstadoNotificacion getEstado() {
        return estado;
    }

    public int getIntentos() {
        return intentos;
    }

    public Instant getProximoIntento() {
        return proximoIntento;
    }

    public String getUltimoError() {
        return ultimoError;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
