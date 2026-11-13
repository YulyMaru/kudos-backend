package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.dto.ArchivoAdjuntoComando;
import co.edu.uco.kudos.aplicacion.dto.KudoPublicadoResultado;
import co.edu.uco.kudos.aplicacion.dto.PublicarKudoComando;
import co.edu.uco.kudos.aplicacion.puerto.entrada.PublicarKudoPuerto;
import co.edu.uco.kudos.aplicacion.puerto.salida.AlmacenamientoArchivos;
import co.edu.uco.kudos.aplicacion.puerto.salida.DetectorTipoArchivo;
import co.edu.uco.kudos.aplicacion.puerto.salida.GestorTransacciones;
import co.edu.uco.kudos.aplicacion.puerto.salida.RegistroAuditoria;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioCategorias;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioKudos;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioOutbox;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioUsuarios;
import co.edu.uco.kudos.dominio.evento.EventoDominio;
import co.edu.uco.kudos.dominio.evento.KudoPublicado;
import co.edu.uco.kudos.dominio.excepcion.CampoObligatorioExcepcion;
import co.edu.uco.kudos.dominio.excepcion.RecursoNoEncontradoExcepcion;
import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;
import co.edu.uco.kudos.dominio.modelo.Adjunto;
import co.edu.uco.kudos.dominio.modelo.Canal;
import co.edu.uco.kudos.dominio.modelo.Categoria;
import co.edu.uco.kudos.dominio.modelo.EntradaAuditoria;
import co.edu.uco.kudos.dominio.modelo.EstadoNotificacion;
import co.edu.uco.kudos.dominio.modelo.Kudo;
import co.edu.uco.kudos.dominio.modelo.Notificacion;
import co.edu.uco.kudos.dominio.modelo.ResultadoAuditoria;
import co.edu.uco.kudos.dominio.modelo.Usuario;
import co.edu.uco.kudos.dominio.regla.ReglaAdjunto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class PublicarKudoCasoUso implements PublicarKudoPuerto {

    private static final String RECURSO_KUDO = "KUDO";
    private static final String MOTIVO_SIN_CELULAR = "El destinatario no tiene un celular verificado";

    private final RepositorioUsuarios usuarios;
    private final RepositorioCategorias categorias;
    private final RepositorioKudos kudos;
    private final RepositorioOutbox outbox;
    private final RegistroAuditoria auditoria;
    private final AlmacenamientoArchivos almacenamiento;
    private final DetectorTipoArchivo detectorTipo;
    private final GestorTransacciones transacciones;
    private final Clock reloj;

    public PublicarKudoCasoUso(RepositorioUsuarios usuarios, RepositorioCategorias categorias,
                               RepositorioKudos kudos, RepositorioOutbox outbox, RegistroAuditoria auditoria,
                               AlmacenamientoArchivos almacenamiento, DetectorTipoArchivo detectorTipo,
                               GestorTransacciones transacciones, Clock reloj) {
        this.usuarios = Objects.requireNonNull(usuarios);
        this.categorias = Objects.requireNonNull(categorias);
        this.kudos = Objects.requireNonNull(kudos);
        this.outbox = Objects.requireNonNull(outbox);
        this.auditoria = Objects.requireNonNull(auditoria);
        this.almacenamiento = Objects.requireNonNull(almacenamiento);
        this.detectorTipo = Objects.requireNonNull(detectorTipo);
        this.transacciones = Objects.requireNonNull(transacciones);
        this.reloj = Objects.requireNonNull(reloj);
    }

    @Override
    public KudoPublicadoResultado publicar(PublicarKudoComando comando) {
        Objects.requireNonNull(comando, "comando");
        Usuario autor = buscarUsuario(comando.autorId(), "autor");
        Usuario destinatario = buscarUsuario(comando.destinatarioId(), "destinatario");
        Categoria categoria = buscarCategoria(comando.categoriaId());

        UUID idKudo = UUID.randomUUID();
        Instant ahora = Instant.now(reloj);
        List<ArchivoPreparado> archivos = prepararAdjuntos(idKudo, comando.adjuntos());
        List<Adjunto> adjuntos = archivos.stream().map(ArchivoPreparado::adjunto).toList();

        Kudo kudo = Kudo.publicar(idKudo, autor, destinatario, categoria, comando.contenido(), adjuntos, ahora);

        List<String> clavesSubidas = new ArrayList<>();
        try {
            for (ArchivoPreparado archivo : archivos) {
                almacenamiento.guardar(archivo.adjunto().claveObjeto(), archivo.adjunto().tipoContenido(),
                        archivo.contenido());
                clavesSubidas.add(archivo.adjunto().claveObjeto());
            }
            transacciones.enTransaccion(() -> persistir(kudo, autor, destinatario, categoria, ahora));
        } catch (RuntimeException error) {
            revertirArchivos(clavesSubidas, error);
            throw error;
        }
        return new KudoPublicadoResultado(idKudo, ahora);
    }

    private Usuario buscarUsuario(UUID id, String campo) {
        if (id == null) {
            throw new CampoObligatorioExcepcion(campo);
        }
        return usuarios.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion(campo, id.toString()));
    }

    private Categoria buscarCategoria(UUID id) {
        if (id == null) {
            throw new CampoObligatorioExcepcion("categoria");
        }
        return categorias.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("categoria", id.toString()));
    }

    private List<ArchivoPreparado> prepararAdjuntos(UUID idKudo, List<ArchivoAdjuntoComando> archivos) {
        ReglaAdjunto.validarCantidad(archivos.size());
        return archivos.stream().map(archivo -> prepararAdjunto(idKudo, archivo)).toList();
    }

    private ArchivoPreparado prepararAdjunto(UUID idKudo, ArchivoAdjuntoComando archivo) {
        byte[] contenido = archivo.contenido();
        String tipo = contenido.length == 0 ? null : detectorTipo.detectar(contenido).orElse(null);
        ReglaAdjunto.validarArchivo(archivo.nombreOriginal(), tipo, contenido.length);
        String clave = "kudos/%s/%s.%s".formatted(idKudo, UUID.randomUUID(), ReglaAdjunto.extension(tipo));
        Adjunto adjunto = new Adjunto(archivo.nombreOriginal(), tipo, contenido.length, sha256(contenido), clave);
        return new ArchivoPreparado(adjunto, contenido);
    }

    private void persistir(Kudo kudo, Usuario autor, Usuario destinatario, Categoria categoria, Instant ahora) {
        kudos.guardar(kudo);
        List<Notificacion> notificaciones = new ArrayList<>();
        for (EventoDominio evento : kudo.extraerEventos()) {
            switch (evento) {
                case KudoPublicado publicado ->
                        notificaciones.addAll(crearNotificaciones(publicado, autor, destinatario, categoria, ahora));
            }
        }
        outbox.guardarTodas(notificaciones);
        auditoria.registrar(EntradaAuditoria.de(ahora, kudo.getId(), autor, AccionAuditoria.KUDO_PUBLICADO,
                RECURSO_KUDO, kudo.getId(), null, ResultadoAuditoria.EXITOSO,
                "destinatario=" + destinatario.getId() + ", categoria=" + categoria.getId()));
        for (Adjunto adjunto : kudo.getAdjuntos()) {
            auditoria.registrar(EntradaAuditoria.de(ahora, kudo.getId(), autor, AccionAuditoria.ADJUNTO_SUBIDO,
                    RECURSO_KUDO, kudo.getId(), null, ResultadoAuditoria.EXITOSO,
                    "tipo=" + adjunto.tipoContenido() + ", tamanoBytes=" + adjunto.tamanoBytes()));
        }
        notificaciones.stream()
                .filter(notificacion -> notificacion.getEstado() == EstadoNotificacion.NO_APLICA)
                .forEach(notificacion -> auditoria.registrar(EntradaAuditoria.de(ahora, kudo.getId(), autor,
                        AccionAuditoria.NOTIFICACION_NO_APLICA, RECURSO_KUDO, kudo.getId(),
                        notificacion.getCanal(), ResultadoAuditoria.NO_APLICA, notificacion.getUltimoError())));
    }

    private List<Notificacion> crearNotificaciones(KudoPublicado evento, Usuario autor, Usuario destinatario,
                                                   Categoria categoria, Instant ahora) {
        Map<String, String> datos = Map.of(
                "idKudo", evento.idKudo().toString(),
                "nombreAutor", autor.getNombreCompleto(),
                "nombreDestinatario", destinatario.getNombreCompleto(),
                "nombreCategoria", categoria.getNombre());
        UUID idEvento = evento.idEvento();
        UUID idCorrelacion = evento.idCorrelacion();

        Notificacion correo = Notificacion.pendiente(idEvento, idCorrelacion, Canal.EMAIL,
                destinatario.getCorreo(), datos, ahora);
        Notificacion sms = destinatario.puedeRecibirSms()
                ? Notificacion.pendiente(idEvento, idCorrelacion, Canal.SMS, destinatario.getCelular(), datos, ahora)
                : Notificacion.noAplica(idEvento, idCorrelacion, Canal.SMS, datos, MOTIVO_SIN_CELULAR, ahora);
        Notificacion push = Notificacion.pendiente(idEvento, idCorrelacion, Canal.PUSH,
                destinatario.getId().toString(), datos, ahora);
        return List.of(correo, sms, push);
    }

    private void revertirArchivos(List<String> claves, RuntimeException errorOriginal) {
        for (String clave : claves) {
            try {
                almacenamiento.eliminar(clave);
            } catch (RuntimeException errorAlEliminar) {
                errorOriginal.addSuppressed(errorAlEliminar);
            }
        }
    }

    private static String sha256(byte[] contenido) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenido));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 no disponible", error);
        }
    }

    private record ArchivoPreparado(Adjunto adjunto, byte[] contenido) {

        @Override
        public boolean equals(Object otro) {
            return otro instanceof ArchivoPreparado archivo && Objects.equals(adjunto, archivo.adjunto)
                    && Arrays.equals(contenido, archivo.contenido);
        }

        @Override
        public int hashCode() {
            return 31 * Objects.hashCode(adjunto) + Arrays.hashCode(contenido);
        }

        @Override
        public String toString() {
            return "ArchivoPreparado[adjunto=" + adjunto + ", contenido=" + Arrays.toString(contenido) + "]";
        }
    }
}
