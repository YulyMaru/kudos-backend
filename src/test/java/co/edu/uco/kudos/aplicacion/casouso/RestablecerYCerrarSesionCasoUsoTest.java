package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.puerto.salida.GestorTransacciones;
import co.edu.uco.kudos.dominio.excepcion.ReglaNegocioExcepcion;
import co.edu.uco.kudos.dominio.modelo.EstadoUsuario;
import co.edu.uco.kudos.dominio.modelo.Rol;
import co.edu.uco.kudos.dominio.modelo.TokenRefresco;
import co.edu.uco.kudos.dominio.modelo.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RestablecerYCerrarSesionCasoUsoTest {

    private static final Instant AHORA = Instant.parse("2026-11-11T10:00:00Z");

    private DoblesAutenticacion.CuentasEnMemoria cuentas;
    private DoblesAutenticacion.CodigosEnMemoria codigos;
    private DoblesAutenticacion.TokensEnMemoria tokens;
    private DoblesAutenticacion.OutboxEnMemoria outbox;
    private DoblesAutenticacion.RelojAjustable reloj;
    private RestablecerContrasenaCasoUso restablecer;
    private CerrarSesionCasoUso cerrarSesion;
    private Usuario ana;

    @BeforeEach
    void preparar() {
        cuentas = new DoblesAutenticacion.CuentasEnMemoria();
        codigos = new DoblesAutenticacion.CodigosEnMemoria();
        tokens = new DoblesAutenticacion.TokensEnMemoria();
        outbox = new DoblesAutenticacion.OutboxEnMemoria();
        reloj = new DoblesAutenticacion.RelojAjustable(AHORA);
        GestorTransacciones transacciones = Runnable::run;
        restablecer = new RestablecerContrasenaCasoUso(cuentas, cuentas, codigos, tokens, outbox,
                new DoblesAutenticacion.CodificadorFalso(), new ArrayList<>()::add, transacciones, reloj,
                Duration.ofMinutes(30));
        cerrarSesion = new CerrarSesionCasoUso(tokens, new ArrayList<>()::add, transacciones, reloj);
        ana = new Usuario(UUID.randomUUID(), "Ana", "ana@uco.edu.co", null, true, Rol.DOCENTE, EstadoUsuario.ACTIVO,
                "hash:Clave-Anterior-2026", 3, null);
        cuentas.guardar(ana);
    }

    @Test
    void restablecerCambiaLaContrasenaYCierraTodasLasSesiones() {
        tokens.guardar(TokenRefresco.emitir(ana.getId(), "hash-sesion", AHORA, Duration.ofDays(7)));

        restablecer.solicitar("ANA@uco.edu.co");
        String token = outbox.notificaciones.getFirst().getDatos().get("token");
        restablecer.confirmar(token, "Clave-Nueva-2026!");

        Usuario actualizada = cuentas.buscarPorId(ana.getId()).orElseThrow();
        assertThat(actualizada.getHashContrasena()).isEqualTo("hash:Clave-Nueva-2026!");
        assertThat(actualizada.getIntentosFallidos()).isZero();
        assertThat(tokens.tokens.values()).allMatch(TokenRefresco::estaRevocado);
        assertThatThrownBy(() -> restablecer.confirmar(token, "Otra-Clave-2026!"))
                .isInstanceOf(ReglaNegocioExcepcion.class).extracting("codigo").isEqualTo("ENLACE_INVALIDO");
    }

    @Test
    void elEnlaceVenceALosTreintaMinutos() {
        restablecer.solicitar("ana@uco.edu.co");
        String token = outbox.notificaciones.getFirst().getDatos().get("token");
        reloj.avanzar(Duration.ofMinutes(31));

        assertThatThrownBy(() -> restablecer.confirmar(token, "Clave-Nueva-2026!"))
                .isInstanceOf(ReglaNegocioExcepcion.class).extracting("codigo").isEqualTo("ENLACE_INVALIDO");
    }

    @Test
    void unCorreoInexistenteRecibeLaMismaRespuestaSinEnviarNada() {
        restablecer.solicitar("nadie@uco.edu.co");

        assertThat(outbox.notificaciones).isEmpty();
    }

    @Test
    void laNuevaContrasenaDebeCumplirLaPolitica() {
        restablecer.solicitar("ana@uco.edu.co");
        String token = outbox.notificaciones.getFirst().getDatos().get("token");

        assertThatThrownBy(() -> restablecer.confirmar(token, "corta"))
                .isInstanceOf(ReglaNegocioExcepcion.class).extracting("codigo").isEqualTo("CONTRASENA_DEBIL");
    }

    @Test
    void cerrarSesionRevocaElTokenDeRefrescoYEsIdempotente() {
        String plano = "token-de-refresco";
        TokenRefresco token = TokenRefresco.emitir(ana.getId(), TokensSeguros.sha256(plano), AHORA, Duration.ofDays(7));
        tokens.guardar(token);

        cerrarSesion.cerrar(plano);
        cerrarSesion.cerrar(plano);
        cerrarSesion.cerrar("token-que-no-existe");

        assertThat(tokens.tokens.get(token.id()).estaRevocado()).isTrue();
    }
}
