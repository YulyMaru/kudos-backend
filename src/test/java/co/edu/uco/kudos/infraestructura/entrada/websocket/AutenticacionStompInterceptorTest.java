package co.edu.uco.kudos.infraestructura.entrada.websocket;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AutenticacionStompInterceptorTest {

    private final JwtDecoder decodificador = mock(JwtDecoder.class);
    private final AutenticacionStompInterceptor interceptor = new AutenticacionStompInterceptor(decodificador);
    private final MessageChannel canal = mock(MessageChannel.class);
    private final String usuario = UUID.randomUUID().toString();

    @Test
    void autenticaLaConexionConUnTokenValido() {
        when(decodificador.decode("token-valido")).thenReturn(jwt());
        StompHeaderAccessor accesor = accesor(StompCommand.CONNECT);
        accesor.addNativeHeader("Authorization", "Bearer token-valido");

        interceptor.preSend(mensaje(accesor), canal);

        assertThat(accesor.getUser()).isNotNull();
        assertThat(accesor.getUser().getName()).isEqualTo(usuario);
    }

    @Test
    void rechazaLaConexionSinToken() {
        StompHeaderAccessor accesor = accesor(StompCommand.CONNECT);

        assertThatThrownBy(() -> interceptor.preSend(mensaje(accesor), canal))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void rechazaLaConexionConTokenInvalido() {
        when(decodificador.decode("token-falso")).thenThrow(new BadJwtException("firma inválida"));
        StompHeaderAccessor accesor = accesor(StompCommand.CONNECT);
        accesor.addNativeHeader("Authorization", "Bearer token-falso");

        assertThatThrownBy(() -> interceptor.preSend(mensaje(accesor), canal))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void permiteSuscribirseSoloALaColaPropia() {
        StompHeaderAccessor accesor = accesor(StompCommand.SUBSCRIBE);
        accesor.setUser(new JwtAuthenticationToken(jwt()));
        accesor.setDestination("/user/queue/notificaciones");

        assertThatCode(() -> interceptor.preSend(mensaje(accesor), canal)).doesNotThrowAnyException();
    }

    @Test
    void impideSuscribirseALaColaDeOtroUsuario() {
        StompHeaderAccessor accesor = accesor(StompCommand.SUBSCRIBE);
        accesor.setUser(new JwtAuthenticationToken(jwt()));
        accesor.setDestination("/queue/notificaciones-user" + UUID.randomUUID());

        assertThatThrownBy(() -> interceptor.preSend(mensaje(accesor), canal))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void impideSuscribirseSinAutenticarse() {
        StompHeaderAccessor accesor = accesor(StompCommand.SUBSCRIBE);
        accesor.setDestination("/user/queue/notificaciones");

        assertThatThrownBy(() -> interceptor.preSend(mensaje(accesor), canal))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void impideQueElClienteEnvieMensajes() {
        StompHeaderAccessor accesor = accesor(StompCommand.SEND);
        accesor.setUser(new JwtAuthenticationToken(jwt()));
        accesor.setDestination("/app/cualquier-cosa");

        assertThatThrownBy(() -> interceptor.preSend(mensaje(accesor), canal))
                .isInstanceOf(AccessDeniedException.class);
    }

    private Jwt jwt() {
        return Jwt.withTokenValue("token-valido").header("alg", "HS256").subject(usuario).build();
    }

    private static StompHeaderAccessor accesor(StompCommand comando) {
        StompHeaderAccessor accesor = StompHeaderAccessor.create(comando);
        accesor.setLeaveMutable(true);
        return accesor;
    }

    private static Message<byte[]> mensaje(StompHeaderAccessor accesor) {
        return MessageBuilder.createMessage(new byte[0], accesor.getMessageHeaders());
    }
}
