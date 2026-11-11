package co.edu.uco.kudos.configuracion;

import co.edu.uco.kudos.infraestructura.entrada.websocket.AutenticacionStompInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfiguracion implements WebSocketMessageBrokerConfigurer {

    private final AutenticacionStompInterceptor autenticacion;
    private final PropiedadesSeguridad propiedadesSeguridad;

    public WebSocketConfiguracion(AutenticacionStompInterceptor autenticacion,
                                  PropiedadesSeguridad propiedadesSeguridad) {
        this.autenticacion = autenticacion;
        this.propiedadesSeguridad = propiedadesSeguridad;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registro) {
        registro.addEndpoint("/ws")
                .setAllowedOrigins(propiedadesSeguridad.origenesPermitidos().toArray(String[]::new));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registro) {
        registro.enableSimpleBroker("/queue");
        registro.setApplicationDestinationPrefixes("/app");
        registro.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registro) {
        registro.interceptors(autenticacion);
    }
}
