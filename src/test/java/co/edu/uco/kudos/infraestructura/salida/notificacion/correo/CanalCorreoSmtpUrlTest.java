package co.edu.uco.kudos.infraestructura.salida.notificacion.correo;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CanalCorreoSmtpUrlTest {

    @ParameterizedTest
    @CsvSource(value = {
            "https://kudos.uco.edu.co|https://kudos.uco.edu.co/iniciar-sesion",
            "https://kudos.uco.edu.co/|https://kudos.uco.edu.co/iniciar-sesion",
            "https://kudos.uco.edu.co///|https://kudos.uco.edu.co/iniciar-sesion",
            "https://kudos.uco.edu.co/app/|https://kudos.uco.edu.co/app/iniciar-sesion",
            "/|/iniciar-sesion",
            "''|/iniciar-sesion"
    }, delimiter = '|')
    void quitaLasBarrasFinalesDeLaUrlDelFrontend(String urlFrontend, String enlaceEsperado) {
        CanalCorreoSmtp canal = new CanalCorreoSmtp(mock(JavaMailSender.class), "no-responder@uco-kudos.app",
                "UCO Kudos", urlFrontend);

        CanalCorreoSmtp.Mensaje mensaje = canal.construir(Map.of("plantilla", "CUENTA_ACTIVADA",
                "nombreDestinatario", "Ana"));

        assertThat(mensaje.texto()).contains(enlaceEsperado + "\n");
        assertThat(mensaje.html()).contains("href=\"" + enlaceEsperado + "\"");
    }
}
