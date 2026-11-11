package co.edu.uco.kudos.infraestructura.salida.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "adjunto")
public class AdjuntoEntidad extends EntidadPersistible {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kudo_id", nullable = false)
    private KudoEntidad kudo;

    @Column(name = "nombre_original", nullable = false, length = 255)
    private String nombreOriginal;

    @Column(name = "tipo_contenido", nullable = false, length = 100)
    private String tipoContenido;

    @Column(name = "tamano_bytes", nullable = false)
    private long tamanoBytes;

    @Column(name = "hash_sha256", nullable = false, length = 64)
    private String hashSha256;

    @Column(name = "clave_objeto", nullable = false, length = 500)
    private String claveObjeto;

    protected AdjuntoEntidad() {
    }

    public AdjuntoEntidad(UUID id, String nombreOriginal, String tipoContenido, long tamanoBytes,
                          String hashSha256, String claveObjeto) {
        this.id = id;
        this.nombreOriginal = nombreOriginal;
        this.tipoContenido = tipoContenido;
        this.tamanoBytes = tamanoBytes;
        this.hashSha256 = hashSha256;
        this.claveObjeto = claveObjeto;
    }

    void asignarKudo(KudoEntidad kudo) {
        this.kudo = kudo;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public KudoEntidad getKudo() {
        return kudo;
    }

    public String getNombreOriginal() {
        return nombreOriginal;
    }

    public String getTipoContenido() {
        return tipoContenido;
    }

    public long getTamanoBytes() {
        return tamanoBytes;
    }

    public String getHashSha256() {
        return hashSha256;
    }

    public String getClaveObjeto() {
        return claveObjeto;
    }
}
