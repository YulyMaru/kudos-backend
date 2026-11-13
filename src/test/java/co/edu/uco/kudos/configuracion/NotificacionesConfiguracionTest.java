package co.edu.uco.kudos.configuracion;

import co.edu.uco.kudos.aplicacion.puerto.salida.CanalNotificacion;
import co.edu.uco.kudos.aplicacion.puerto.salida.GestorTransacciones;
import co.edu.uco.kudos.aplicacion.puerto.salida.RegistroAuditoria;
import co.edu.uco.kudos.aplicacion.puerto.salida.RepositorioNotificacionesPendientes;
import co.edu.uco.kudos.infraestructura.salida.notificacion.whatsapp.CanalSmsTraccar;
import co.edu.uco.kudos.infraestructura.salida.notificacion.whatsapp.CanalWhatsappCloudApi;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class NotificacionesConfiguracionTest {

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withUserConfiguration(NotificacionesConfiguracion.class)
            .withBean(JavaMailSender.class, () -> mock(JavaMailSender.class))
            .withBean(SimpMessagingTemplate.class, () -> mock(SimpMessagingTemplate.class))
            .withBean(SimpUserRegistry.class, () -> mock(SimpUserRegistry.class))
            .withBean(RepositorioNotificacionesPendientes.class, () -> mock(RepositorioNotificacionesPendientes.class))
            .withBean(RegistroAuditoria.class, () -> mock(RegistroAuditoria.class))
            .withBean(GestorTransacciones.class, () -> mock(GestorTransacciones.class))
            .withBean(Clock.class, Clock::systemUTC);

    @Test
    void usaMetaPorDefecto() {
        contexto.run(aplicacion -> assertThat(aplicacion.getBeansOfType(CanalNotificacion.class).values())
                .hasAtLeastOneElementOfType(CanalWhatsappCloudApi.class)
                .noneMatch(CanalSmsTraccar.class::isInstance));
    }

    @Test
    void usaSmsTraccarCuandoSeConfiguraElProveedor() {
        contexto.withPropertyValues("kudos.notificaciones.whatsapp.proveedor=sms-traccar",
                        "kudos.notificaciones.smstraccar.token=token-secreto")
                .run(aplicacion -> {
                    assertThat(aplicacion.getBeansOfType(CanalNotificacion.class).values())
                            .hasAtLeastOneElementOfType(CanalSmsTraccar.class)
                            .noneMatch(CanalWhatsappCloudApi.class::isInstance);
                    assertThat(aplicacion.getBean(PropiedadesSmsTraccar.class).toString())
                            .doesNotContain("token-secreto");
                });
    }
}
