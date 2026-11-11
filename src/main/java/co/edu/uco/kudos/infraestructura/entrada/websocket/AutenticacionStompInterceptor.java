package co.edu.uco.kudos.infraestructura.entrada.websocket;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class AutenticacionStompInterceptor implements ChannelInterceptor {

    private static final String PREFIJO_BEARER = "Bearer ";
    private static final String PREFIJO_DESTINOS_PERMITIDOS = "/user/queue/";

    private final JwtDecoder decodificador;

    public AutenticacionStompInterceptor(JwtDecoder decodificador) {
        this.decodificador = decodificador;
    }

    @Override
    public Message<?> preSend(Message<?> mensaje, MessageChannel canal) {
        StompHeaderAccessor accesor = MessageHeaderAccessor.getAccessor(mensaje, StompHeaderAccessor.class);
        if (accesor == null || accesor.getCommand() == null) {
            return mensaje;
        }
        switch (accesor.getCommand()) {
            case CONNECT, STOMP -> autenticar(accesor);
            case SUBSCRIBE -> validarSuscripcion(accesor);
            case SEND -> throw new AccessDeniedException("El cliente no puede enviar mensajes por este canal");
            default -> {
            }
        }
        return mensaje;
    }

    private void autenticar(StompHeaderAccessor accesor) {
        String encabezado = accesor.getFirstNativeHeader("Authorization");
        if (encabezado == null || !encabezado.startsWith(PREFIJO_BEARER)) {
            throw new BadCredentialsException("Se requiere un token Bearer para conectarse");
        }
        try {
            Jwt jwt = decodificador.decode(encabezado.substring(PREFIJO_BEARER.length()));
            accesor.setUser(new JwtAuthenticationToken(jwt));
        } catch (JwtException error) {
            throw new BadCredentialsException("Token inválido o expirado", error);
        }
    }

    private static void validarSuscripcion(StompHeaderAccessor accesor) {
        if (accesor.getUser() == null) {
            throw new AccessDeniedException("Debe autenticarse antes de suscribirse");
        }
        String destino = accesor.getDestination();
        if (destino == null || !destino.startsWith(PREFIJO_DESTINOS_PERMITIDOS)) {
            throw new AccessDeniedException("Solo se permite suscribirse a las notificaciones propias");
        }
    }
}
