package co.edu.uco.kudos.infraestructura.salida.persistencia;

import co.edu.uco.kudos.aplicacion.casouso.PublicarKudoCasoUso;
import co.edu.uco.kudos.aplicacion.dto.ArchivoAdjuntoComando;
import co.edu.uco.kudos.aplicacion.dto.KudoPublicadoResultado;
import co.edu.uco.kudos.aplicacion.dto.PublicarKudoComando;
import co.edu.uco.kudos.aplicacion.puerto.salida.AlmacenamientoArchivos;
import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;
import co.edu.uco.kudos.dominio.modelo.Canal;
import co.edu.uco.kudos.dominio.modelo.EstadoNotificacion;
import co.edu.uco.kudos.dominio.modelo.EstadoUsuario;
import co.edu.uco.kudos.dominio.modelo.Rol;
import co.edu.uco.kudos.infraestructura.salida.auditoria.RegistroAuditoriaAdaptador;
import co.edu.uco.kudos.infraestructura.salida.persistencia.adaptador.GestorTransaccionesAdaptador;
import co.edu.uco.kudos.infraestructura.salida.persistencia.adaptador.RepositorioCategoriasAdaptador;
import co.edu.uco.kudos.infraestructura.salida.persistencia.adaptador.RepositorioKudosAdaptador;
import co.edu.uco.kudos.infraestructura.salida.persistencia.adaptador.RepositorioNotificacionesPendientesAdaptador;
import co.edu.uco.kudos.infraestructura.salida.persistencia.adaptador.RepositorioCuentasAdaptador;
import co.edu.uco.kudos.infraestructura.salida.persistencia.adaptador.RepositorioTokensRefrescoAdaptador;
import co.edu.uco.kudos.dominio.modelo.TokenRefresco;
import co.edu.uco.kudos.dominio.modelo.Usuario;
import co.edu.uco.kudos.dominio.regla.PoliticaBloqueo;
import co.edu.uco.kudos.aplicacion.casouso.ConsultarFeedCasoUso;
import co.edu.uco.kudos.aplicacion.dto.PaginaFeed;
import co.edu.uco.kudos.infraestructura.salida.persistencia.consulta.ConsultasKudosAdaptador;
import co.edu.uco.kudos.infraestructura.salida.persistencia.consulta.ConsultasUsuariosAdaptador;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import javax.sql.DataSource;
import co.edu.uco.kudos.dominio.modelo.Notificacion;
import java.time.Duration;
import co.edu.uco.kudos.infraestructura.salida.persistencia.adaptador.RepositorioOutboxAdaptador;
import co.edu.uco.kudos.infraestructura.salida.persistencia.adaptador.RepositorioUsuariosAdaptador;
import co.edu.uco.kudos.infraestructura.salida.persistencia.entidad.KudoEntidad;
import co.edu.uco.kudos.infraestructura.salida.persistencia.entidad.NotificacionOutboxEntidad;
import co.edu.uco.kudos.infraestructura.salida.persistencia.entidad.RegistroAuditoriaEntidad;
import co.edu.uco.kudos.infraestructura.salida.persistencia.entidad.UsuarioEntidad;
import co.edu.uco.kudos.infraestructura.salida.persistencia.repositorio.CategoriaRepositorioJpa;
import co.edu.uco.kudos.infraestructura.salida.persistencia.repositorio.KudoRepositorioJpa;
import co.edu.uco.kudos.infraestructura.salida.persistencia.repositorio.NotificacionOutboxRepositorioJpa;
import co.edu.uco.kudos.infraestructura.salida.persistencia.repositorio.RegistroAuditoriaRepositorioJpa;
import co.edu.uco.kudos.infraestructura.salida.persistencia.repositorio.UsuarioRepositorioJpa;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@Import({RepositorioUsuariosAdaptador.class, RepositorioCategoriasAdaptador.class, RepositorioKudosAdaptador.class,
        RepositorioOutboxAdaptador.class, RegistroAuditoriaAdaptador.class, GestorTransaccionesAdaptador.class,
        RepositorioNotificacionesPendientesAdaptador.class, RepositorioCuentasAdaptador.class,
        RepositorioTokensRefrescoAdaptador.class})
class PersistenciaPublicarKudoIntegracionTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private UsuarioRepositorioJpa usuarioRepositorio;
    @Autowired
    private CategoriaRepositorioJpa categoriaRepositorio;
    @Autowired
    private KudoRepositorioJpa kudoRepositorio;
    @Autowired
    private NotificacionOutboxRepositorioJpa outboxRepositorio;
    @Autowired
    private RegistroAuditoriaRepositorioJpa auditoriaRepositorio;
    @Autowired
    private RepositorioUsuariosAdaptador usuarios;
    @Autowired
    private RepositorioCategoriasAdaptador categorias;
    @Autowired
    private RepositorioKudosAdaptador kudos;
    @Autowired
    private RepositorioOutboxAdaptador outbox;
    @Autowired
    private RegistroAuditoriaAdaptador auditoria;
    @Autowired
    private GestorTransaccionesAdaptador transacciones;
    @Autowired
    private RepositorioNotificacionesPendientesAdaptador pendientes;
    @Autowired
    private RepositorioCuentasAdaptador cuentas;
    @Autowired
    private DataSource fuenteDatos;
    @Autowired
    private RepositorioTokensRefrescoAdaptador tokensRefresco;

    private final List<String> archivosGuardados = new ArrayList<>();
    private UsuarioEntidad autor;
    private UsuarioEntidad destinatario;
    private PublicarKudoCasoUso casoUso;

    @BeforeEach
    void preparar() {
        autor = usuarioRepositorio.save(usuario("Ana Autora", "+573001111111"));
        destinatario = usuarioRepositorio.save(usuario("Diego Destinatario", "+573002222222"));
        AlmacenamientoArchivos almacenamiento = new AlmacenamientoArchivos() {
            @Override
            public void guardar(String clave, String tipoContenido, byte[] contenido) {
                archivosGuardados.add(clave);
            }

            @Override
            public void eliminar(String clave) {
                archivosGuardados.remove(clave);
            }
        };
        casoUso = new PublicarKudoCasoUso(usuarios, categorias, kudos, outbox, auditoria, almacenamiento,
                contenido -> Optional.of("image/png"), transacciones, Clock.systemUTC());
    }

    @Test
    void lasCategoriasActivasSonExactamenteLasQuinceDelEnunciado() {
        assertThat(categoriaRepositorio.findByActivaTrueOrderByNombreAsc())
                .extracting(categoria -> categoria.getNombre())
                .containsExactlyInAnyOrder("Rendimiento Académico", "Mejor Investigador", "Empatía",
                        "Comunicación Asertiva", "Orientación al Logro", "Trabajo en Equipo", "Trabajo Bajo Presión",
                        "Visión Estratégica", "Innovación", "Liderazgo", "Transformación Digital", "Mentor Inspirador",
                        "Espíritu UCO", "Servicio a la Comunidad", "Compromiso Institucional");
        assertThat(categoriaRepositorio.findByNombreIgnoreCase("Respeto"))
                .hasValueSatisfying(categoria -> assertThat(categoria.isActiva()).isFalse());
    }

    @Test
    void elFeedSePaginaConCursorEnPostgresYElDetalleTraeLosAdjuntos() {
        ConsultasKudosAdaptador consultas = new ConsultasKudosAdaptador(new NamedParameterJdbcTemplate(fuenteDatos));
        UUID categoriaId = categoriaRepositorio.findByNombreIgnoreCase("Liderazgo").orElseThrow().getId();
        for (int i = 0; i < 3; i++) {
            casoUso.publicar(new PublicarKudoComando(autor.getId(), destinatario.getId(), categoriaId,
                    "x".repeat(300) + i, List.of(new ArchivoAdjuntoComando("evidencia.png", PNG))));
        }
        entityManager.flush();

        ConsultarFeedCasoUso feed = new ConsultarFeedCasoUso(consultas);
        PaginaFeed primera = feed.consultar(null, 2);
        PaginaFeed segunda = feed.consultar(primera.siguienteCursor(), 2);

        assertThat(primera.kudos()).hasSize(2);
        assertThat(segunda.kudos()).hasSize(1);
        assertThat(segunda.siguienteCursor()).isNull();
        assertThat(primera.kudos()).allSatisfy(kudo -> {
            assertThat(kudo.extracto()).hasSize(280);
            assertThat(kudo.contenidoTruncado()).isTrue();
            assertThat(kudo.cantidadAdjuntos()).isEqualTo(1);
            assertThat(kudo.autor().nombreCompleto()).isEqualTo("Ana Autora");
            assertThat(kudo.categoria().nombre()).isEqualTo("Liderazgo");
        });
        assertThat(primera.kudos().getFirst().fechaPublicacion())
                .isAfterOrEqualTo(primera.kudos().getLast().fechaPublicacion());

        UUID id = segunda.kudos().getFirst().id();
        assertThat(consultas.detalle(id)).hasValueSatisfying(detalle -> {
            assertThat(detalle.contenido()).hasSize(301);
            assertThat(detalle.adjuntos()).singleElement()
                    .satisfies(adjunto -> assertThat(adjunto.tipoContenido()).isEqualTo("image/png"));
        });
        assertThat(consultas.detalle(UUID.randomUUID())).isEmpty();
    }

    @Test
    void laBusquedaDeUsuariosEncuentraActivosPorNombreOCorreoYExcluyeAlSolicitante() {
        ConsultasUsuariosAdaptador consultas = new ConsultasUsuariosAdaptador(new NamedParameterJdbcTemplate(fuenteDatos));
        entityManager.flush();

        assertThat(consultas.buscarActivos("destina", 10, autor.getId()))
                .extracting(persona -> persona.nombreCompleto()).containsExactly("Diego Destinatario");
        assertThat(consultas.buscarActivos("Ana", 10, autor.getId())).isEmpty();
        assertThat(consultas.buscarActivos("%", 10, null)).isEmpty();
    }

    @Test
    void publicarKudoGuardaKudoAdjuntoOutboxYAuditoriaEnPostgres() {
        UUID categoriaId = categoriaRepositorio.findByNombreIgnoreCase("Liderazgo").orElseThrow().getId();

        KudoPublicadoResultado resultado = casoUso.publicar(new PublicarKudoComando(autor.getId(),
                destinatario.getId(), categoriaId, "Gracias por tu acompañamiento",
                List.of(new ArchivoAdjuntoComando("evidencia.png", PNG))));
        entityManager.flush();
        entityManager.clear();

        KudoEntidad kudo = kudoRepositorio.findById(resultado.idKudo()).orElseThrow();
        assertThat(kudo.getAdjuntos()).singleElement()
                .satisfies(adjunto -> assertThat(archivosGuardados).contains(adjunto.getClaveObjeto()));

        List<NotificacionOutboxEntidad> notificaciones = outboxRepositorio.findByIdCorrelacion(resultado.idKudo());
        assertThat(notificaciones).extracting(NotificacionOutboxEntidad::getCanal)
                .containsExactlyInAnyOrder(Canal.EMAIL, Canal.WHATSAPP, Canal.PUSH);
        assertThat(notificaciones).allSatisfy(notificacion -> {
            assertThat(notificacion.getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE);
            assertThat(notificacion.getPayload()).containsEntry("nombreAutor", "Ana Autora")
                    .containsEntry("nombreCategoria", "Liderazgo");
        });

        assertThat(auditoriaRepositorio.findByIdCorrelacionOrderByFechaHoraAsc(resultado.idKudo()))
                .extracting(RegistroAuditoriaEntidad::getAccion)
                .containsExactlyInAnyOrder(AccionAuditoria.KUDO_PUBLICADO, AccionAuditoria.ADJUNTO_SUBIDO);
    }

    @Test
    void laBaseDeDatosImpideModificarLaAuditoria() {
        UUID idCorrelacion = publicarKudoSinAdjuntos();

        assertThatThrownBy(() -> entityManager.getEntityManager()
                .createNativeQuery("UPDATE registro_auditoria SET detalle = 'alterado' WHERE id_correlacion = :id")
                .setParameter("id", idCorrelacion)
                .executeUpdate())
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    void laBaseDeDatosImpideEliminarLaAuditoria() {
        UUID idCorrelacion = publicarKudoSinAdjuntos();

        assertThatThrownBy(() -> entityManager.getEntityManager()
                .createNativeQuery("DELETE FROM registro_auditoria WHERE id_correlacion = :id")
                .setParameter("id", idCorrelacion)
                .executeUpdate())
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    void laBaseDeDatosImpideElAutoReconocimientoAunqueSeSalteElDominio() {
        UUID categoriaId = categoriaRepositorio.findByNombreIgnoreCase("Empatía").orElseThrow().getId();

        assertThatThrownBy(() -> {
            kudoRepositorio.save(new KudoEntidad(UUID.randomUUID(), autor.getId(), autor.getId(), categoriaId,
                    "Me reconozco", Instant.now()));
            entityManager.flush();
        }).isInstanceOf(Exception.class);
    }

    @Test
    void reservaLasNotificacionesPendientesUnaSolaVezYActualizaSuResultado() {
        UUID idKudo = publicarKudoSinAdjuntos();
        Instant ahora = Instant.now().plusSeconds(1);

        List<Notificacion> reservadas = pendientes.reservarPendientes(ahora, 10, Duration.ofMinutes(2));
        entityManager.flush();
        List<Notificacion> segundaReserva = pendientes.reservarPendientes(ahora, 10, Duration.ofMinutes(2));

        assertThat(reservadas).hasSize(3).allSatisfy(n -> assertThat(n.getIdCorrelacion()).isEqualTo(idKudo));
        assertThat(segundaReserva).isEmpty();

        Notificacion enviada = reservadas.getFirst().marcarEnviada();
        pendientes.actualizar(enviada);
        entityManager.flush();
        entityManager.clear();

        NotificacionOutboxEntidad guardada = outboxRepositorio.findById(enviada.getId()).orElseThrow();
        assertThat(guardada.getEstado()).isEqualTo(EstadoNotificacion.ENVIADO);
        assertThat(guardada.getIntentos()).isEqualTo(1);
    }

    @Test
    void guardaElBloqueoDeLaCuentaYLaBuscaPorCorreoSinImportarMayusculas() {
        Instant ahora = Instant.now();
        Usuario bloqueado = cuentas.buscarPorCorreo(autor.getCorreo().toUpperCase()).orElseThrow()
                .registrarIntentoFallido(ahora, new PoliticaBloqueo(1, Duration.ofMinutes(15)));

        cuentas.guardar(bloqueado);
        entityManager.flush();
        entityManager.clear();

        Usuario leido = cuentas.buscarPorCorreo(autor.getCorreo()).orElseThrow();
        assertThat(leido.estaBloqueado(ahora.plusSeconds(1))).isTrue();
        assertThat(cuentas.existeConRol(Rol.ESTUDIANTE)).isTrue();
        assertThat(cuentas.existeConRol(Rol.RECTOR)).isFalse();
    }

    @Test
    void revocaUnTokenDeRefrescoUnaSolaVez() {
        Instant ahora = Instant.now();
        TokenRefresco token = TokenRefresco.emitir(autor.getId(), "a".repeat(64), ahora, Duration.ofDays(7));
        tokensRefresco.guardar(token);
        entityManager.flush();

        boolean primera = tokensRefresco.revocarSiVigente(token.id(), ahora, UUID.randomUUID());
        boolean segunda = tokensRefresco.revocarSiVigente(token.id(), ahora, UUID.randomUUID());

        assertThat(primera).isTrue();
        assertThat(segunda).isFalse();
        assertThat(tokensRefresco.buscarPorHash("a".repeat(64))).hasValueSatisfying(
                leido -> assertThat(leido.estaRevocado()).isTrue());
    }

    private UUID publicarKudoSinAdjuntos() {
        UUID categoriaId = categoriaRepositorio.findByNombreIgnoreCase("Innovación").orElseThrow().getId();
        KudoPublicadoResultado resultado = casoUso.publicar(new PublicarKudoComando(autor.getId(),
                destinatario.getId(), categoriaId, "Excelente propuesta", List.of()));
        entityManager.flush();
        return resultado.idKudo();
    }

    private static UsuarioEntidad usuario(String nombre, String celular) {
        return new UsuarioEntidad(UUID.randomUUID(), nombre, UUID.randomUUID() + "@uco.net.co", celular, true,
                Rol.ESTUDIANTE, EstadoUsuario.ACTIVO);
    }
}
