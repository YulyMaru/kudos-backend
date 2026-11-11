package co.edu.uco.kudos.infraestructura.salida.persistencia.entidad;

import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;
import co.edu.uco.kudos.dominio.modelo.Canal;
import co.edu.uco.kudos.dominio.modelo.ResultadoAuditoria;
import co.edu.uco.kudos.dominio.modelo.Rol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "registro_auditoria")
public class RegistroAuditoriaEntidad extends EntidadPersistible {

    @Id
    private UUID id;

    @Column(name = "fecha_hora", nullable = false)
    private Instant fechaHora;

    @Column(name = "id_correlacion")
    private UUID idCorrelacion;

    @Column(name = "actor_id")
    private UUID actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol_actor", length = 30)
    private Rol rolActor;

    @Enumerated(EnumType.STRING)
    @Column(name = "accion", nullable = false, length = 40)
    private AccionAuditoria accion;

    @Column(name = "recurso_tipo", length = 40)
    private String recursoTipo;

    @Column(name = "recurso_id")
    private UUID recursoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal", length = 20)
    private Canal canal;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado", nullable = false, length = 20)
    private ResultadoAuditoria resultado;

    @Column(name = "detalle", columnDefinition = "text")
    private String detalle;

    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    protected RegistroAuditoriaEntidad() {
    }

    public RegistroAuditoriaEntidad conContexto(String ip, String traza) {
        this.ipOrigen = ip == null || ip.length() > 45 ? null : ip;
        this.traceId = traza == null || traza.length() > 64 ? null : traza;
        return this;
    }

    public String getIpOrigen() {
        return ipOrigen;
    }

    public String getTraceId() {
        return traceId;
    }

    public RegistroAuditoriaEntidad(UUID id, Instant fechaHora, UUID idCorrelacion, UUID actorId, Rol rolActor,
                                    AccionAuditoria accion, String recursoTipo, UUID recursoId, Canal canal,
                                    ResultadoAuditoria resultado, String detalle) {
        this.id = id;
        this.fechaHora = fechaHora;
        this.idCorrelacion = idCorrelacion;
        this.actorId = actorId;
        this.rolActor = rolActor;
        this.accion = accion;
        this.recursoTipo = recursoTipo;
        this.recursoId = recursoId;
        this.canal = canal;
        this.resultado = resultado;
        this.detalle = detalle;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public Instant getFechaHora() {
        return fechaHora;
    }

    public UUID getIdCorrelacion() {
        return idCorrelacion;
    }

    public UUID getActorId() {
        return actorId;
    }

    public Rol getRolActor() {
        return rolActor;
    }

    public AccionAuditoria getAccion() {
        return accion;
    }

    public String getRecursoTipo() {
        return recursoTipo;
    }

    public UUID getRecursoId() {
        return recursoId;
    }

    public Canal getCanal() {
        return canal;
    }

    public ResultadoAuditoria getResultado() {
        return resultado;
    }

    public String getDetalle() {
        return detalle;
    }
}
