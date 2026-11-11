package co.edu.uco.kudos.infraestructura.salida.persistencia.entidad;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "kudo")
public class KudoEntidad extends EntidadPersistible {

    @Id
    private UUID id;

    @Column(name = "autor_id", nullable = false)
    private UUID autorId;

    @Column(name = "destinatario_id", nullable = false)
    private UUID destinatarioId;

    @Column(name = "categoria_id", nullable = false)
    private UUID categoriaId;

    @Column(name = "contenido", nullable = false, columnDefinition = "text")
    private String contenido;

    @Column(name = "fecha_publicacion", nullable = false)
    private Instant fechaPublicacion;

    @OneToMany(mappedBy = "kudo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AdjuntoEntidad> adjuntos = new ArrayList<>();

    protected KudoEntidad() {
    }

    public KudoEntidad(UUID id, UUID autorId, UUID destinatarioId, UUID categoriaId,
                       String contenido, Instant fechaPublicacion) {
        this.id = id;
        this.autorId = autorId;
        this.destinatarioId = destinatarioId;
        this.categoriaId = categoriaId;
        this.contenido = contenido;
        this.fechaPublicacion = fechaPublicacion;
    }

    public void agregarAdjunto(AdjuntoEntidad adjunto) {
        adjunto.asignarKudo(this);
        adjuntos.add(adjunto);
    }

    @Override
    public UUID getId() {
        return id;
    }

    public UUID getAutorId() {
        return autorId;
    }

    public UUID getDestinatarioId() {
        return destinatarioId;
    }

    public UUID getCategoriaId() {
        return categoriaId;
    }

    public String getContenido() {
        return contenido;
    }

    public Instant getFechaPublicacion() {
        return fechaPublicacion;
    }

    public List<AdjuntoEntidad> getAdjuntos() {
        return Collections.unmodifiableList(adjuntos);
    }
}
