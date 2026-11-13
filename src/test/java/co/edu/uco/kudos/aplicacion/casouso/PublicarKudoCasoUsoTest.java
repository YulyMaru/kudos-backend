package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.dto.ArchivoAdjuntoComando;
import co.edu.uco.kudos.aplicacion.dto.KudoPublicadoResultado;
import co.edu.uco.kudos.aplicacion.dto.PublicarKudoComando;
import co.edu.uco.kudos.aplicacion.puerto.salida.AlmacenamientoArchivos;
import co.edu.uco.kudos.aplicacion.puerto.salida.DetectorTipoArchivo;
import co.edu.uco.kudos.aplicacion.puerto.salida.GestorTransacciones;
import co.edu.uco.kudos.aplicacion.puerto.salida.RegistroAuditoria;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioCategorias;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioKudos;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioOutbox;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioUsuarios;
import co.edu.uco.kudos.dominio.excepcion.RecursoNoEncontradoExcepcion;
import co.edu.uco.kudos.dominio.excepcion.ReglaNegocioExcepcion;
import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;
import co.edu.uco.kudos.dominio.modelo.Canal;
import co.edu.uco.kudos.dominio.modelo.Categoria;
import co.edu.uco.kudos.dominio.modelo.EntradaAuditoria;
import co.edu.uco.kudos.dominio.modelo.EstadoNotificacion;
import co.edu.uco.kudos.dominio.modelo.EstadoUsuario;
import co.edu.uco.kudos.dominio.modelo.Kudo;
import co.edu.uco.kudos.dominio.modelo.Notificacion;
import co.edu.uco.kudos.dominio.modelo.Rol;
import co.edu.uco.kudos.dominio.modelo.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PublicarKudoCasoUsoTest {

    private static final Instant AHORA = Instant.parse("2026-11-10T15:00:00Z");
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};

    private final Usuario autor = usuario("Ana Autora", "+573001111111", true);
    private final Usuario destinatario = usuario("Diego Destinatario", "+573002222222", true);
    private final Categoria categoria = new Categoria(UUID.randomUUID(), "Liderazgo", true);

    private UsuariosEnMemoria usuarios;
    private KudosEnMemoria kudos;
    private OutboxEnMemoria outbox;
    private AuditoriaEnMemoria auditoria;
    private AlmacenamientoEnMemoria almacenamiento;
    private PublicarKudoCasoUso casoUso;

    @BeforeEach
    void preparar() {
        usuarios = new UsuariosEnMemoria();
        usuarios.agregar(autor);
        usuarios.agregar(destinatario);
        kudos = new KudosEnMemoria();
        outbox = new OutboxEnMemoria();
        auditoria = new AuditoriaEnMemoria();
        almacenamiento = new AlmacenamientoEnMemoria();
        casoUso = crearCasoUso(outbox);
    }

    @Test
    void publicaKudoYGuardaTresNotificacionesYAuditoriaEnLaMismaTransaccion() {
        KudoPublicadoResultado resultado = casoUso.publicar(comando(destinatario.getId(), List.of()));

        assertThat(resultado.fechaPublicacion()).isEqualTo(AHORA);
        assertThat(kudos.guardados).singleElement().extracting(Kudo::getId).isEqualTo(resultado.idKudo());
        assertThat(outbox.guardadas).extracting(Notificacion::getCanal)
                .containsExactlyInAnyOrder(Canal.EMAIL, Canal.SMS, Canal.PUSH);
        assertThat(outbox.guardadas).allSatisfy(notificacion -> {
            assertThat(notificacion.getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE);
            assertThat(notificacion.getIdCorrelacion()).isEqualTo(resultado.idKudo());
            assertThat(notificacion.getDatos()).containsEntry("nombreAutor", "Ana Autora")
                    .containsEntry("nombreCategoria", "Liderazgo");
        });
        assertThat(destinoDe(Canal.EMAIL)).isEqualTo(destinatario.getCorreo());
        assertThat(destinoDe(Canal.SMS)).isEqualTo(destinatario.getCelular());
        assertThat(destinoDe(Canal.PUSH)).isEqualTo(destinatario.getId().toString());
        assertThat(auditoria.entradas).singleElement().satisfies(entrada -> {
            assertThat(entrada.accion()).isEqualTo(AccionAuditoria.KUDO_PUBLICADO);
            assertThat(entrada.idCorrelacion()).isEqualTo(resultado.idKudo());
            assertThat(entrada.actorId()).isEqualTo(autor.getId());
        });
    }

    @Test
    void marcaSmsComoNoAplicaCuandoElDestinatarioNoTieneCelularVerificado() {
        Usuario sinCelular = usuario("Sin Celular", null, false);
        usuarios.agregar(sinCelular);

        casoUso.publicar(comando(sinCelular.getId(), List.of()));

        Notificacion sms = outbox.guardadas.stream()
                .filter(notificacion -> notificacion.getCanal() == Canal.SMS).findFirst().orElseThrow();
        assertThat(sms.getEstado()).isEqualTo(EstadoNotificacion.NO_APLICA);
        assertThat(auditoria.entradas).extracting(EntradaAuditoria::accion)
                .contains(AccionAuditoria.NOTIFICACION_NO_APLICA);
    }

    @Test
    void rechazaDestinatarioInexistenteSinGuardarNada() {
        PublicarKudoComando comando = comando(UUID.randomUUID(), List.of());

        assertThatThrownBy(() -> casoUso.publicar(comando)).isInstanceOf(RecursoNoEncontradoExcepcion.class);
        assertThat(kudos.guardados).isEmpty();
        assertThat(outbox.guardadas).isEmpty();
    }

    @Test
    void rechazaAutoReconocimientoSinSubirArchivosNiGuardar() {
        PublicarKudoComando comando = comando(autor.getId(), List.of(new ArchivoAdjuntoComando("foto.png", PNG)));

        assertThatThrownBy(() -> casoUso.publicar(comando))
                .isInstanceOf(ReglaNegocioExcepcion.class)
                .extracting("codigo").isEqualTo("AUTO_RECONOCIMIENTO");
        assertThat(almacenamiento.archivos).isEmpty();
        assertThat(kudos.guardados).isEmpty();
    }

    @Test
    void subeAdjuntoValidoConHashYLoAudita() {
        KudoPublicadoResultado resultado = casoUso.publicar(
                comando(destinatario.getId(), List.of(new ArchivoAdjuntoComando("foto.png", PNG))));

        Kudo kudo = kudos.guardados.getFirst();
        assertThat(kudo.getAdjuntos()).singleElement().satisfies(adjunto -> {
            assertThat(adjunto.tipoContenido()).isEqualTo("image/png");
            assertThat(adjunto.hashSha256()).hasSize(64);
            assertThat(adjunto.claveObjeto()).startsWith("kudos/" + resultado.idKudo() + "/").endsWith(".png");
            assertThat(almacenamiento.archivos).containsKey(adjunto.claveObjeto());
        });
        assertThat(auditoria.entradas).extracting(EntradaAuditoria::accion)
                .containsExactly(AccionAuditoria.KUDO_PUBLICADO, AccionAuditoria.ADJUNTO_SUBIDO);
    }

    @Test
    void rechazaAdjuntoDeTipoNoPermitidoPorContenido() {
        byte[] ejecutable = {'M', 'Z', 0, 0, 1, 2};
        PublicarKudoComando comando = comando(destinatario.getId(),
                List.of(new ArchivoAdjuntoComando("foto.png", ejecutable)));

        assertThatThrownBy(() -> casoUso.publicar(comando))
                .isInstanceOf(ReglaNegocioExcepcion.class)
                .extracting("codigo").isEqualTo("TIPO_ADJUNTO_NO_PERMITIDO");
        assertThat(almacenamiento.archivos).isEmpty();
    }

    @Test
    void siFallaLaTransaccionNoQuedaNadaGuardadoYSeEliminanLosArchivosSubidos() {
        casoUso = crearCasoUso(notificaciones -> {
            throw new IllegalStateException("Base de datos no disponible");
        });
        PublicarKudoComando comando = comando(destinatario.getId(),
                List.of(new ArchivoAdjuntoComando("foto.png", PNG)));

        assertThatThrownBy(() -> casoUso.publicar(comando)).isInstanceOf(IllegalStateException.class);
        assertThat(kudos.guardados).isEmpty();
        assertThat(auditoria.entradas).isEmpty();
        assertThat(almacenamiento.archivos).isEmpty();
        assertThat(almacenamiento.eliminadas).hasSize(1);
    }

    private PublicarKudoCasoUso crearCasoUso(RepositorioOutbox repositorioOutbox) {
        RepositorioCategorias categorias = id -> categoria.getId().equals(id) ? Optional.of(categoria) : Optional.empty();
        return new PublicarKudoCasoUso(usuarios, categorias, kudos, repositorioOutbox, auditoria, almacenamiento,
                new DetectorPorFirma(), new TransaccionesEnMemoria(), Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private PublicarKudoComando comando(UUID destinatarioId, List<ArchivoAdjuntoComando> adjuntos) {
        return new PublicarKudoComando(autor.getId(), destinatarioId, categoria.getId(),
                "Gracias por acompañar el semillero", adjuntos);
    }

    private String destinoDe(Canal canal) {
        return outbox.guardadas.stream().filter(notificacion -> notificacion.getCanal() == canal)
                .findFirst().orElseThrow().getDestinatario();
    }

    private static Usuario usuario(String nombre, String celular, boolean celularVerificado) {
        return new Usuario(UUID.randomUUID(), nombre, nombre.toLowerCase().replace(' ', '.') + "@uco.net.co",
                celular, celularVerificado, Rol.ESTUDIANTE, EstadoUsuario.ACTIVO);
    }

    private static final class UsuariosEnMemoria implements RepositorioUsuarios {
        private final Map<UUID, Usuario> datos = new HashMap<>();

        void agregar(Usuario usuario) {
            datos.put(usuario.getId(), usuario);
        }

        @Override
        public Optional<Usuario> buscarPorId(UUID id) {
            return Optional.ofNullable(datos.get(id));
        }
    }

    private static final class KudosEnMemoria implements RepositorioKudos {
        private final List<Kudo> guardados = new ArrayList<>();

        @Override
        public void guardar(Kudo kudo) {
            guardados.add(kudo);
        }
    }

    private static final class OutboxEnMemoria implements RepositorioOutbox {
        private final List<Notificacion> guardadas = new ArrayList<>();

        @Override
        public void guardarTodas(List<Notificacion> notificaciones) {
            guardadas.addAll(notificaciones);
        }
    }

    private static final class AuditoriaEnMemoria implements RegistroAuditoria {
        private final List<EntradaAuditoria> entradas = new ArrayList<>();

        @Override
        public void registrar(EntradaAuditoria entrada) {
            entradas.add(entrada);
        }
    }

    private static final class AlmacenamientoEnMemoria implements AlmacenamientoArchivos {
        private final Map<String, byte[]> archivos = new HashMap<>();
        private final List<String> eliminadas = new ArrayList<>();

        @Override
        public void guardar(String clave, String tipoContenido, byte[] contenido) {
            archivos.put(clave, contenido);
        }

        @Override
        public void eliminar(String clave) {
            archivos.remove(clave);
            eliminadas.add(clave);
        }
    }

    private static final class DetectorPorFirma implements DetectorTipoArchivo {
        @Override
        public Optional<String> detectar(byte[] contenido) {
            if (contenido.length >= 4 && contenido[0] == (byte) 0x89 && contenido[1] == 'P') {
                return Optional.of("image/png");
            }
            return Optional.empty();
        }
    }

    private final class TransaccionesEnMemoria implements GestorTransacciones {
        @Override
        public void enTransaccion(Runnable operacion) {
            int kudosAntes = kudos.guardados.size();
            int notificacionesAntes = outbox.guardadas.size();
            int auditoriaAntes = auditoria.entradas.size();
            try {
                operacion.run();
            } catch (RuntimeException error) {
                kudos.guardados.subList(kudosAntes, kudos.guardados.size()).clear();
                outbox.guardadas.subList(notificacionesAntes, outbox.guardadas.size()).clear();
                auditoria.entradas.subList(auditoriaAntes, auditoria.entradas.size()).clear();
                throw error;
            }
        }
    }
}
