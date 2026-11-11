package co.edu.uco.kudos.infraestructura.salida.seguridad;

import co.edu.uco.kudos.aplicacion.dto.TokenAcceso;
import co.edu.uco.kudos.dominio.modelo.EstadoUsuario;
import co.edu.uco.kudos.dominio.modelo.Rol;
import co.edu.uco.kudos.dominio.modelo.Usuario;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SeguridadAdaptadoresTest {

    private static final SecretKey CLAVE = new SecretKeySpec(
            "secreto-de-pruebas-con-al-menos-32-bytes".getBytes(StandardCharsets.UTF_8), "HmacSHA256");

    private final Usuario usuario = new Usuario(UUID.randomUUID(), "Ana Docente", "ana@uco.net.co", null, false,
            Rol.DOCENTE, EstadoUsuario.ACTIVO);

    @Test
    void elTokenEmitidoContieneSujetoRolEmisorYExpiracion() {
        Instant ahora = Instant.now();
        TokenAcceso token = emisor("uco-kudos").emitir(usuario, ahora);

        Jwt jwt = decodificador("uco-kudos").decode(token.valor());

        assertThat(jwt.getSubject()).isEqualTo(usuario.getId().toString());
        assertThat(jwt.getClaimAsString("rol")).isEqualTo("DOCENTE");
        assertThat(jwt.getClaimAsString("nombre")).isEqualTo("Ana Docente");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("uco-kudos");
        assertThat(jwt.getExpiresAt()).isEqualTo(token.expiraEn().truncatedTo(ChronoUnit.SECONDS));
        assertThat(jwt.getId()).isNotBlank();
        assertThat(token.toString()).doesNotContain(token.valor());
    }

    @Test
    void unTokenDeOtroEmisorEsRechazado() {
        TokenAcceso token = emisor("otro-sistema").emitir(usuario, Instant.now());

        assertThatThrownBy(() -> decodificador("uco-kudos").decode(token.valor()))
                .isInstanceOf(JwtValidationException.class);
    }

    @Test
    void unTokenExpiradoEsRechazado() {
        TokenAcceso token = emisor("uco-kudos").emitir(usuario, Instant.now().minus(Duration.ofHours(1)));

        assertThatThrownBy(() -> decodificador("uco-kudos").decode(token.valor()))
                .isInstanceOf(JwtValidationException.class);
    }

    @Test
    void bcryptCodificaYVerificaContrasenas() {
        CodificadorContrasenasBCrypt codificador = new CodificadorContrasenasBCrypt(4);

        String hash = codificador.codificar("Clave-Segura-2026");

        assertThat(hash).startsWith("$2").isNotEqualTo("Clave-Segura-2026");
        assertThat(codificador.coincide("Clave-Segura-2026", hash)).isTrue();
        assertThat(codificador.coincide("otra-clave", hash)).isFalse();
        assertThat(codificador.coincide("Clave-Segura-2026", "no-es-un-hash")).isFalse();
        assertThat(codificador.coincide("Clave-Segura-2026", null)).isFalse();
    }

    private static EmisorTokensJwt emisor(String nombreEmisor) {
        return new EmisorTokensJwt(new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(CLAVE)), nombreEmisor,
                Duration.ofMinutes(15));
    }

    private static NimbusJwtDecoder decodificador(String emisorEsperado) {
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(CLAVE).macAlgorithm(MacAlgorithm.HS256).build();
        decodificador.setJwtValidator(JwtValidators.createDefaultWithIssuer(emisorEsperado));
        return decodificador;
    }
}
