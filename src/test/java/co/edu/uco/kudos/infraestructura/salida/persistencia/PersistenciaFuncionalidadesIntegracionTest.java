package co.edu.uco.kudos.infraestructura.salida.persistencia;

import co.edu.uco.kudos.aplicacion.dto.FiltroAuditoria;
import co.edu.uco.kudos.aplicacion.dto.PaginaAuditoria;
import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;
import co.edu.uco.kudos.dominio.modelo.Calificacion;
import co.edu.uco.kudos.dominio.modelo.Categoria;
import co.edu.uco.kudos.dominio.modelo.CodigoVerificacion;
import co.edu.uco.kudos.dominio.modelo.EntradaAuditoria;
import co.edu.uco.kudos.dominio.modelo.EstadoUsuario;
import co.edu.uco.kudos.dominio.modelo.Facultad;
import co.edu.uco.kudos.dominio.modelo.Programa;
import co.edu.uco.kudos.dominio.modelo.PropositoCodigo;
import co.edu.uco.kudos.dominio.modelo.Replica;
import co.edu.uco.kudos.dominio.modelo.Reporte;
import co.edu.uco.kudos.dominio.modelo.ResultadoAuditoria;
import co.edu.uco.kudos.dominio.modelo.Rol;
import co.edu.uco.kudos.dominio.modelo.SolicitudRegistro;
import co.edu.uco.kudos.dominio.modelo.TipoContenido;
import co.edu.uco.kudos.dominio.modelo.Usuario;
import co.edu.uco.kudos.dominio.modelo.Veto;
import co.edu.uco.kudos.infraestructura.salida.auditoria.RegistroAuditoriaAdaptador;
import co.edu.uco.kudos.infraestructura.salida.persistencia.adaptador.RepositorioCuentasAdaptador;
import co.edu.uco.kudos.infraestructura.salida.persistencia.consulta.ConsultasKudosAdaptador;
import co.edu.uco.kudos.infraestructura.salida.persistencia.entidad.KudoEntidad;
import co.edu.uco.kudos.infraestructura.salida.persistencia.jdbc.ConsultasAuditoriaJdbc;
import co.edu.uco.kudos.infraestructura.salida.persistencia.jdbc.ConsultasConversacionJdbc;
import co.edu.uco.kudos.infraestructura.salida.persistencia.jdbc.ContenidosModerablesJdbc;
import co.edu.uco.kudos.infraestructura.salida.persistencia.jdbc.MantenimientoDatosJdbc;
import co.edu.uco.kudos.infraestructura.salida.persistencia.jdbc.RepositorioCalificacionesJdbc;
import co.edu.uco.kudos.infraestructura.salida.persistencia.jdbc.RepositorioCategoriasAdministracionJdbc;
import co.edu.uco.kudos.infraestructura.salida.persistencia.jdbc.RepositorioCodigosJdbc;
import co.edu.uco.kudos.infraestructura.salida.persistencia.jdbc.RepositorioEstructuraJdbc;
import co.edu.uco.kudos.infraestructura.salida.persistencia.jdbc.RepositorioModeracionJdbc;
import co.edu.uco.kudos.infraestructura.salida.persistencia.jdbc.RepositorioReplicasJdbc;
import co.edu.uco.kudos.infraestructura.salida.persistencia.jdbc.RepositorioSolicitudesJdbc;
import co.edu.uco.kudos.infraestructura.salida.persistencia.jdbc.RepositorioVinculosJdbc;
import co.edu.uco.kudos.infraestructura.salida.persistencia.repositorio.CategoriaRepositorioJpa;
import co.edu.uco.kudos.infraestructura.salida.persistencia.repositorio.KudoRepositorioJpa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@Import({RepositorioCuentasAdaptador.class, RegistroAuditoriaAdaptador.class})
class PersistenciaFuncionalidadesIntegracionTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private DataSource fuenteDatos;
    @Autowired
    private RepositorioCuentasAdaptador cuentas;
    @Autowired
    private RegistroAuditoriaAdaptador registroAuditoria;
    @Autowired
    private KudoRepositorioJpa kudoRepositorio;
    @Autowired
    private CategoriaRepositorioJpa categoriaRepositorio;

    private NamedParameterJdbcTemplate jdbc;
    private Instant ahora;
    private Usuario autor;
    private Usuario destinatario;
    private UUID kudoId;

    @BeforeEach
    void preparar() {
        jdbc = new NamedParameterJdbcTemplate(fuenteDatos);
        ahora = Instant.now().truncatedTo(ChronoUnit.MICROS);
        autor = activo("autor@uco.edu.co", "+573001110001", Rol.DOCENTE);
        destinatario = activo("destinatario@uco.edu.co", "+573001110002", Rol.ESTUDIANTE);
        UUID categoriaId = categoriaRepositorio.findByNombreIgnoreCase("Liderazgo").orElseThrow().getId();
        kudoId = UUID.randomUUID();
        kudoRepositorio.save(new KudoEntidad(kudoId, autor.getId(), destinatario.getId(), categoriaId, "Gracias", ahora));
        entityManager.flush();
    }

    @Test
    void replicasCalificacionesYVetosFuncionanEnPostgres() {
        RepositorioReplicasJdbc replicas = new RepositorioReplicasJdbc(jdbc);
        ContenidosModerablesJdbc contenidos = new ContenidosModerablesJdbc(jdbc);
        RepositorioCalificacionesJdbc calificaciones = new RepositorioCalificacionesJdbc(jdbc);
        ConsultasConversacionJdbc conversacion = new ConsultasConversacionJdbc(jdbc);
        ConsultasKudosAdaptador consultasKudos = new ConsultasKudosAdaptador(jdbc);

        Replica raiz = Replica.publicar(kudoId, null, autor, "Raíz", ahora);
        replicas.guardar(raiz);
        replicas.guardar(Replica.publicar(kudoId, raiz.id(), destinatario, "Respuesta", ahora.plusSeconds(1)));
        calificaciones.guardarOReemplazar(new Calificacion(autor.getId(), TipoContenido.KUDO, kudoId, 3, ahora));
        calificaciones.guardarOReemplazar(new Calificacion(autor.getId(), TipoContenido.KUDO, kudoId, 5, ahora));
        calificaciones.guardarOReemplazar(new Calificacion(destinatario.getId(), TipoContenido.KUDO, kudoId, 4, ahora));

        assertThat(calificaciones.resumen(TipoContenido.KUDO, kudoId, autor.getId())).satisfies(resumen -> {
            assertThat(resumen.cantidad()).isEqualTo(2);
            assertThat(resumen.promedio()).isEqualTo(4.5);
            assertThat(resumen.miCalificacion()).isEqualTo(5);
        });
        assertThat(conversacion.replicas(kudoId, null, null, 10)).singleElement().satisfies(r -> {
            assertThat(r.cantidadRespuestas()).isEqualTo(1);
            assertThat(r.autor().nombreCompleto()).isEqualTo("autor@uco.edu.co");
        });
        assertThat(conversacion.replicas(kudoId, raiz.id(), null, 10)).singleElement()
                .satisfies(r -> assertThat(r.contenido()).isEqualTo("Respuesta"));
        assertThat(consultasKudos.feed(null, 10)).singleElement().satisfies(k -> {
            assertThat(k.cantidadReplicas()).isEqualTo(2);
            assertThat(k.cantidadCalificaciones()).isEqualTo(2);
        });

        contenidos.marcarVetado(TipoContenido.KUDO, kudoId);

        assertThat(consultasKudos.feed(null, 10)).isEmpty();
        assertThat(consultasKudos.detalle(kudoId)).isEmpty();
        assertThat(contenidos.buscar(TipoContenido.REPLICA, raiz.id()))
                .hasValueSatisfying(estado -> assertThat(estado.vetado()).isTrue());
    }

    @Test
    void reportesYVetosSeRegistranYAtiendenEnPostgres() {
        RepositorioModeracionJdbc moderacion = new RepositorioModeracionJdbc(jdbc);
        ConsultasConversacionJdbc conversacion = new ConsultasConversacionJdbc(jdbc);
        Usuario rector = activo("rector@uco.edu.co", "+573001110003", Rol.RECTOR);

        moderacion.guardarReporte(Reporte.crear(TipoContenido.KUDO, kudoId, destinatario.getId(), "Ofensivo", ahora));

        assertThat(moderacion.existeReporte(TipoContenido.KUDO, kudoId, destinatario.getId())).isTrue();
        assertThat(conversacion.reportesPendientes(10)).singleElement()
                .satisfies(r -> assertThat(r.extractoContenido()).isEqualTo("Gracias"));

        moderacion.guardarVeto(Veto.aplicar(rector, TipoContenido.KUDO, kudoId, "No respeta los valores", ahora));
        moderacion.marcarReportesAtendidos(TipoContenido.KUDO, kudoId, ahora);

        assertThat(conversacion.reportesPendientes(10)).isEmpty();
    }

    @Test
    void estructuraSolicitudesCodigosYVinculosFuncionanEnPostgres() {
        RepositorioEstructuraJdbc estructura = new RepositorioEstructuraJdbc(jdbc);
        RepositorioSolicitudesJdbc solicitudes = new RepositorioSolicitudesJdbc(jdbc);
        RepositorioCodigosJdbc codigos = new RepositorioCodigosJdbc(jdbc);
        RepositorioVinculosJdbc vinculos = new RepositorioVinculosJdbc(jdbc);
        Facultad ingenieria = Facultad.crear("Ingeniería");
        estructura.guardarFacultad(ingenieria);
        Programa sistemas = Programa.crear(ingenieria.id(), "Sistemas");
        estructura.guardarPrograma(sistemas);

        Usuario solicitante = Usuario.registrar("Nuevo Estudiante", "nuevo@uco.edu.co", "+573001110004",
                Rol.ESTUDIANTE, "hash", null, null);
        cuentas.guardar(solicitante);
        SolicitudRegistro solicitud = SolicitudRegistro.crear(solicitante, null, sistemas.id(), ahora);
        solicitudes.guardar(solicitud);

        assertThat(estructura.existeFacultad("INGENIERÍA")).isTrue();
        assertThat(estructura.programasActivos(ingenieria.id())).containsExactly(sistemas);
        assertThat(solicitudes.pendientes(List.of(Rol.ESTUDIANTE, Rol.DOCENTE), sistemas.id(), null, 10))
                .singleElement().satisfies(p -> {
                    assertThat(p.programa()).isEqualTo("Sistemas");
                    assertThat(p.facultad()).isEqualTo("Ingeniería");
                });
        solicitudes.actualizar(solicitud.aprobar(autor.getId(), ahora));
        assertThat(solicitudes.buscarPorUsuario(solicitante.getId()))
                .hasValueSatisfying(s -> assertThat(s.estaAprobada()).isTrue());

        CodigoVerificacion codigo = CodigoVerificacion.emitir(solicitante.getId(), PropositoCodigo.VERIFICAR_CORREO,
                "b".repeat(64), ahora, Duration.ofMinutes(15));
        codigos.guardar(codigo);
        codigos.actualizar(codigo.conIntentoFallido());
        assertThat(codigos.ultimo(solicitante.getId(), PropositoCodigo.VERIFICAR_CORREO))
                .hasValueSatisfying(c -> assertThat(c.intentos()).isEqualTo(1));
        assertThat(codigos.contarEmitidosDesde(solicitante.getId(), PropositoCodigo.VERIFICAR_CORREO,
                ahora.minusSeconds(60))).isEqualTo(1);
        codigos.invalidarVigentes(solicitante.getId(), PropositoCodigo.VERIFICAR_CORREO, ahora);
        assertThat(codigos.buscarPorHash(PropositoCodigo.VERIFICAR_CORREO, "b".repeat(64)))
                .hasValueSatisfying(c -> assertThat(c.estaVigente(ahora)).isFalse());

        vinculos.vincular(destinatario.getId(), sistemas.id());
        vinculos.vincular(destinatario.getId(), sistemas.id());
        assertThat(vinculos.programasDe(destinatario.getId())).containsExactly(sistemas);
        assertThat(vinculos.miembros(sistemas.id())).hasSize(1);
        assertThat(vinculos.desvincular(destinatario.getId(), sistemas.id())).isTrue();
        assertThat(vinculos.desvincular(destinatario.getId(), sistemas.id())).isFalse();
    }

    @Test
    void categoriasAuditoriaYLimpiezaFuncionanEnPostgres() {
        RepositorioCategoriasAdministracionJdbc categorias = new RepositorioCategoriasAdministracionJdbc(jdbc);
        Categoria nueva = Categoria.crear("Resiliencia");
        categorias.guardar(nueva);
        categorias.guardar(nueva.renombrar("Resiliencia Académica").conEstado(false));

        assertThat(categorias.buscarPorId(nueva.getId())).hasValueSatisfying(c -> {
            assertThat(c.getNombre()).isEqualTo("Resiliencia Académica");
            assertThat(c.isActiva()).isFalse();
        });
        assertThat(categorias.existeNombre("LIDERAZGO", null)).isTrue();
        assertThat(categorias.existeNombre("Resiliencia Académica", nueva.getId())).isFalse();

        registroAuditoria.registrar(EntradaAuditoria.de(ahora, kudoId, autor, AccionAuditoria.KUDO_PUBLICADO, "KUDO",
                kudoId, null, ResultadoAuditoria.EXITOSO, null));
        registroAuditoria.registrar(EntradaAuditoria.de(ahora, kudoId, autor, AccionAuditoria.REPLICA_PUBLICADA,
                "REPLICA", UUID.randomUUID(), null, ResultadoAuditoria.EXITOSO, null));
        entityManager.flush();

        PaginaAuditoria pagina = new ConsultasAuditoriaJdbc(jdbc).buscar(new FiltroAuditoria(ahora.minusSeconds(5),
                null, autor.getId(), AccionAuditoria.KUDO_PUBLICADO, null, null, kudoId), 0, 10);
        assertThat(pagina.total()).isEqualTo(1);
        assertThat(pagina.registros()).singleElement()
                .satisfies(r -> assertThat(r.accion()).isEqualTo(AccionAuditoria.KUDO_PUBLICADO));

        MantenimientoDatosJdbc mantenimiento = new MantenimientoDatosJdbc(jdbc);
        assertThat(mantenimiento.eliminarTokensRefrescoVencidos(ahora.plus(Duration.ofDays(1)))).isZero();
        assertThat(mantenimiento.eliminarCodigosVencidos(ahora)).isZero();
        assertThat(mantenimiento.eliminarNotificacionesFinalizadas(ahora)).isZero();
    }

    private Usuario activo(String correo, String celular, Rol rol) {
        Usuario usuario = new Usuario(UUID.randomUUID(), correo, correo, celular, true, true, rol, EstadoUsuario.ACTIVO,
                "hash", 0, null, null, null);
        cuentas.guardar(usuario);
        return usuario;
    }
}
