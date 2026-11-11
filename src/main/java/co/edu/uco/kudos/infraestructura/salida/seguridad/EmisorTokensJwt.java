package co.edu.uco.kudos.infraestructura.salida.seguridad;

import co.edu.uco.kudos.aplicacion.dto.TokenAcceso;
import co.edu.uco.kudos.aplicacion.puerto.salida.EmisorTokens;
import co.edu.uco.kudos.dominio.modelo.Usuario;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class EmisorTokensJwt implements EmisorTokens {

    public static final String RECLAMO_ROL = "rol";
    public static final String RECLAMO_NOMBRE = "nombre";

    private final JwtEncoder codificador;
    private final String emisor;
    private final Duration vigencia;

    public EmisorTokensJwt(JwtEncoder codificador, String emisor, Duration vigencia) {
        this.codificador = Objects.requireNonNull(codificador);
        this.emisor = Objects.requireNonNull(emisor);
        this.vigencia = Objects.requireNonNull(vigencia);
    }

    @Override
    public TokenAcceso emitir(Usuario usuario, Instant ahora) {
        Instant expiracion = ahora.plus(vigencia);
        JwtClaimsSet reclamos = JwtClaimsSet.builder()
                .id(UUID.randomUUID().toString())
                .issuer(emisor)
                .subject(usuario.getId().toString())
                .issuedAt(ahora)
                .expiresAt(expiracion)
                .claim(RECLAMO_ROL, usuario.getRol().name())
                .claim(RECLAMO_NOMBRE, usuario.getNombreCompleto())
                .build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        String valor = codificador.encode(JwtEncoderParameters.from(cabecera, reclamos)).getTokenValue();
        return new TokenAcceso(valor, expiracion);
    }
}
