package co.edu.uco.kudos.infraestructura.salida.notificacion.correo;

import co.edu.uco.kudos.aplicacion.dto.ResultadoEnvio;
import co.edu.uco.kudos.dominio.modelo.Canal;
import co.edu.uco.kudos.dominio.modelo.Notificacion;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Instant;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanalCorreoSmtpTest {

    private final JavaMailSender enviador = mock(JavaMailSender.class);
    private final CanalCorreoSmtp canal = new CanalCorreoSmtp(enviador, "no-responder@uco-kudos.app", "UCO Kudos",
            "https://kudos.uco.edu.co/");

    @Test
    void enviaElCorreoAlDestinatarioConAsuntoYRemitente() throws Exception {
        when(enviador.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        Notificacion notificacion = notificacion();

        ResultadoEnvio resultado = canal.enviar(notificacion);

        ArgumentCaptor<MimeMessage> mensaje = ArgumentCaptor.forClass(MimeMessage.class);
        verify(enviador).send(mensaje.capture());
        assertThat(resultado.entregado()).isTrue();
        assertThat(canal.canal()).isEqualTo(Canal.EMAIL);
        assertThat(mensaje.getValue().getSubject()).isEqualTo(CanalCorreoSmtp.ASUNTO);
        assertThat(mensaje.getValue().getRecipients(Message.RecipientType.TO))
                .extracting(Object::toString).containsExactly("diego@uco.net.co");
        assertThat(((InternetAddress) mensaje.getValue().getFrom()[0]).getAddress())
                .isEqualTo("no-responder@uco-kudos.app");
        assertThat(mensaje.getValue().getHeader(CanalCorreoSmtp.ENCABEZADO_ID_NOTIFICACION))
                .containsExactly(notificacion.getId().toString());
    }

    @Test
    void propagaLaFallaDelServidorSmtpParaQueSeReintente() {
        when(enviador.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new MailSendException("SMTP no disponible")).when(enviador).send(any(MimeMessage.class));

        assertThatThrownBy(() -> canal.enviar(notificacion())).isInstanceOf(MailSendException.class);
    }

    @Test
    void escapaElHtmlDeLosDatosDelUsuario() {
        assertThat(CanalCorreoSmtp.escapar("<script>alert('x')</script> & \"y\""))
                .isEqualTo("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt; &amp; &quot;y&quot;");
    }

    private static Notificacion notificacion() {
        UUID idKudo = UUID.randomUUID();
        return Notificacion.pendiente(UUID.randomUUID(), idKudo, Canal.EMAIL, "diego@uco.net.co",
                Map.of("nombreAutor", "Ana", "nombreDestinatario", "Diego", "nombreCategoria", "Liderazgo",
                        "idKudo", idKudo.toString()),
                Instant.parse("2026-11-11T10:00:00Z"));
    }
}
