package co.edu.uco.kudos.dominio.modelo;

import co.edu.uco.kudos.dominio.evento.EventoDominio;
import co.edu.uco.kudos.dominio.evento.KudoPublicado;
import co.edu.uco.kudos.dominio.regla.ReglaKudo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Kudo {

    private final UUID id;
    private final UUID autorId;
    private final UUID destinatarioId;
    private final UUID categoriaId;
    private final String contenido;
    private final Instant fechaPublicacion;
    private final List<Adjunto> adjuntos;
    private final List<EventoDominio> eventos = new ArrayList<>();

    private Kudo(UUID id, UUID autorId, UUID destinatarioId, UUID categoriaId,
                 String contenido, Instant fechaPublicacion, List<Adjunto> adjuntos) {
        this.id = Objects.requireNonNull(id, "id");
        this.autorId = Objects.requireNonNull(autorId, "autorId");
        this.destinatarioId = Objects.requireNonNull(destinatarioId, "destinatarioId");
        this.categoriaId = Objects.requireNonNull(categoriaId, "categoriaId");
        this.contenido = Objects.requireNonNull(contenido, "contenido");
        this.fechaPublicacion = Objects.requireNonNull(fechaPublicacion, "fechaPublicacion");
        this.adjuntos = adjuntos == null ? List.of() : List.copyOf(adjuntos);
    }

    public static Kudo publicar(UUID id, Usuario autor, Usuario destinatario, Categoria categoria,
                                String contenido, List<Adjunto> adjuntos, Instant fechaPublicacion) {
        ReglaKudo.validarPublicacion(autor, destinatario, categoria, contenido);
        Kudo kudo = new Kudo(id, autor.getId(), destinatario.getId(), categoria.getId(),
                contenido, fechaPublicacion, adjuntos);
        kudo.eventos.add(new KudoPublicado(UUID.randomUUID(), id, autor.getId(),
                destinatario.getId(), categoria.getId(), fechaPublicacion));
        return kudo;
    }

    public static Kudo reconstruir(UUID id, UUID autorId, UUID destinatarioId, UUID categoriaId,
                                   String contenido, Instant fechaPublicacion, List<Adjunto> adjuntos) {
        return new Kudo(id, autorId, destinatarioId, categoriaId, contenido, fechaPublicacion, adjuntos);
    }

    public List<EventoDominio> extraerEventos() {
        List<EventoDominio> pendientes = List.copyOf(eventos);
        eventos.clear();
        return pendientes;
    }

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

    public List<Adjunto> getAdjuntos() {
        return adjuntos;
    }
}
