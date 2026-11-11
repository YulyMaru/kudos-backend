package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.dto.IniciarSesionComando;
import co.edu.uco.kudos.aplicacion.dto.SesionResultado;
import co.edu.uco.kudos.aplicacion.puerto.salida.GestorTransacciones;
import co.edu.uco.kudos.dominio.excepcion.CampoObligatorioExcepcion;
import co.edu.uco.kudos.dominio.excepcion.CredencialesInvalidasExcepcion;
import co.edu.uco.kudos.dominio.excepcion.CuentaBloqueadaExcepcion;
import co.edu.uco.kudos.dominio.excepcion.CuentaNoActivaExcepcion;
import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;
import co.edu.uco.kudos.dominio.modelo.EntradaAuditoria;
import co.edu.uco.kudos.dominio.modelo.EstadoUsuario;
import co.edu.uco.kudos.dominio.modelo.ResultadoAuditoria;
import co.edu.uco.kudos.dominio.modelo.Rol;
import co.edu.uco.kudos.dominio.modelo.TokenRefresco;
import co.edu.uco.kudos.dominio.modelo.Usuario;
import co.edu.uco.kudos.dominio.regla.PoliticaBloqueo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IniciarSesionCasoUsoTest {

    private static final Instant AHORA = Instant.parse("2026-11-11T10:00:00Z");
    private static final String CONTRASENA = "Clave-Segura-2026";

    private DoblesAutenticacion.CuentasEnMemoria cuentas;
    private DoblesAutenticacion.TokensEnMemoria tokens;
    private DoblesAutenticacion.SolicitudesEnMemoria solicitudes;
    private DoblesAutenticacion.CodificadorFalso codificador;
    private DoblesAutenticacion.RelojAjustable reloj;
    private List<EntradaAuditoria> auditoria;
    private IniciarSesionCasoUso casoUso;
    private Usuario ana;

    @BeforeEach
    void preparar() {
        cuentas = new DoblesAutenticacion.CuentasEnMemoria();
        tokens = new DoblesAutenticacion.TokensEnMemoria();
        solicitudes = new DoblesAutenticacion.SolicitudesEnMemoria();
        codificador = new DoblesAutenticacion.CodificadorFalso();
        reloj = new DoblesAutenticacion.RelojAjustable(AHORA);
        auditoria = new ArrayList<>();
        ana = usuario("ana@uco.net.co", EstadoUsuario.ACTIVO);
        cuentas.guardar(ana);
        GestorTransacciones transacciones = Runnable::run;
        casoUso = new IniciarSesionCasoUso(cuentas, solicitudes, tokens, codificador, new DoblesAutenticacion.EmisorFalso(),
                auditoria::add, transacciones, reloj, new PoliticaBloqueo(5, Duration.ofMinutes(15)),
                Duration.ofDays(7));
    }

    @Test
    void iniciaSesionYGuardaSoloElHashDelTokenDeRefresco() {
        SesionResultado sesion = casoUso.iniciarSesion(new IniciarSesionComando(" ANA@uco.net.co ", CONTRASENA));

        assertThat(sesion.tokenAcceso()).isEqualTo("jwt-" + ana.getId());
        assertThat(sesion.expiraAcceso()).isEqualTo(AHORA.plus(Duration.ofMinutes(15)));
        assertThat(sesion.usuarioId()).isEqualTo(ana.getId());
        assertThat(sesion.rol()).isEqualTo(Rol.DOCENTE);
        assertThat(sesion.tokenRefresco()).hasSizeGreaterThanOrEqualTo(43);
        assertThat(sesion.expiraRefresco()).isEqualTo(AHORA.plus(Duration.ofDays(7)));

        TokenRefresco guardado = tokens.tokens.values().iterator().next();
        assertThat(guardado.hashToken()).isEqualTo(TokensSeguros.sha256(sesion.tokenRefresco()));
        assertThat(guardado.hashToken()).isNotEqualTo(sesion.tokenRefresco());
        assertThat(auditoria).singleElement().satisfies(entrada -> {
            assertThat(entrada.accion()).isEqualTo(AccionAuditoria.INICIO_SESION);
            assertThat(entrada.resultado()).isEqualTo(ResultadoAuditoria.EXITOSO);
            assertThat(entrada.actorId()).isEqualTo(ana.getId());
        });
    }

    @Test
    void cadaInicioDeSesionEntregaUnTokenDeRefrescoDistinto() {
        SesionResultado primera = casoUso.iniciarSesion(new IniciarSesionComando("ana@uco.net.co", CONTRASENA));
        SesionResultado segunda = casoUso.iniciarSesion(new IniciarSesionComando("ana@uco.net.co", CONTRASENA));

        assertThat(primera.tokenRefresco()).isNotEqualTo(segunda.tokenRefresco());
        assertThat(tokens.tokens).hasSize(2);
    }

    @Test
    void responderLoMismoSiElCorreoNoExisteOSiLaContrasenaEsIncorrecta() {
        int comparacionesIniciales = codificador.comparaciones;

        assertThatThrownBy(() -> casoUso.iniciarSesion(new IniciarSesionComando("nadie@uco.net.co", CONTRASENA)))
                .isInstanceOf(CredencialesInvalidasExcepcion.class)
                .hasMessage("Correo o contraseña incorrectos");
        assertThatThrownBy(() -> casoUso.iniciarSesion(new IniciarSesionComando("ana@uco.net.co", "otra-clave-123")))
                .isInstanceOf(CredencialesInvalidasExcepcion.class)
                .hasMessage("Correo o contraseña incorrectos");

        assertThat(codificador.comparaciones - comparacionesIniciales).isEqualTo(2);
        assertThat(auditoria).extracting(EntradaAuditoria::accion)
                .containsOnly(AccionAuditoria.INICIO_SESION_FALLIDO);
        assertThat(tokens.tokens).isEmpty();
    }

    @Test
    void registraElIntentoFallidoEnLaCuenta() {
        intentarConClaveIncorrecta();

        assertThat(cuentas.usuarios.get(ana.getId()).getIntentosFallidos()).isEqualTo(1);
        assertThat(auditoria).last().satisfies(entrada -> assertThat(entrada.detalle()).contains("intentosFallidos=1"));
    }

    @Test
    void bloqueaLaCuentaAlQuintoIntentoFallidoYLoAudita() {
        for (int i = 0; i < 4; i++) {
            intentarConClaveIncorrecta();
        }

        assertThatThrownBy(() -> casoUso.iniciarSesion(new IniciarSesionComando("ana@uco.net.co", "incorrecta-123")))
                .isInstanceOf(CuentaBloqueadaExcepcion.class)
                .extracting("bloqueadaHasta").isEqualTo(AHORA.plus(Duration.ofMinutes(15)));
        assertThat(auditoria).last().satisfies(entrada -> assertThat(entrada.detalle()).contains("bloqueada=true"));
    }

    @Test
    void unaCuentaBloqueadaNoEntraNiConLaContrasenaCorrecta() {
        for (int i = 0; i < 5; i++) {
            intentarConClaveIncorrecta();
        }

        assertThatThrownBy(() -> casoUso.iniciarSesion(new IniciarSesionComando("ana@uco.net.co", CONTRASENA)))
                .isInstanceOf(CuentaBloqueadaExcepcion.class);
        assertThat(tokens.tokens).isEmpty();
    }

    @Test
    void trasVencerElBloqueoPuedeEntrarYSeLimpianLosIntentos() {
        for (int i = 0; i < 5; i++) {
            intentarConClaveIncorrecta();
        }
        reloj.avanzar(Duration.ofMinutes(16));

        SesionResultado sesion = casoUso.iniciarSesion(new IniciarSesionComando("ana@uco.net.co", CONTRASENA));

        assertThat(sesion.usuarioId()).isEqualTo(ana.getId());
        Usuario guardado = cuentas.usuarios.get(ana.getId());
        assertThat(guardado.getBloqueadoHasta()).isNull();
        assertThat(guardado.getIntentosFallidos()).isZero();
    }

    @Test
    void unaCuentaNoActivaSoloSeRevelaConLaContrasenaCorrecta() {
        Usuario pendiente = usuario("pendiente@uco.net.co", EstadoUsuario.PENDIENTE);
        cuentas.guardar(pendiente);

        assertThatThrownBy(() -> casoUso.iniciarSesion(new IniciarSesionComando("pendiente@uco.net.co", "mala-clave-12")))
                .isInstanceOf(CredencialesInvalidasExcepcion.class);
        assertThatThrownBy(() -> casoUso.iniciarSesion(new IniciarSesionComando("pendiente@uco.net.co", CONTRASENA)))
                .isInstanceOf(CuentaNoActivaExcepcion.class)
                .extracting("estado").isEqualTo(EstadoUsuario.PENDIENTE);
    }

    @Test
    void exigeCorreoYContrasena() {
        assertThatThrownBy(() -> casoUso.iniciarSesion(new IniciarSesionComando(" ", CONTRASENA)))
                .isInstanceOf(CampoObligatorioExcepcion.class).extracting("campo").isEqualTo("correo");
        assertThatThrownBy(() -> casoUso.iniciarSesion(new IniciarSesionComando("ana@uco.net.co", "")))
                .isInstanceOf(CampoObligatorioExcepcion.class).extracting("campo").isEqualTo("contrasena");
    }

    @Test
    void elComandoNoMuestraLaContrasenaEnSuRepresentacion() {
        assertThat(new IniciarSesionComando("ana@uco.net.co", CONTRASENA).toString()).doesNotContain(CONTRASENA);
    }

    private void intentarConClaveIncorrecta() {
        try {
            casoUso.iniciarSesion(new IniciarSesionComando("ana@uco.net.co", "incorrecta-123"));
        } catch (CredencialesInvalidasExcepcion | CuentaBloqueadaExcepcion esperado) {
            assertThat(esperado).isNotNull();
        }
    }

    private static Usuario usuario(String correo, EstadoUsuario estado) {
        return new Usuario(UUID.randomUUID(), "Ana Docente", correo, null, false, Rol.DOCENTE, estado,
                "hash:" + CONTRASENA, 0, null);
    }
}
