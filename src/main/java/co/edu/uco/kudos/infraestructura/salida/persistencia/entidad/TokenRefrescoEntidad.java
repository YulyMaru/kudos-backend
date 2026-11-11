package co.edu.uco.kudos.infraestructura.salida.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "token_refresco")
public class TokenRefrescoEntidad extends EntidadPersistible {

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "hash_token", nullable = false, length = 64)
    private String hashToken;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Column(name = "revocado_en")
    private Instant revocadoEn;

    @Column(name = "reemplazado_por")
    private UUID reemplazadoPor;

    protected TokenRefrescoEntidad() {
    }

    public TokenRefrescoEntidad(UUID id, UUID usuarioId, String hashToken, Instant creadoEn, Instant expiraEn,
                                Instant revocadoEn, UUID reemplazadoPor) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.hashToken = hashToken;
        this.creadoEn = creadoEn;
        this.expiraEn = expiraEn;
        this.revocadoEn = revocadoEn;
        this.reemplazadoPor = reemplazadoPor;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public String getHashToken() {
        return hashToken;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getExpiraEn() {
        return expiraEn;
    }

    public Instant getRevocadoEn() {
        return revocadoEn;
    }

    public UUID getReemplazadoPor() {
        return reemplazadoPor;
    }
}
