package co.edu.uco.kudos.dominio.modelo;

import co.edu.uco.kudos.dominio.regla.PoliticaBloqueo;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioTest {

    private static final Instant AHORA = Instant.parse("2026-11-11T10:00:00Z");
    private final PoliticaBloqueo politica = new PoliticaBloqueo(5, Duration.ofMinutes(15));

    @Test
    void cuentaLosIntentosFallidosSinBloquearAntesDelLimite() {
        Usuario usuario = usuario();
        for (int i = 0; i < 4; i++) {
            usuario = usuario.registrarIntentoFallido(AHORA, politica);
        }

        assertThat(usuario.getIntentosFallidos()).isEqualTo(4);
        assertThat(usuario.estaBloqueado(AHORA)).isFalse();
    }

    @Test
    void seBloqueaQuinceMinutosAlQuintoIntentoFallido() {
        Usuario usuario = usuario();
        for (int i = 0; i < 5; i++) {
            usuario = usuario.registrarIntentoFallido(AHORA, politica);
        }

        assertThat(usuario.estaBloqueado(AHORA)).isTrue();
        assertThat(usuario.estaBloqueado(AHORA.plus(Duration.ofMinutes(14)))).isTrue();
        assertThat(usuario.estaBloqueado(AHORA.plus(Duration.ofMinutes(15)))).isFalse();
        assertThat(usuario.getBloqueadoHasta()).isEqualTo(AHORA.plus(Duration.ofMinutes(15)));
        assertThat(usuario.getIntentosFallidos()).isZero();
    }

    @Test
    void unInicioExitosoLimpiaLosIntentosYElBloqueo() {
        Usuario usuario = usuario().registrarIntentoFallido(AHORA, politica).registrarIntentoFallido(AHORA, politica);

        Usuario conectado = usuario.registrarInicioExitoso();

        assertThat(usuario.requiereLimpiarIntentos()).isTrue();
        assertThat(conectado.getIntentosFallidos()).isZero();
        assertThat(conectado.getBloqueadoHasta()).isNull();
        assertThat(conectado.requiereLimpiarIntentos()).isFalse();
        assertThat(conectado.getHashContrasena()).isEqualTo("hash");
    }

    @Test
    void sabeSiTieneContrasena() {
        assertThat(usuario().tieneContrasena()).isTrue();
        assertThat(new Usuario(UUID.randomUUID(), "Sin clave", "s@uco.net.co", null, false, Rol.ESTUDIANTE,
                EstadoUsuario.ACTIVO).tieneContrasena()).isFalse();
    }

    private static Usuario usuario() {
        return new Usuario(UUID.randomUUID(), "Ana", "ana@uco.net.co", null, false, Rol.DOCENTE,
                EstadoUsuario.ACTIVO, "hash", 0, null);
    }
}
