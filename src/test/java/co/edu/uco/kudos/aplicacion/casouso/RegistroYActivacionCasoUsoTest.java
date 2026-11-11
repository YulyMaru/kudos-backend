package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.dto.RegistrarUsuarioComando;
import co.edu.uco.kudos.aplicacion.dto.SolicitudPendienteVista;
import co.edu.uco.kudos.aplicacion.puerto.salida.GestorTransacciones;
import co.edu.uco.kudos.dominio.excepcion.AccesoDenegadoExcepcion;
import co.edu.uco.kudos.dominio.excepcion.ReglaNegocioExcepcion;
import co.edu.uco.kudos.dominio.modelo.Canal;
import co.edu.uco.kudos.dominio.modelo.EntradaAuditoria;
import co.edu.uco.kudos.dominio.modelo.EstadoAprobacion;
import co.edu.uco.kudos.dominio.modelo.EstadoUsuario;
import co.edu.uco.kudos.dominio.modelo.Facultad;
import co.edu.uco.kudos.dominio.modelo.Notificacion;
import co.edu.uco.kudos.dominio.modelo.Programa;
import co.edu.uco.kudos.dominio.modelo.PropositoCodigo;
import co.edu.uco.kudos.dominio.modelo.Rol;
import co.edu.uco.kudos.dominio.modelo.SolicitudRegistro;
import co.edu.uco.kudos.dominio.modelo.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistroYActivacionCasoUsoTest {

    private static final Instant AHORA = Instant.parse("2026-11-11T10:00:00Z");
    private static final String CLAVE = "Clave-Segura-2026";

    private DoblesAutenticacion.CuentasEnMemoria cuentas;
    private DoblesAutenticacion.SolicitudesEnMemoria solicitudes;
    private DoblesAutenticacion.CodigosEnMemoria codigos;
    private DoblesAutenticacion.OutboxEnMemoria outbox;
    private DoblesAutenticacion.VinculosEnMemoria vinculos;
    private DoblesAutenticacion.EstructuraEnMemoria estructura;
    private DoblesAutenticacion.RelojAjustable reloj;
    private List<EntradaAuditoria> auditoria;
    private RegistrarUsuarioCasoUso registrar;
    private VerificarContactoCasoUso verificar;
    private DecidirSolicitudCasoUso decidir;
    private Programa sistemas;
    private Programa derecho;
    private Usuario coordinadorSistemas;

    @BeforeEach
    void preparar() {
        cuentas = new DoblesAutenticacion.CuentasEnMemoria();
        solicitudes = new DoblesAutenticacion.SolicitudesEnMemoria();
        codigos = new DoblesAutenticacion.CodigosEnMemoria();
        outbox = new DoblesAutenticacion.OutboxEnMemoria();
        vinculos = new DoblesAutenticacion.VinculosEnMemoria();
        estructura = new DoblesAutenticacion.EstructuraEnMemoria();
        reloj = new DoblesAutenticacion.RelojAjustable(AHORA);
        auditoria = new ArrayList<>();
        Facultad ingenieria = Facultad.crear("Ingeniería");
        Facultad cienciasSociales = Facultad.crear("Ciencias Sociales");
        estructura.guardarFacultad(ingenieria);
        estructura.guardarFacultad(cienciasSociales);
        sistemas = Programa.crear(ingenieria.id(), "Ingeniería de Sistemas");
        derecho = Programa.crear(cienciasSociales.id(), "Derecho");
        estructura.guardarPrograma(sistemas);
        estructura.guardarPrograma(derecho);
        coordinadorSistemas = activo("coord.sistemas@uco.edu.co", Rol.COORDINADOR_PROGRAMA, null, sistemas.id());
        GestorTransacciones transacciones = Runnable::run;
        DoblesAutenticacion.CodificadorFalso codificador = new DoblesAutenticacion.CodificadorFalso();
        registrar = new RegistrarUsuarioCasoUso(cuentas, solicitudes, codigos, estructura, outbox, codificador,
                auditoria::add, transacciones, reloj, List.of("uco.edu.co"), Duration.ofMinutes(15));
        verificar = new VerificarContactoCasoUso(cuentas, codigos, solicitudes, vinculos, outbox, auditoria::add,
                transacciones, reloj, Duration.ofMinutes(15));
        decidir = new DecidirSolicitudCasoUso(solicitudes, cuentas, cuentas, estructura, vinculos, outbox,
                auditoria::add, transacciones, reloj);
    }

    @Test
    void elRegistroCreaUnaSolicitudPendienteYEnviaLosDosCodigos() {
        registrar.registrar(comando("ana.estudiante@uco.edu.co", "+573001112233", Rol.ESTUDIANTE, sistemas.id()));

        Usuario ana = cuentas.buscarPorCorreo("ana.estudiante@uco.edu.co").orElseThrow();
        assertThat(ana.getEstado()).isEqualTo(EstadoUsuario.PENDIENTE);
        assertThat(ana.isCorreoVerificado()).isFalse();
        assertThat(ana.isCelularVerificado()).isFalse();
        assertThat(solicitudes.buscarPorUsuario(ana.getId())).hasValueSatisfying(s ->
                assertThat(s.estado()).isEqualTo(EstadoAprobacion.PENDIENTE));
        assertThat(outbox.notificaciones).extracting(Notificacion::getCanal)
                .containsExactlyInAnyOrder(Canal.EMAIL, Canal.WHATSAPP);
        assertThat(codigos.codigos.values()).hasSize(2)
                .allSatisfy(codigo -> assertThat(codigo.hashCodigo()).hasSize(64));
    }

    @Test
    void laCuentaSeActivaCuandoSeCumplenLasTresCondicionesEnCualquierOrden() {
        registrar.registrar(comando("ana.estudiante@uco.edu.co", "+573001112233", Rol.ESTUDIANTE, sistemas.id()));
        Usuario ana = cuentas.buscarPorCorreo("ana.estudiante@uco.edu.co").orElseThrow();
        SolicitudRegistro solicitud = solicitudes.buscarPorUsuario(ana.getId()).orElseThrow();

        decidir.aprobar(coordinadorSistemas.getId(), solicitud.id());
        verificar.verificar("ana.estudiante@uco.edu.co", PropositoCodigo.VERIFICAR_CORREO, codigoEnviado(Canal.EMAIL));
        assertThat(cuentas.buscarPorId(ana.getId()).orElseThrow().estaActivo()).isFalse();

        verificar.verificar("ana.estudiante@uco.edu.co", PropositoCodigo.VERIFICAR_CELULAR,
                codigoEnviado(Canal.WHATSAPP));

        Usuario activada = cuentas.buscarPorId(ana.getId()).orElseThrow();
        assertThat(activada.estaActivo()).isTrue();
        assertThat(vinculos.vinculos).contains(ana.getId() + "/" + sistemas.id());
        assertThat(outbox.notificaciones).anySatisfy(n ->
                assertThat(n.getDatos()).containsEntry("plantilla", "CUENTA_ACTIVADA"));
    }

    @Test
    void rechazaCorreosNoInstitucionalesDuplicadosYSinAceptarLaPolitica() {
        assertThatThrownBy(() -> registrar.registrar(comando("ana@gmail.com", "+573001112233", Rol.ESTUDIANTE,
                sistemas.id()))).isInstanceOf(ReglaNegocioExcepcion.class)
                .extracting("codigo").isEqualTo("CORREO_NO_INSTITUCIONAL");

        registrar.registrar(comando("ana@uco.edu.co", "+573001112233", Rol.ESTUDIANTE, sistemas.id()));
        assertThatThrownBy(() -> registrar.registrar(comando("otra@uco.edu.co", "+573001112233", Rol.DOCENTE,
                sistemas.id()))).isInstanceOf(ReglaNegocioExcepcion.class)
                .extracting("codigo").isEqualTo("CUENTA_EXISTENTE");

        RegistrarUsuarioComando sinPolitica = new RegistrarUsuarioComando("Luis", "luis@uco.edu.co", "+573009998877",
                Rol.ESTUDIANTE, null, sistemas.id(), CLAVE, false);
        assertThatThrownBy(() -> registrar.registrar(sinPolitica)).isInstanceOf(ReglaNegocioExcepcion.class)
                .extracting("codigo").isEqualTo("POLITICA_NO_ACEPTADA");
    }

    @Test
    void unCodigoIncorrectoSumaIntentosYAlQuintoBloqueaLaVerificacion() {
        registrar.registrar(comando("ana@uco.edu.co", "+573001112233", Rol.ESTUDIANTE, sistemas.id()));

        for (int intento = 1; intento < 5; intento++) {
            assertThatThrownBy(() -> verificar.verificar("ana@uco.edu.co", PropositoCodigo.VERIFICAR_CORREO, "000000x"))
                    .isInstanceOf(ReglaNegocioExcepcion.class).extracting("codigo").isEqualTo("CODIGO_INVALIDO");
        }
        assertThatThrownBy(() -> verificar.verificar("ana@uco.edu.co", PropositoCodigo.VERIFICAR_CORREO, "000000x"))
                .isInstanceOf(ReglaNegocioExcepcion.class).extracting("codigo").isEqualTo("VERIFICACION_BLOQUEADA");
        String correcto = codigoEnviado(Canal.EMAIL);
        assertThatThrownBy(() -> verificar.verificar("ana@uco.edu.co", PropositoCodigo.VERIFICAR_CORREO, correcto))
                .isInstanceOf(ReglaNegocioExcepcion.class).extracting("codigo").isEqualTo("CODIGO_EXPIRADO");
    }

    @Test
    void elReenvioDeCodigosTieneUnLimite() {
        registrar.registrar(comando("ana@uco.edu.co", "+573001112233", Rol.ESTUDIANTE, sistemas.id()));

        verificar.reenviar("ana@uco.edu.co", PropositoCodigo.VERIFICAR_CELULAR);
        verificar.reenviar("ana@uco.edu.co", PropositoCodigo.VERIFICAR_CELULAR);

        assertThatThrownBy(() -> verificar.reenviar("ana@uco.edu.co", PropositoCodigo.VERIFICAR_CELULAR))
                .isInstanceOf(ReglaNegocioExcepcion.class).extracting("codigo").isEqualTo("REENVIOS_EXCEDIDOS");
        reloj.avanzar(Duration.ofMinutes(16));
        verificar.reenviar("ana@uco.edu.co", PropositoCodigo.VERIFICAR_CELULAR);
    }

    @Test
    void unCoordinadorSoloVeYApruebaSolicitudesDeSuPrograma() {
        registrar.registrar(comando("ana@uco.edu.co", "+573001112233", Rol.ESTUDIANTE, sistemas.id()));
        registrar.registrar(comando("pedro@uco.edu.co", "+573004445566", Rol.ESTUDIANTE, derecho.id()));
        Usuario pedro = cuentas.buscarPorCorreo("pedro@uco.edu.co").orElseThrow();
        UUID solicitudPedro = solicitudes.buscarPorUsuario(pedro.getId()).orElseThrow().id();

        List<SolicitudPendienteVista> pendientes = decidir.pendientes(coordinadorSistemas.getId());

        assertThat(pendientes).hasSize(1);
        assertThatThrownBy(() -> decidir.aprobar(coordinadorSistemas.getId(), solicitudPedro))
                .isInstanceOf(AccesoDenegadoExcepcion.class);
    }

    @Test
    void unCoordinadorDeProgramaNoApruebaAUnCoordinadorDeFacultad() {
        registrar.registrar(new RegistrarUsuarioComando("Coord Facultad", "coord.fac@uco.edu.co", "+573007778899",
                Rol.COORDINADOR_FACULTAD, sistemas.facultadId(), null, CLAVE, true));
        UUID solicitud = solicitudes.solicitudes.values().iterator().next().id();

        assertThatThrownBy(() -> decidir.aprobar(coordinadorSistemas.getId(), solicitud))
                .isInstanceOf(AccesoDenegadoExcepcion.class);
    }

    @Test
    void alActivarseUnNuevoRectorElAnteriorQuedaInactivo() {
        Usuario administrador = activo("admin@uco.edu.co", Rol.ADMINISTRADOR, null, null);
        Usuario rectorAnterior = activo("rector.anterior@uco.edu.co", Rol.RECTOR, null, null);
        registrar.registrar(new RegistrarUsuarioComando("Nueva Rectora", "rectora@uco.edu.co", "+573001231234",
                Rol.RECTOR, null, null, CLAVE, true));
        Usuario rectora = cuentas.buscarPorCorreo("rectora@uco.edu.co").orElseThrow();
        verificar.verificar("rectora@uco.edu.co", PropositoCodigo.VERIFICAR_CORREO, codigoEnviado(Canal.EMAIL));
        verificar.verificar("rectora@uco.edu.co", PropositoCodigo.VERIFICAR_CELULAR, codigoEnviado(Canal.WHATSAPP));

        decidir.aprobar(administrador.getId(), solicitudes.buscarPorUsuario(rectora.getId()).orElseThrow().id());

        assertThat(cuentas.buscarPorId(rectora.getId()).orElseThrow().estaActivo()).isTrue();
        assertThat(cuentas.buscarPorId(rectorAnterior.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoUsuario.INACTIVO);
    }

    @Test
    void elRechazoGuardaElMotivoYNotificaAlSolicitante() {
        registrar.registrar(comando("ana@uco.edu.co", "+573001112233", Rol.ESTUDIANTE, sistemas.id()));
        Usuario ana = cuentas.buscarPorCorreo("ana@uco.edu.co").orElseThrow();
        UUID solicitud = solicitudes.buscarPorUsuario(ana.getId()).orElseThrow().id();

        decidir.rechazar(coordinadorSistemas.getId(), solicitud, "No pertenece al programa");

        assertThat(solicitudes.buscarPorId(solicitud)).hasValueSatisfying(s -> {
            assertThat(s.estado()).isEqualTo(EstadoAprobacion.RECHAZADA);
            assertThat(s.motivoRechazo()).isEqualTo("No pertenece al programa");
        });
        assertThat(outbox.notificaciones).anySatisfy(n ->
                assertThat(n.getDatos()).containsEntry("plantilla", "SOLICITUD_RECHAZADA"));
        assertThatThrownBy(() -> decidir.aprobar(coordinadorSistemas.getId(), solicitud))
                .isInstanceOf(ReglaNegocioExcepcion.class).extracting("codigo").isEqualTo("SOLICITUD_YA_DECIDIDA");
    }

    private String codigoEnviado(Canal canal) {
        return outbox.notificaciones.stream()
                .filter(n -> n.getCanal() == canal && "CODIGO_VERIFICACION".equals(n.getDatos().get("plantilla")))
                .reduce((primera, segunda) -> segunda)
                .orElseThrow()
                .getDatos().get("codigo");
    }

    private Usuario activo(String correo, Rol rol, UUID facultadId, UUID programaId) {
        Usuario usuario = new Usuario(UUID.randomUUID(), correo, correo, null, true, false, rol, EstadoUsuario.ACTIVO,
                "hash:" + CLAVE, 0, null, facultadId, programaId);
        cuentas.guardar(usuario);
        return usuario;
    }

    private static RegistrarUsuarioComando comando(String correo, String celular, Rol rol, UUID programaId) {
        return new RegistrarUsuarioComando("Persona UCO", correo, celular, rol, null, programaId, CLAVE, true);
    }
}
