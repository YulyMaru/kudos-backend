package co.edu.uco.kudos.configuracion;

import co.edu.uco.kudos.aplicacion.casouso.ProcesarNotificacionesCasoUso;
import co.edu.uco.kudos.aplicacion.puerto.entrada.ProcesarNotificacionesPuerto;
import co.edu.uco.kudos.aplicacion.puerto.salida.CanalNotificacion;
import co.edu.uco.kudos.aplicacion.puerto.salida.GestorTransacciones;
import co.edu.uco.kudos.aplicacion.puerto.salida.RegistroAuditoria;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioNotificacionesPendientes;
import co.edu.uco.kudos.dominio.regla.PoliticaReintentos;
import co.edu.uco.kudos.infraestructura.salida.notificacion.correo.CanalCorreoSmtp;
import co.edu.uco.kudos.infraestructura.salida.notificacion.push.CanalPushWebSocket;
import co.edu.uco.kudos.infraestructura.salida.notificacion.whatsapp.CanalWhatsappCloudApi;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({PropiedadesCorreo.class, PropiedadesWhatsapp.class, PropiedadesOutbox.class})
public class NotificacionesConfiguracion {

    @Bean
    CanalNotificacion canalCorreo(JavaMailSender enviador, PropiedadesCorreo propiedades) {
        return new CanalCorreoSmtp(enviador, propiedades.remitente(), propiedades.nombreRemitente(),
                propiedades.urlFrontend());
    }

    @Bean
    CanalNotificacion canalWhatsapp(PropiedadesWhatsapp propiedades) {
        HttpClient clienteHttp = HttpClient.newBuilder().connectTimeout(propiedades.tiempoConexion()).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(clienteHttp);
        fabrica.setReadTimeout(propiedades.tiempoLectura());
        RestClient cliente = RestClient.builder()
                .baseUrl(propiedades.urlApi())
                .requestFactory(fabrica)
                .build();
        return new CanalWhatsappCloudApi(cliente, propiedades.token(), propiedades.telefonoId(),
                propiedades.plantilla(), propiedades.plantillaCodigo(), propiedades.idioma());
    }

    @Bean
    CanalNotificacion canalPush(SimpMessagingTemplate mensajeria, SimpUserRegistry usuariosConectados) {
        return new CanalPushWebSocket(mensajeria, usuariosConectados);
    }

    @Bean
    PoliticaReintentos politicaReintentos(PropiedadesOutbox propiedades) {
        return new PoliticaReintentos(propiedades.maximoIntentos(), propiedades.retrasoBase(),
                propiedades.retrasoMaximo());
    }

    @Bean
    ProcesarNotificacionesPuerto procesarNotificaciones(RepositorioNotificacionesPendientes pendientes,
                                                        List<CanalNotificacion> canales,
                                                        RegistroAuditoria auditoria,
                                                        GestorTransacciones transacciones, Clock reloj,
                                                        PoliticaReintentos politica,
                                                        PropiedadesOutbox propiedades) {
        return new ProcesarNotificacionesCasoUso(pendientes, canales, auditoria, transacciones, reloj, politica,
                propiedades.tamanoLote(), propiedades.duracionReserva(),
                () -> ThreadLocalRandom.current().nextDouble());
    }
}
