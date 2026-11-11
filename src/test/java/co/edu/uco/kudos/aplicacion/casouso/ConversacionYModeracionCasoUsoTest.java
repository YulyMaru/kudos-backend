package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.dto.CalificarComando;
import co.edu.uco.kudos.aplicacion.dto.CursorFeed;
import co.edu.uco.kudos.aplicacion.dto.EstadoContenido;
import co.edu.uco.kudos.aplicacion.dto.ModerarComando;
import co.edu.uco.kudos.aplicacion.dto.PaginaReplicas;
import co.edu.uco.kudos.aplicacion.dto.PersonaVista;
import co.edu.uco.kudos.aplicacion.dto.PublicarReplicaComando;
import co.edu.uco.kudos.aplicacion.dto.ReplicaPublicadaResultado;
import co.edu.uco.kudos.aplicacion.dto.ReplicaVista;
import co.edu.uco.kudos.aplicacion.dto.ReporteVista;
import co.edu.uco.kudos.aplicacion.dto.ResumenCalificacion;
import co.edu.uco.kudos.aplicacion.puerto.salida.ConsultasConversacion;
import co.edu.uco.kudos.aplicacion.puerto.salida.ContenidosModerables;
import co.edu.uco.kudos.aplicacion.puerto.salida.GestorTransacciones;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioCalificaciones;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioModeracion;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioReplicas;
import co.edu.uco.kudos.dominio.excepcion.AccesoDenegadoExcepcion;
import co.edu.uco.kudos.dominio.excepcion.RecursoNoEncontradoExcepcion;
import co.edu.uco.kudos.dominio.excepcion.ReglaNegocioExcepcion;
import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;
import co.edu.uco.kudos.dominio.modelo.Calificacion;
import co.edu.uco.kudos.dominio.modelo.EntradaAuditoria;
import co.edu.uco.kudos.dominio.modelo.EstadoUsuario;
import co.edu.uco.kudos.dominio.modelo.Replica;
import co.edu.uco.kudos.dominio.modelo.Reporte;
import co.edu.uco.kudos.dominio.modelo.Rol;
import co.edu.uco.kudos.dominio.modelo.TipoContenido;
import co.edu.uco.kudos.dominio.modelo.Usuario;
import co.edu.uco.kudos.dominio.modelo.Veto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConversacionYModeracionCasoUsoTest {

    private static final Instant AHORA = Instant.parse("2026-11-11T10:00:00Z");

    private DoblesAutenticacion.CuentasEnMemoria usuarios;
    private ContenidosEnMemoria contenidos;
    private CalificacionesEnMemoria calificaciones;
    private ModeracionEnMemoria moderacion;
    private List<EntradaAuditoria> auditoria;
    private PublicarReplicaCasoUso publicarReplica;
    private CalificarContenidoCasoUso calificar;
    private ReportarContenidoCasoUso reportar;
    private VetarContenidoCasoUso vetar;
    private Usuario docente;
    private Usuario rector;
    private UUID kudoId;

    @BeforeEach
    void preparar() {
        usuarios = new DoblesAutenticacion.CuentasEnMemoria();
        contenidos = new ContenidosEnMemoria();
        calificaciones = new CalificacionesEnMemoria();
        moderacion = new ModeracionEnMemoria();
        auditoria = new ArrayList<>();
        DoblesAutenticacion.RelojAjustable reloj = new DoblesAutenticacion.RelojAjustable(AHORA);
        GestorTransacciones transacciones = Runnable::run;
        publicarReplica = new PublicarReplicaCasoUso(usuarios, contenidos, contenidos, auditoria::add, transacciones,
                reloj);
        calificar = new CalificarContenidoCasoUso(usuarios, contenidos, calificaciones, reloj);
        reportar = new ReportarContenidoCasoUso(usuarios, contenidos, moderacion, auditoria::add, transacciones, reloj);
        vetar = new VetarContenidoCasoUso(usuarios, contenidos, moderacion, auditoria::add, transacciones, reloj);
        docente = usuario(Rol.DOCENTE);
        rector = usuario(Rol.RECTOR);
        kudoId = UUID.randomUUID();
        contenidos.estados.put(kudoId, new EstadoContenido(TipoContenido.KUDO, kudoId, kudoId, docente.getId(), false));
    }

    @Test
    void sePuedeReplicarAUnKudoYAOtraReplicaSinLimiteDeProfundidad() {
        ReplicaPublicadaResultado primera = publicarReplica.publicar(
                new PublicarReplicaComando(docente.getId(), kudoId, null, "¡Felicitaciones!"));
        ReplicaPublicadaResultado segunda = publicarReplica.publicar(
                new PublicarReplicaComando(rector.getId(), kudoId, primera.id(), "Totalmente de acuerdo"));
        ReplicaPublicadaResultado tercera = publicarReplica.publicar(
                new PublicarReplicaComando(docente.getId(), kudoId, segunda.id(), "Gracias"));

        assertThat(contenidos.replicas).extracting(Replica::replicaPadreId)
                .containsExactly(null, primera.id(), segunda.id());
        assertThat(tercera.kudoId()).isEqualTo(kudoId);
        assertThat(auditoria).extracting(EntradaAuditoria::accion).containsOnly(AccionAuditoria.REPLICA_PUBLICADA);
    }

    @Test
    void noSePuedeResponderAUnaReplicaDeOtroKudoNiAContenidoVetado() {
        UUID otroKudo = UUID.randomUUID();
        contenidos.estados.put(otroKudo, new EstadoContenido(TipoContenido.KUDO, otroKudo, otroKudo, docente.getId(), false));
        ReplicaPublicadaResultado ajena = publicarReplica.publicar(
                new PublicarReplicaComando(docente.getId(), otroKudo, null, "Hola"));

        assertThatThrownBy(() -> publicarReplica.publicar(
                new PublicarReplicaComando(docente.getId(), kudoId, ajena.id(), "Respuesta")))
                .isInstanceOf(RecursoNoEncontradoExcepcion.class);

        contenidos.marcarVetado(TipoContenido.KUDO, kudoId);
        assertThatThrownBy(() -> publicarReplica.publicar(
                new PublicarReplicaComando(docente.getId(), kudoId, null, "Hola")))
                .isInstanceOf(RecursoNoEncontradoExcepcion.class);
    }

    @Test
    void calificarDeNuevoReemplazaLaCalificacionAnterior() {
        calificar.calificar(new CalificarComando(docente.getId(), TipoContenido.KUDO, kudoId, 3));
        ResumenCalificacion resumen = calificar.calificar(new CalificarComando(docente.getId(), TipoContenido.KUDO,
                kudoId, 5));
        ResumenCalificacion conOtro = calificar.calificar(new CalificarComando(rector.getId(), TipoContenido.KUDO,
                kudoId, 4));

        assertThat(resumen.cantidad()).isEqualTo(1);
        assertThat(resumen.miCalificacion()).isEqualTo(5);
        assertThat(conOtro.cantidad()).isEqualTo(2);
        assertThat(conOtro.promedio()).isEqualTo(4.5);
    }

    @Test
    void unaCalificacionFueraDeRangoSeRechaza() {
        assertThatThrownBy(() -> calificar.calificar(new CalificarComando(docente.getId(), TipoContenido.KUDO, kudoId, 6)))
                .isInstanceOf(ReglaNegocioExcepcion.class).extracting("codigo").isEqualTo("CALIFICACION_FUERA_DE_RANGO");
        assertThatThrownBy(() -> calificar.calificar(new CalificarComando(docente.getId(), TipoContenido.KUDO, kudoId, 0)))
                .isInstanceOf(ReglaNegocioExcepcion.class);
    }

    @Test
    void unUsuarioNoPuedeReportarDosVecesElMismoContenido() {
        reportar.reportar(new ModerarComando(docente.getId(), TipoContenido.KUDO, kudoId, "Lenguaje ofensivo"));

        assertThatThrownBy(() -> reportar.reportar(new ModerarComando(docente.getId(), TipoContenido.KUDO, kudoId,
                "Otra vez"))).isInstanceOf(ReglaNegocioExcepcion.class).extracting("codigo").isEqualTo("YA_REPORTADO");
        assertThat(moderacion.reportes).hasSize(1);
    }

    @Test
    void soloElRectorYElCoordinadorAcademicoPuedenVetar() {
        Usuario administrador = usuario(Rol.ADMINISTRADOR);

        assertThatThrownBy(() -> vetar.vetar(new ModerarComando(docente.getId(), TipoContenido.KUDO, kudoId, "x")))
                .isInstanceOf(AccesoDenegadoExcepcion.class);
        assertThatThrownBy(() -> vetar.vetar(new ModerarComando(administrador.getId(), TipoContenido.KUDO, kudoId, "x")))
                .isInstanceOf(AccesoDenegadoExcepcion.class);
        assertThat(contenidos.estados.get(kudoId).vetado()).isFalse();
    }

    @Test
    void vetarOcultaElContenidoSinBorrarloYAtiendeLosReportes() {
        reportar.reportar(new ModerarComando(docente.getId(), TipoContenido.KUDO, kudoId, "Inapropiado"));

        vetar.vetar(new ModerarComando(rector.getId(), TipoContenido.KUDO, kudoId, "No respeta los valores"));

        assertThat(contenidos.estados.get(kudoId).vetado()).isTrue();
        assertThat(moderacion.vetos).singleElement().satisfies(veto -> assertThat(veto.moderadorId())
                .isEqualTo(rector.getId()));
        assertThat(moderacion.reportesAtendidos).contains(kudoId);
        assertThat(auditoria).extracting(EntradaAuditoria::accion).contains(AccionAuditoria.CONTENIDO_VETADO);
        assertThatThrownBy(() -> vetar.vetar(new ModerarComando(rector.getId(), TipoContenido.KUDO, kudoId, "Otra vez")))
                .isInstanceOf(ReglaNegocioExcepcion.class).extracting("codigo").isEqualTo("YA_VETADO");
    }

    @Test
    void lasReplicasSePaginanPorNivelYSoloLasVenLosModeradoresSiHayReportes() {
        PersonaVista autor = new PersonaVista(docente.getId(), "Docente", Rol.DOCENTE);
        List<ReplicaVista> todas = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            todas.add(new ReplicaVista(UUID.randomUUID(), kudoId, null, autor, "r" + i, AHORA.plusSeconds(i), 0, 0, 0));
        }
        ConsultasConversacion consultas = new ConsultasConversacion() {
            @Override
            public List<ReplicaVista> replicas(UUID kudo, UUID padre, CursorFeed despuesDe, int limite) {
                return todas.stream()
                        .filter(r -> despuesDe == null || r.fechaPublicacion().isAfter(despuesDe.fechaPublicacion()))
                        .limit(limite).toList();
            }

            @Override
            public List<ReporteVista> reportesPendientes(int limite) {
                return List.of();
            }
        };
        ConsultarReplicasCasoUso consultar = new ConsultarReplicasCasoUso(consultas, contenidos);

        PaginaReplicas primera = consultar.consultar(kudoId, null, null, 2);
        PaginaReplicas segunda = consultar.consultar(kudoId, null, primera.siguienteCursor(), 2);

        assertThat(primera.replicas()).extracting(ReplicaVista::contenido).containsExactly("r0", "r1");
        assertThat(segunda.replicas()).extracting(ReplicaVista::contenido).containsExactly("r2");
        assertThat(segunda.siguienteCursor()).isNull();

        ConsultarReportesCasoUso reportes = new ConsultarReportesCasoUso(usuarios, consultas);
        assertThat(reportes.pendientes(rector.getId())).isEmpty();
        assertThatThrownBy(() -> reportes.pendientes(docente.getId())).isInstanceOf(AccesoDenegadoExcepcion.class);
    }

    private Usuario usuario(Rol rol) {
        Usuario usuario = new Usuario(UUID.randomUUID(), rol.name(), rol.name().toLowerCase() + "@uco.edu.co", null,
                false, rol, EstadoUsuario.ACTIVO);
        usuarios.guardar(usuario);
        return usuario;
    }

    private static final class ContenidosEnMemoria implements ContenidosModerables, RepositorioReplicas {
        private final Map<UUID, EstadoContenido> estados = new LinkedHashMap<>();
        private final List<Replica> replicas = new ArrayList<>();

        @Override
        public Optional<EstadoContenido> buscar(TipoContenido tipo, UUID id) {
            return Optional.ofNullable(estados.get(id)).filter(estado -> estado.tipo() == tipo);
        }

        @Override
        public void marcarVetado(TipoContenido tipo, UUID id) {
            EstadoContenido actual = estados.get(id);
            estados.put(id, new EstadoContenido(actual.tipo(), actual.id(), actual.kudoId(), actual.autorId(), true));
        }

        @Override
        public void guardar(Replica replica) {
            replicas.add(replica);
            estados.put(replica.id(), new EstadoContenido(TipoContenido.REPLICA, replica.id(), replica.kudoId(),
                    replica.autorId(), false));
        }
    }

    private static final class CalificacionesEnMemoria implements RepositorioCalificaciones {
        private final Map<String, Calificacion> datos = new HashMap<>();

        @Override
        public void guardarOReemplazar(Calificacion calificacion) {
            datos.put(calificacion.usuarioId() + "|" + calificacion.tipoContenido() + "|" + calificacion.contenidoId(),
                    calificacion);
        }

        @Override
        public ResumenCalificacion resumen(TipoContenido tipo, UUID contenidoId, UUID usuarioId) {
            List<Calificacion> delContenido = datos.values().stream()
                    .filter(c -> c.tipoContenido() == tipo && c.contenidoId().equals(contenidoId)).toList();
            double promedio = delContenido.stream().mapToInt(Calificacion::valor).average().orElse(0);
            Integer mia = delContenido.stream().filter(c -> c.usuarioId().equals(usuarioId))
                    .map(Calificacion::valor).findFirst().orElse(null);
            return new ResumenCalificacion(tipo, contenidoId, promedio, delContenido.size(), mia);
        }
    }

    private static final class ModeracionEnMemoria implements RepositorioModeracion {
        private final List<Reporte> reportes = new ArrayList<>();
        private final List<Veto> vetos = new ArrayList<>();
        private final List<UUID> reportesAtendidos = new ArrayList<>();

        @Override
        public boolean existeReporte(TipoContenido tipo, UUID contenidoId, UUID reportanteId) {
            return reportes.stream().anyMatch(r -> r.tipoContenido() == tipo && r.contenidoId().equals(contenidoId)
                    && r.reportanteId().equals(reportanteId));
        }

        @Override
        public void guardarReporte(Reporte reporte) {
            reportes.add(reporte);
        }

        @Override
        public void guardarVeto(Veto veto) {
            vetos.add(veto);
        }

        @Override
        public void marcarReportesAtendidos(TipoContenido tipo, UUID contenidoId, Instant fecha) {
            reportesAtendidos.add(contenidoId);
        }
    }
}
