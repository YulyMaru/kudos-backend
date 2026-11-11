package co.edu.uco.kudos.aplicacion.casouso;

import co.edu.uco.kudos.aplicacion.dto.SesionResultado;
import co.edu.uco.kudos.aplicacion.puerto.salida.GestorTransacciones;
import co.edu.uco.kudos.dominio.excepcion.CredencialesInvalidasExcepcion;
import co.edu.uco.kudos.dominio.modelo.AccionAuditoria;
import co.edu.uco.kudos.dominio.modelo.EntradaAuditoria;
import co.edu.uco.kudos.dominio.modelo.EstadoUsuario;
import co.edu.uco.kudos.dominio.modelo.Rol;
import co.edu.uco.kudos.dominio.modelo.TokenRefresco;
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

class RefrescarSesionCasoUsoTest {

    private static final Instant AHORA = Instant.parse("2026-11-11T10:00:00Z");
    private static final String TOKEN_PLANO = "token-de-refresco-original";

    private DoblesAutenticacion.CuentasEnMemoria cuentas;
    private DoblesAutenticacion.TokensEnMemoria tokens;
    private DoblesAutenticacion.RelojAjustable reloj;
    private List<EntradaAuditoria> auditoria;
    private RefrescarSesionCasoUso casoUso;
    private Usuario ana;
    private TokenRefresco original;

    @BeforeEach
    void preparar() {
        cuentas = new DoblesAutenticacion.CuentasEnMemoria();
        tokens = new DoblesAutenticacion.TokensEnMemoria();
        reloj = new DoblesAutenticacion.RelojAjustable(AHORA);
        auditoria = new ArrayList<>();
        ana = usuario(EstadoUsuario.ACTIVO);
        cuentas.guardar(ana);
        original = TokenRefresco.emitir(ana.getId(), TokensSeguros.sha256(TOKEN_PLANO), AHORA, Duration.ofDays(7));
        tokens.guardar(original);
        GestorTransacciones transacciones = Runnable::run;
        casoUso = new RefrescarSesionCasoUso(tokens, cuentas, new DoblesAutenticacion.EmisorFalso(), auditoria::add,
                transacciones, reloj, Duration.ofDays(7));
    }

    @Test
    void rotaElTokenDeRefrescoYEntregaUnoNuevo() {
        reloj.avanzar(Duration.ofHours(1));

        SesionResultado sesion = casoUso.refrescar(TOKEN_PLANO);

        assertThat(sesion.tokenAcceso()).isEqualTo("jwt-" + ana.getId());
        assertThat(sesion.tokenRefresco()).isNotEqualTo(TOKEN_PLANO);
        TokenRefresco nuevo = tokens.buscarPorHash(TokensSeguros.sha256(sesion.tokenRefresco())).orElseThrow();
        TokenRefresco anterior = tokens.tokens.get(original.id());
        assertThat(anterior.estaRevocado()).isTrue();
        assertThat(anterior.reemplazadoPor()).isEqualTo(nuevo.id());
        assertThat(nuevo.expiraEn()).isEqualTo(AHORA.plus(Duration.ofHours(1)).plus(Duration.ofDays(7)));
        assertThat(auditoria).singleElement()
                .satisfies(entrada -> assertThat(entrada.accion()).isEqualTo(AccionAuditoria.SESION_REFRESCADA));
    }

    @Test
    void reutilizarUnTokenYaRotadoRevocaTodasLasSesionesDelUsuario() {
        SesionResultado sesion = casoUso.refrescar(TOKEN_PLANO);

        assertThatThrownBy(() -> casoUso.refrescar(TOKEN_PLANO)).isInstanceOf(CredencialesInvalidasExcepcion.class);

        assertThat(tokens.tokens.values()).allMatch(TokenRefresco::estaRevocado);
        assertThat(auditoria).extracting(EntradaAuditoria::accion).contains(AccionAuditoria.REUSO_TOKEN_REFRESCO);
        assertThatThrownBy(() -> casoUso.refrescar(sesion.tokenRefresco()))
                .isInstanceOf(CredencialesInvalidasExcepcion.class);
    }

    @Test
    void unaRotacionConcurrenteSeTrataComoReuso() {
        tokens.simularRotacionConcurrente = true;

        assertThatThrownBy(() -> casoUso.refrescar(TOKEN_PLANO)).isInstanceOf(CredencialesInvalidasExcepcion.class);

        assertThat(tokens.tokens).hasSize(1);
        assertThat(tokens.tokens.get(original.id()).estaRevocado()).isTrue();
        assertThat(auditoria).extracting(EntradaAuditoria::accion).containsExactly(AccionAuditoria.REUSO_TOKEN_REFRESCO);
    }

    @Test
    void rechazaTokensExpiradosDesconocidosOVacios() {
        reloj.avanzar(Duration.ofDays(8));

        assertThatThrownBy(() -> casoUso.refrescar(TOKEN_PLANO)).isInstanceOf(CredencialesInvalidasExcepcion.class);
        assertThatThrownBy(() -> casoUso.refrescar("token-inventado")).isInstanceOf(CredencialesInvalidasExcepcion.class);
        assertThatThrownBy(() -> casoUso.refrescar(" ")).isInstanceOf(CredencialesInvalidasExcepcion.class);
    }

    @Test
    void siLaCuentaYaNoEstaActivaRevocaSusTokens() {
        cuentas.guardar(new Usuario(ana.getId(), ana.getNombreCompleto(), ana.getCorreo(), null, false, ana.getRol(),
                EstadoUsuario.INACTIVO, ana.getHashContrasena(), 0, null));

        assertThatThrownBy(() -> casoUso.refrescar(TOKEN_PLANO)).isInstanceOf(CredencialesInvalidasExcepcion.class);

        assertThat(tokens.tokens.get(original.id()).estaRevocado()).isTrue();
    }

    private static Usuario usuario(EstadoUsuario estado) {
        return new Usuario(UUID.randomUUID(), "Ana", "ana@uco.net.co", null, false, Rol.ESTUDIANTE, estado,
                "hash:clave", 0, null);
    }
}
