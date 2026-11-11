package co.edu.uco.kudos.configuracion;

import co.edu.uco.kudos.infraestructura.salida.seguridad.EmisorTokensJwt;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Configuration
@EnableConfigurationProperties(PropiedadesSeguridad.class)
public class SeguridadConfiguracion {

    @Bean
    SecurityFilterChain cadenaSeguridad(HttpSecurity http, JwtAuthenticationConverter convertidor) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(autorizacion -> autorizacion
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/iniciar-sesion", "/api/v1/auth/refrescar",
                                "/api/v1/auth/cerrar-sesion", "/api/v1/auth/restablecer/**", "/api/v1/registro",
                                "/api/v1/registro/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/catalogo/**").permitAll()
                        .requestMatchers("/ws", "/ws/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/categorias", "/api/v1/categorias/**")
                        .hasAnyRole("ADMINISTRADOR", "RECTOR", "COORDINADOR_ACADEMICO")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/categorias/**")
                        .hasAnyRole("ADMINISTRADOR", "RECTOR", "COORDINADOR_ACADEMICO")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/categorias/**")
                        .hasAnyRole("ADMINISTRADOR", "RECTOR", "COORDINADOR_ACADEMICO")
                        .requestMatchers("/api/v1/auditoria", "/api/v1/auditoria/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/reportes", "/api/v1/reportes/**")
                        .hasAnyRole("RECTOR", "COORDINADOR_ACADEMICO")
                        .requestMatchers(HttpMethod.POST, "/api/v1/vetos", "/api/v1/vetos/**")
                        .hasAnyRole("RECTOR", "COORDINADOR_ACADEMICO")
                        .requestMatchers(HttpMethod.POST, "/api/v1/facultades", "/api/v1/programas")
                        .hasAnyRole("ADMINISTRADOR", "COORDINADOR_ACADEMICO")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(servidor -> servidor.jwt(jwt -> jwt.jwtAuthenticationConverter(convertidor)));
        return http.build();
    }

    @Bean
    JwtDecoder decodificadorJwt(PropiedadesSeguridad propiedades) {
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(clave(propiedades))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decodificador.setJwtValidator(JwtValidators.createDefaultWithIssuer(propiedades.emisorJwt()));
        return decodificador;
    }

    @Bean
    JwtEncoder codificadorJwt(PropiedadesSeguridad propiedades) {
        return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(clave(propiedades)));
    }

    @Bean
    JwtAuthenticationConverter convertidorAutenticacionJwt() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName(EmisorTokensJwt.RECLAMO_ROL);
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter(roles);
        return convertidor;
    }

    @Bean
    CorsConfigurationSource origenesCors(PropiedadesSeguridad propiedades) {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(propiedades.origenesPermitidos());
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuracion.setExposedHeaders(List.of("Location"));
        configuracion.setMaxAge(Duration.ofHours(1));
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", configuracion);
        return fuente;
    }

    private static SecretKey clave(PropiedadesSeguridad propiedades) {
        return new SecretKeySpec(propiedades.jwtSecreto().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
