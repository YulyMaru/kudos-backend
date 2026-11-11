package co.edu.uco.kudos.infraestructura.salida.persistencia.entidad;

import co.edu.uco.kudos.dominio.modelo.Canal;
import co.edu.uco.kudos.dominio.modelo.EstadoNotificacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "outbox_notificacion")
public class NotificacionOutboxEntidad extends EntidadPersistible {

    @Id
    private UUID id;

    @Column(name = "id_evento", nullable = false)
    private UUID idEvento;

    @Column(name = "id_correlacion", nullable = false)
    private UUID idCorrelacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false, length = 20)
    private Canal canal;

    @Column(name = "destinatario", length = 254)
    private String destinatario;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, String> payload = new HashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoNotificacion estado;

    @Column(name = "intentos", nullable = false)
    private int intentos;

    @Column(name = "proximo_intento")
    private Instant proximoIntento;

    @Column(name = "ultimo_error", columnDefinition = "text")
    private String ultimoError;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    protected NotificacionOutboxEntidad() {
    }

    public NotificacionOutboxEntidad(UUID id, UUID idEvento, UUID idCorrelacion, Canal canal, String destinatario,
                                     Map<String, String> payload, EstadoNotificacion estado, int intentos,
                                     Instant proximoIntento, String ultimoError, Instant creadoEn) {
        this.id = id;
        this.idEvento = idEvento;
        this.idCorrelacion = idCorrelacion;
        this.canal = canal;
        this.destinatario = destinatario;
        this.payload = new HashMap<>(payload);
        this.estado = estado;
        this.intentos = intentos;
        this.proximoIntento = proximoIntento;
        this.ultimoError = ultimoError;
        this.creadoEn = creadoEn;
    }

    public void reservarHasta(Instant limiteReserva) {
        this.proximoIntento = limiteReserva;
    }

    public void actualizarResultado(EstadoNotificacion nuevoEstado, int nuevosIntentos, Instant siguienteIntento,
                                    String error) {
        this.estado = nuevoEstado;
        this.intentos = nuevosIntentos;
        this.proximoIntento = siguienteIntento;
        this.ultimoError = error;
    }

    @Override
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

    public Map<String, String> getPayload() {
        return Map.copyOf(payload);
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
