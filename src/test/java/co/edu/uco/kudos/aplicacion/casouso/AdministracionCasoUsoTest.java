package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.dto.AdjuntoDatos;
import co.edu.uco.kudos.aplicacion.dto.CategoriaVista;
import co.edu.uco.kudos.aplicacion.dto.CursorFeed;
import co.edu.uco.kudos.aplicacion.dto.EnlaceAdjunto;
import co.edu.uco.kudos.aplicacion.dto.FiltroAuditoria;
import co.edu.uco.kudos.aplicacion.dto.KudoDetalleDatos;
import co.edu.uco.kudos.aplicacion.dto.KudoResumenVista;
import co.edu.uco.kudos.aplicacion.dto.PaginaAuditoria;
import co.edu.uco.kudos.aplicacion.dto.ResultadoLimpieza;
import co.edu.uco.kudos.aplicacion.puerto.salida.ConsultasKudos;
import co.edu.uco.kudos.aplicacion.puerto.salida.GestorTransacciones;
import co.edu.uco.kudos.aplicacion.puerto.salida.MantenimientoDatos;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioCategoriasAdministracion;
import co.edu.uco.kudos.dominio.excepcion.AccesoDenegadoExcepcion;
import co.edu.uco.kudos.dominio.excepcion.ParametroInvalidoExcepcion;
import co.edu.uco.kudos.dominio.excepcion.RecursoNoEncontradoExcepcion;
import co.edu.uco.kudos.dominio.excepcion.ReglaNegocioExcepcion;
import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;
import co.edu.uco.kudos.dominio.modelo.Categoria;
import co.edu.uco.kudos.dominio.modelo.EntradaAuditoria;
import co.edu.uco.kudos.dominio.modelo.EstadoUsuario;
import co.edu.uco.kudos.dominio.modelo.Facultad;
import co.edu.uco.kudos.dominio.modelo.Programa;
import co.edu.uco.kudos.dominio.modelo.Rol;
import co.edu.uco.kudos.dominio.modelo.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdministracionCasoUsoTest {

    private static final Instant AHORA = Instant.parse("2026-11-11T10:00:00Z");

    private DoblesAutenticacion.CuentasEnMemoria usuarios;
    private DoblesAutenticacion.EstructuraEnMemoria estructura;
    private DoblesAutenticacion.VinculosEnMemoria vinculos;
    private DoblesAutenticacion.RelojAjustable reloj;
    private List<EntradaAuditoria> auditoria;
    private GestorTransacciones transacciones;

    @BeforeEach
    void preparar() {
        usuarios = new DoblesAutenticacion.CuentasEnMemoria();
        estructura = new DoblesAutenticacion.EstructuraEnMemoria();
        vinculos = new DoblesAutenticacion.VinculosEnMemoria();
        reloj = new DoblesAutenticacion.RelojAjustable(AHORA);
        auditoria = new ArrayList<>();
        transacciones = Runnable::run;
    }

    @Test
    void losRolesAutorizadosCreanRenombranYDesactivanCategorias() {
        CategoriasEnMemoria categorias = new CategoriasEnMemoria();
        AdministrarCategoriasCasoUso casoUso = new AdministrarCategoriasCasoUso(usuarios, categorias, auditoria::add,
                transacciones, reloj);
        Usuario academico = usuario(Rol.COORDINADOR_ACADEMICO, null, null);

        CategoriaVista creada = casoUso.crear(academico.getId(), "  Resiliencia  ");
        casoUso.renombrar(academico.getId(), creada.id(), "Resiliencia Académica");
        casoUso.cambiarEstado(academico.getId(), creada.id(), false);

        Categoria guardada = categorias.datos.get(creada.id());
        assertThat(guardada.getNombre()).isEqualTo("Resiliencia Académica");
        assertThat(guardada.isActiva()).isFalse();
        assertThat(auditoria).extracting(EntradaAuditoria::accion)
                .containsExactly(AccionAuditoria.CATEGORIA_CREADA, AccionAuditoria.CATEGORIA_ACTUALIZADA,
                        AccionAuditoria.CATEGORIA_ACTUALIZADA);
    }

    @Test
    void noSeDuplicaElNombreDeUnaCategoriaNiLaAdministraOtroRol() {
        CategoriasEnMemoria categorias = new CategoriasEnMemoria();
        AdministrarCategoriasCasoUso casoUso = new AdministrarCategoriasCasoUso(usuarios, categorias, auditoria::add,
                transacciones, reloj);
        Usuario rector = usuario(Rol.RECTOR, null, null);
        Usuario estudiante = usuario(Rol.ESTUDIANTE, null, null);
        casoUso.crear(rector.getId(), "Liderazgo");

        assertThatThrownBy(() -> casoUso.crear(rector.getId(), "LIDERAZGO"))
                .isInstanceOf(ReglaNegocioExcepcion.class).extracting("codigo").isEqualTo("CATEGORIA_DUPLICADA");
        assertThatThrownBy(() -> casoUso.crear(estudiante.getId(), "Otra"))
                .isInstanceOf(AccesoDenegadoExcepcion.class);
    }

    @Test
    void laEstructuraLaAdministranElAdministradorYElCoordinadorAcademico() {
        AdministrarEstructuraCasoUso casoUso = new AdministrarEstructuraCasoUso(usuarios, estructura, auditoria::add,
                transacciones, reloj);
        Usuario administrador = usuario(Rol.ADMINISTRADOR, null, null);
        Usuario docente = usuario(Rol.DOCENTE, null, null);

        Facultad ingenieria = casoUso.crearFacultad(administrador.getId(), "Ingeniería");
        Programa sistemas = casoUso.crearPrograma(administrador.getId(), ingenieria.id(), "Ingeniería de Sistemas");

        assertThat(casoUso.facultades()).containsExactly(ingenieria);
        assertThat(casoUso.programas(ingenieria.id())).containsExactly(sistemas);
        assertThatThrownBy(() -> casoUso.crearFacultad(administrador.getId(), "ingeniería"))
                .isInstanceOf(ReglaNegocioExcepcion.class).extracting("codigo").isEqualTo("FACULTAD_DUPLICADA");
        assertThatThrownBy(() -> casoUso.crearPrograma(docente.getId(), ingenieria.id(), "Otro"))
                .isInstanceOf(AccesoDenegadoExcepcion.class);
        assertThatThrownBy(() -> casoUso.crearPrograma(administrador.getId(), UUID.randomUUID(), "Otro"))
                .isInstanceOf(RecursoNoEncontradoExcepcion.class);
    }

    @Test
    void elCoordinadorDelProgramaVinculaYDesvinculaEstudiantesYDocentes() {
        Facultad ingenieria = Facultad.crear("Ingeniería");
        estructura.guardarFacultad(ingenieria);
        Programa sistemas = Programa.crear(ingenieria.id(), "Sistemas");
        Programa industrial = Programa.crear(ingenieria.id(), "Industrial");
        estructura.guardarPrograma(sistemas);
        estructura.guardarPrograma(industrial);
        Usuario coordinador = usuario(Rol.COORDINADOR_PROGRAMA, null, sistemas.id());
        Usuario coordinadorFacultad = usuario(Rol.COORDINADOR_FACULTAD, ingenieria.id(), null);
        Usuario estudiante = usuario(Rol.ESTUDIANTE, null, null);
        GestionarVinculosCasoUso casoUso = new GestionarVinculosCasoUso(usuarios, estructura, vinculos, auditoria::add,
                transacciones, reloj);

        casoUso.vincular(coordinador.getId(), sistemas.id(), estudiante.getId());

        assertThat(vinculos.vinculos).contains(estudiante.getId() + "/" + sistemas.id());
        assertThatThrownBy(() -> casoUso.vincular(coordinador.getId(), industrial.id(), estudiante.getId()))
                .isInstanceOf(AccesoDenegadoExcepcion.class);
        assertThatThrownBy(() -> casoUso.vincular(coordinador.getId(), sistemas.id(), coordinadorFacultad.getId()))
                .isInstanceOf(ReglaNegocioExcepcion.class).extracting("codigo").isEqualTo("ROL_NO_VINCULABLE");
        assertThat(casoUso.miembros(coordinadorFacultad.getId(), sistemas.id())).isEmpty();
        assertThatThrownBy(() -> casoUso.miembros(estudiante.getId(), sistemas.id()))
                .isInstanceOf(AccesoDenegadoExcepcion.class);

        casoUso.desvincular(coordinador.getId(), sistemas.id(), estudiante.getId());
        assertThat(vinculos.vinculos).isEmpty();
        assertThatThrownBy(() -> casoUso.desvincular(coordinador.getId(), sistemas.id(), estudiante.getId()))
                .isInstanceOf(RecursoNoEncontradoExcepcion.class);
    }

    @Test
    void laConsultaDeAuditoriaValidaPaginacionYFechas() {
        List<FiltroAuditoria> filtrosRecibidos = new ArrayList<>();
        ConsultarAuditoriaCasoUso casoUso = new ConsultarAuditoriaCasoUso((filtro, pagina, tamano) -> {
            filtrosRecibidos.add(filtro);
            return new PaginaAuditoria(List.of(), pagina, tamano, 0);
        });

        PaginaAuditoria pagina = casoUso.consultar(null, null, null);

        assertThat(pagina.tamano()).isEqualTo(ConsultarAuditoriaCasoUso.TAMANO_POR_DEFECTO);
        assertThat(filtrosRecibidos).hasSize(1);
        assertThatThrownBy(() -> casoUso.consultar(null, -1, 10)).isInstanceOf(ParametroInvalidoExcepcion.class);
        assertThatThrownBy(() -> casoUso.consultar(null, 0, 500)).isInstanceOf(ParametroInvalidoExcepcion.class);
        FiltroAuditoria invertido = new FiltroAuditoria(AHORA, AHORA.minusSeconds(1), null, null, null, null, null);
        assertThatThrownBy(() -> casoUso.consultar(invertido, 0, 10)).isInstanceOf(ParametroInvalidoExcepcion.class);
    }

    @Test
    void laLimpiezaUsaLasRetencionesConfiguradas() {
        List<Instant> limites = new ArrayList<>();
        MantenimientoDatos mantenimiento = new MantenimientoDatos() {
            @Override
            public int eliminarTokensRefrescoVencidos(Instant antesDe) {
                limites.add(antesDe);
                return 2;
            }

            @Override
            public int eliminarNotificacionesFinalizadas(Instant antesDe) {
                limites.add(antesDe);
                return 5;
            }

            @Override
            public int eliminarCodigosVencidos(Instant antesDe) {
                limites.add(antesDe);
                return 1;
            }
        };

        ResultadoLimpieza resultado = new LimpiarDatosCasoUso(mantenimiento, reloj, Duration.ofDays(30),
                Duration.ofDays(1)).limpiar();

        assertThat(resultado).isEqualTo(new ResultadoLimpieza(2, 5, 1));
        assertThat(limites).containsExactly(AHORA.minus(Duration.ofDays(1)), AHORA.minus(Duration.ofDays(30)),
                AHORA.minus(Duration.ofDays(1)));
    }

    @Test
    void elEnlaceDeUnAdjuntoQuedaAuditadoYRequiereAlmacenamientoConEnlaces() {
        UUID kudoId = UUID.randomUUID();
        AdjuntoDatos adjunto = new AdjuntoDatos(UUID.randomUUID(), "evidencia.pdf", "application/pdf", 10,
                "kudos/1/a.pdf");
        ConsultasKudos consultas = new ConsultasKudos() {
            @Override
            public List<KudoResumenVista> feed(CursorFeed despuesDe, int limite) {
                return List.of();
            }

            @Override
            public Optional<KudoDetalleDatos> detalle(UUID id) {
                return Optional.empty();
            }

            @Override
            public Optional<AdjuntoDatos> adjunto(UUID kudo, UUID adjuntoId) {
                return kudo.equals(kudoId) && adjuntoId.equals(adjunto.id()) ? Optional.of(adjunto) : Optional.empty();
            }
        };
        Usuario docente = usuario(Rol.DOCENTE, null, null);
        GenerarEnlaceAdjuntoCasoUso conEnlaces = new GenerarEnlaceAdjuntoCasoUso(usuarios, consultas,
                (clave, vigencia) -> Optional.of("https://almacen/" + clave), auditoria::add, reloj,
                Duration.ofMinutes(10));

        EnlaceAdjunto enlace = conEnlaces.generar(kudoId, adjunto.id(), docente.getId());

        assertThat(enlace.url()).isEqualTo("https://almacen/kudos/1/a.pdf");
        assertThat(enlace.expiraEn()).isEqualTo(AHORA.plus(Duration.ofMinutes(10)));
        assertThat(auditoria).extracting(EntradaAuditoria::accion).containsExactly(AccionAuditoria.ADJUNTO_DESCARGADO);
        assertThatThrownBy(() -> conEnlaces.generar(kudoId, UUID.randomUUID(), docente.getId()))
                .isInstanceOf(RecursoNoEncontradoExcepcion.class);
        GenerarEnlaceAdjuntoCasoUso sinEnlaces = new GenerarEnlaceAdjuntoCasoUso(usuarios, consultas,
                (clave, vigencia) -> Optional.empty(), auditoria::add, reloj, Duration.ofMinutes(10));
        assertThatThrownBy(() -> sinEnlaces.generar(kudoId, adjunto.id(), docente.getId()))
                .isInstanceOf(ReglaNegocioExcepcion.class).extracting("codigo").isEqualTo("ENLACE_NO_DISPONIBLE");
    }

    private Usuario usuario(Rol rol, UUID facultadId, UUID programaId) {
        Usuario usuario = new Usuario(UUID.randomUUID(), rol.name(), UUID.randomUUID() + "@uco.edu.co", null, true,
                false, rol, EstadoUsuario.ACTIVO, null, 0, null, facultadId, programaId);
        usuarios.guardar(usuario);
        return usuario;
    }

    private static final class CategoriasEnMemoria implements RepositorioCategoriasAdministracion {
        private final Map<UUID, Categoria> datos = new LinkedHashMap<>();

        @Override
        public Optional<Categoria> buscarPorId(UUID id) {
            return Optional.ofNullable(datos.get(id));
        }

        @Override
        public boolean existeNombre(String nombre, UUID excluirId) {
            return datos.values().stream()
                    .anyMatch(c -> c.getNombre().equalsIgnoreCase(nombre) && !c.getId().equals(excluirId));
        }

        @Override
        public void guardar(Categoria categoria) {
            datos.put(categoria.getId(), categoria);
        }
    }
}
